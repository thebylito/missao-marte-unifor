package br.edu.unifor.missaomarte.servidor.horda;

import static br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas.FORTALEZA;

import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda;
import br.edu.unifor.missaomarte.servidor.partida.Dificuldade;
import br.edu.unifor.missaomarte.servidor.seguranca.Sementes;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicoDeHordas {

    // Horda 0: 01/01/2026, 00:00 em Fortaleza. Este marco não muda em novos deploys.
    private static final ZonedDateTime ANCORA = LocalDate.of(2026, 1, 1).atStartOfDay(FORTALEZA);
    public static final Duration DURACAO = Duration.ofHours(2);
    public static final Duration TOLERANCIA = Duration.ofMinutes(30);

    private final CatalogoDeCavernas catalogo;
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Clock relogio;
    private final byte[] segredo;
    private final DescricaoDeCaverna cavernaDeEnsaio;
    private final AjustesDeVoo ajustes;

    public ServicoDeHordas(CatalogoDeCavernas catalogo, JdbcTemplate jdbc, JsonMapper json, Clock relogio,
                           AjustesDeVoo ajustes,
                           @Value("${missaomarte.hordas.segredo:}") String segredo,
                           @Value("${missaomarte.hordas.ensaio:}") String ensaio) {
        this.catalogo = catalogo;
        this.ajustes = ajustes;
        this.jdbc = jdbc;
        this.json = json;
        this.relogio = relogio;
        this.segredo = segredo.isBlank() ? segredoTemporario() : segredo.getBytes(StandardCharsets.UTF_8);
        if (this.segredo.length < 32) {
            throw new IllegalStateException("MISSAOMARTE_HORDA_SEGREDO precisa ter pelo menos 32 bytes (HMAC-SHA256).");
        }
        this.cavernaDeEnsaio = ensaio.isBlank() ? null : catalogo.porId(ensaio)
                .orElseThrow(() -> new IllegalArgumentException("caverna de ensaio desconhecida: " + ensaio));
    }

    public static long numeroEm(Instant instante) {
        ZonedDateTime local = ZonedDateTime.ofInstant(instante, FORTALEZA);
        return Math.floorDiv(Duration.between(ANCORA, local).getSeconds(), DURACAO.toSeconds());
    }

    public static Instant inicioDe(long numero) {
        return ANCORA.toInstant().plus(DURACAO.multipliedBy(numero));
    }

    @Transactional
    public Horda atual(Instant agora) {
        long numero = numeroEm(agora);
        if (cavernaDeEnsaio != null) {
            return sortearEnsaio(numero);
        }
        return gravada(numero).orElseGet(() -> gerar(numero));
    }

    /** Salas só são abertas para hordas publicadas, dentro da janela das partidas. */
    public Optional<Horda> sala(long numero, Instant agora) {
        if (!naJanela(numero, agora)) {
            return Optional.empty();
        }
        return cavernaDeEnsaio == null ? gravada(numero) : Optional.of(sortearEnsaio(numero));
    }

    @Transactional
    public Instant conferirPartida(Long numero, Dificuldade dificuldade) {
        var estado = travarParaPartida(numero);
        return conferirPartida(numero, dificuldade, estado);
    }

    /** Mantém a ordem de travas horda → jogador e permite consultar um reenvio antes de validar a janela. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<EstadoDaHorda> travarParaPartida(Long numero) {
        if (numero == null) {
            throw pedidoInvalido("informe o número da horda");
        }
        Instant agora = relogio.instant();
        if (cavernaDeEnsaio != null) {
            var dificuldade = Dificuldade.valueOf(sortearEnsaio(numero).descricao().dificuldade().name());
            return Optional.of(new EstadoDaHorda(dificuldade, false));
        }
        var estado = travar(numero);
        if (estado.isEmpty() && numero == numeroEm(agora)) {
            atual(agora);
            estado = travar(numero);
        }
        return estado;
    }

    public Instant conferirPartida(long numero, Dificuldade dificuldade, Optional<EstadoDaHorda> estado) {
        Instant agora = relogio.instant();
        conferirJanela(numero, agora);
        EstadoDaHorda horda = estado.orElseThrow(() -> pedidoInvalido("horda desconhecida"));
        if (horda.fechada()) {
            throw encerrada();
        }
        if (dificuldade != horda.dificuldade()) {
            throw pedidoInvalido("a dificuldade não corresponde à da horda");
        }
        return agora;
    }

    private static void conferirJanela(long numero, Instant agora) {
        if (numero > numeroEm(agora)) {
            throw pedidoInvalido("a horda ainda não começou");
        }
        if (!naJanela(numero, agora)) {
            throw encerrada();
        }
    }

    private static ResponseStatusException encerrada() {
        var erro = new ResponseStatusException(HttpStatus.CONFLICT, "a horda está encerrada");
        erro.getBody().setProperty("motivo", "HORDA_ENCERRADA");
        return erro;
    }

    private static boolean naJanela(long numero, Instant agora) {
        long atual = numeroEm(agora);
        return numero == atual || numero == atual - 1 && !agora.isAfter(inicioDe(atual).plus(TOLERANCIA));
    }

    private Optional<EstadoDaHorda> travar(long numero) {
        return jdbc.query("select dificuldade, fechada_em from hordas where numero = ? for update",
                (r, i) -> new EstadoDaHorda(Dificuldade.valueOf(r.getString("dificuldade")),
                        r.getObject("fechada_em") != null), numero).stream().findFirst();
    }

    public record EstadoDaHorda(Dificuldade dificuldade, boolean fechada) {}

    private Optional<Horda> gravada(long numero) {
        return jdbc.query("select numero, inicio, fim, semente, semente_agenda, descricao from hordas where numero = ?",
                (r, i) -> new Horda(r.getLong("numero"), r.getObject("inicio", OffsetDateTime.class).toInstant(),
                        r.getObject("fim", OffsetDateTime.class).toInstant(), r.getLong("semente"),
                        r.getString("semente_agenda"), json.readValue(r.getString("descricao"), DescricaoDaHorda.class)),
                numero).stream().findFirst();
    }

    private Horda gerar(long numero) {
        String anterior = jdbc.query("select caverna from hordas where numero = ?",
                (r, i) -> r.getString(1), numero - 1).stream().findFirst().orElse(null);
        long semente = semente(numero);
        Horda horda = sortear(numero, semente, sementeAgenda(numero), catalogo.liberadas(), anterior, ajustes);
        jdbc.update("""
                insert into hordas (numero, inicio, fim, semente, caverna, dificuldade, descricao, semente_agenda)
                values (?, ?, ?, ?, ?, ?, ?, ?) on conflict do nothing
                """, numero, utc(horda.inicio()), utc(horda.fim()), semente, horda.descricao().caverna().id(),
                horda.descricao().dificuldade().name(), json.writeValueAsString(horda.descricao()), horda.sementeAgenda());
        return gravada(numero).orElseThrow();
    }

    private Horda sortearEnsaio(long numero) {
        DescricaoDeCaverna c = cavernaDeEnsaio;
        var liberada = new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(),
                c.pontosDePouso(), c.asteroides(), true);
        Horda horda = sortear(numero, semente(numero), sementeAgenda(numero), List.of(liberada), null, ajustes);
        DescricaoDaHorda d = horda.descricao();
        return new Horda(numero, horda.inicio(), horda.fim(), horda.semente(), horda.sementeAgenda(),
                new DescricaoDaHorda(numero, d.dificuldade(), c, d.passageiros(), d.velocidadeDosAsteroides(),
                        d.fasesDosAsteroides(), d.modificadores(), d.roteiro(), d.ajustes()));
    }

    /** A horda sorteada leva os ajustes do servidor de agora (DA-48) e fica com eles depois de gravada. */
    private static Horda sortear(long numero, long semente, String sementeAgenda,
                                List<DescricaoDeCaverna> cavernas, String anterior, AjustesDeVoo ajustes) {
        Instant inicio = inicioDe(numero);
        return new Horda(numero, inicio, inicio.plus(DURACAO), semente, sementeAgenda,
                SorteioDaHorda.sortear(numero, semente, cavernas, anterior).comAjustes(ajustes));
    }

    private long semente(long numero) {
        return Sementes.comoLong(Sementes.derivar(segredo, "horda:" + numero));
    }

    private String sementeAgenda(long numero) {
        return Sementes.emHex(Sementes.derivar(segredo, "agenda:" + numero));
    }

    private static byte[] segredoTemporario() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        LoggerFactory.getLogger(ServicoDeHordas.class).warn(
                "MISSAOMARTE_HORDA_SEGREDO não definido: usando um segredo temporário para hordas novas.");
        return bytes;
    }

    private static OffsetDateTime utc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    private static ResponseStatusException pedidoInvalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
