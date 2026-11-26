package br.edu.unifor.missaomarte.servidor.horda;

import static br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas.FORTALEZA;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import br.edu.unifor.missaomarte.pilotagem.horda.SorteioDaHorda;
import br.edu.unifor.missaomarte.servidor.partida.Dificuldade;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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

    public ServicoDeHordas(CatalogoDeCavernas catalogo, JdbcTemplate jdbc, JsonMapper json, Clock relogio,
                           @Value("${missaomarte.hordas.segredo:}") String segredo,
                           @Value("${missaomarte.hordas.ensaio:}") String ensaio) {
        this.catalogo = catalogo;
        this.jdbc = jdbc;
        this.json = json;
        this.relogio = relogio;
        this.segredo = segredo.isBlank() ? segredoTemporario() : segredo.getBytes(StandardCharsets.UTF_8);
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

    /** Chamado na transação da partida, com a mesma trava que protege seu fechamento. */
    @Transactional
    public Instant conferirPartida(Long numero, Dificuldade dificuldade) {
        if (numero == null) {
            throw pedidoInvalido("informe o número da horda");
        }
        Instant agora = relogio.instant();
        conferirJanela(numero, agora);
        Dificuldade sorteada;
        if (cavernaDeEnsaio != null) {
            sorteada = Dificuldade.valueOf(sortearEnsaio(numero).descricao().dificuldade().name());
        } else {
            var estado = travar(numero);
            if (estado.isEmpty() && numero == numeroEm(agora)) {
                atual(agora);
                estado = travar(numero);
            }
            EstadoDaHorda horda = estado.orElseThrow(() -> pedidoInvalido("horda desconhecida"));
            agora = relogio.instant();
            conferirJanela(numero, agora);
            if (horda.fechada()) {
                throw encerrada();
            }
            sorteada = horda.dificuldade();
        }
        if (dificuldade != sorteada) {
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

    private record EstadoDaHorda(Dificuldade dificuldade, boolean fechada) {}

    private Optional<Horda> gravada(long numero) {
        return jdbc.query("select numero, inicio, fim, semente, descricao from hordas where numero = ?",
                (r, i) -> new Horda(r.getLong("numero"), r.getObject("inicio", OffsetDateTime.class).toInstant(),
                        r.getObject("fim", OffsetDateTime.class).toInstant(), r.getLong("semente"),
                        json.readValue(r.getString("descricao"), DescricaoDaHorda.class)), numero).stream().findFirst();
    }

    private Horda gerar(long numero) {
        String anterior = jdbc.query("select caverna from hordas where numero = ?",
                (r, i) -> r.getString(1), numero - 1).stream().findFirst().orElse(null);
        long semente = semente(numero);
        Horda horda = sortear(numero, semente, catalogo.liberadas(), anterior);
        jdbc.update("""
                insert into hordas (numero, inicio, fim, semente, caverna, dificuldade, descricao)
                values (?, ?, ?, ?, ?, ?, ?) on conflict do nothing
                """, numero, utc(horda.inicio()), utc(horda.fim()), semente, horda.descricao().caverna().id(),
                horda.descricao().dificuldade().name(), json.writeValueAsString(horda.descricao()));
        return gravada(numero).orElseThrow();
    }

    private Horda sortearEnsaio(long numero) {
        DescricaoDeCaverna c = cavernaDeEnsaio;
        var liberada = new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(),
                c.pontosDePouso(), c.asteroides(), true);
        Horda horda = sortear(numero, semente(numero), List.of(liberada), null);
        DescricaoDaHorda d = horda.descricao();
        return new Horda(numero, horda.inicio(), horda.fim(), horda.semente(), new DescricaoDaHorda(numero,
                d.dificuldade(), c, d.passageiros(), d.velocidadeDosAsteroides(), d.fasesDosAsteroides(),
                d.modificadores(), d.roteiro()));
    }

    private static Horda sortear(long numero, long semente, List<DescricaoDeCaverna> cavernas, String anterior) {
        Instant inicio = inicioDe(numero);
        return new Horda(numero, inicio, inicio.plus(DURACAO), semente,
                SorteioDaHorda.sortear(numero, semente, cavernas, anterior));
    }

    private long semente(long numero) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return ByteBuffer.wrap(hmac.doFinal(("horda:" + numero).getBytes(StandardCharsets.UTF_8))).getLong();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("não foi possível calcular a semente da horda", e);
        }
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
