package br.edu.unifor.missaomarte.servidor.sombras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDeCaverna;
import br.edu.unifor.missaomarte.pilotagem.horda.PontoDePouso;
import br.edu.unifor.missaomarte.servidor.Agendador;
import br.edu.unifor.missaomarte.servidor.ApiTeste;
import br.edu.unifor.missaomarte.servidor.ApiTeste.RelogioDeTeste;
import br.edu.unifor.missaomarte.servidor.evento.AgendaDeEventos;
import br.edu.unifor.missaomarte.servidor.horda.CatalogoDeCavernas;
import br.edu.unifor.missaomarte.servidor.horda.Horda;
import br.edu.unifor.missaomarte.servidor.horda.ServicoDeHordas;
import br.edu.unifor.missaomarte.servidor.moedas.Origem;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * O modo shadow de ponta a ponta: o servidor de verdade numa porta aleatória e clientes WebSocket de verdade. O
 * relógio é o de teste: o limite de 40 ms entre posições só "passa" quando o teste avança o relógio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:missaomarte-sombras;DB_CLOSE_DELAY=-1" + ApiTeste.MODO_POSTGRESQL,
        "missaomarte.jwt.segredo=segredo-so-para-os-testes-com-32-bytes-ou-mais",
        "MISSAOMARTE_ADMINS= aDmIn-Fase8 , admin-vip, ADMIN-PERIGO "})
@AutoConfigureMockMvc
class CanalDeSombrasTeste {

    private static final long ESPERA_MS = 3000;
    /** Tempo para concluir que uma mensagem NÃO chegou. */
    private static final long SILENCIO_MS = 300;

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    /** Um piloto conectado: guarda o que recebe. */
    private static final class Cliente extends TextWebSocketHandler {
        final BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        final CountDownLatch fechou = new CountDownLatch(1);
        volatile CloseStatus statusDoFechamento;
        WebSocketSession sessao;

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage m) {
            recebidas.add(m.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
            statusDoFechamento = status;
            fechou.countDown();
        }

        void enviar(String texto) throws Exception {
            sessao.sendMessage(new TextMessage(texto));
        }

        String proxima() throws InterruptedException {
            String m = recebidas.poll(ESPERA_MS, TimeUnit.MILLISECONDS);
            assertThat(m).as("mensagem esperada").isNotNull();
            return m;
        }

        void nadaChegou() throws InterruptedException {
            assertThat(recebidas.poll(SILENCIO_MS, TimeUnit.MILLISECONDS)).isNull();
        }
    }

    @LocalServerPort
    int porta;

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioDeTeste relogio;

    @Autowired
    CanalDeSombras canal;

    @Autowired
    ServicoDeMoedas moedas;

    @Autowired
    Agendador agendador;

    @Autowired
    ServicoDeHordas hordas;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    CatalogoDeCavernas catalogo;

    private final List<Cliente> abertos = new ArrayList<>();

    @BeforeEach
    void zerarRelogio() {
        relogio.agora = Instant.now();
    }

    @AfterEach
    void fecharTodos() throws Exception {
        for (Cliente c : abertos) {
            if (c.sessao != null && c.sessao.isOpen()) {
                c.sessao.close();
            }
        }
        for (Cliente c : abertos) {
            c.fechou.await(ESPERA_MS, TimeUnit.MILLISECONDS);
        }
        // o cliente vê o fechamento antes de o servidor tirar o piloto da lista: o próximo teste começa do zero
        esperarConectados(0);
    }

    /** Cadastra e entra com um nome novo (o banco não é desfeito entre os testes); devolve o token. */
    private String token(String nome) throws Exception {
        String senha = "senha-" + UUID.randomUUID();
        mvc.perform(post("/api/jogadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}")).andExpect(status().isCreated());
        String corpo = mvc.perform(post("/api/sessoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private static String nomeUnico(String prefixo) {
        return prefixo + UUID.randomUUID().toString().substring(0, 8);
    }

    private Cliente conectar(String token) throws Exception {
        return conectar(token, "");
    }

    private Cliente conectar(String token, String consulta) throws Exception {
        WebSocketHttpHeaders cabecalhos = new WebSocketHttpHeaders();
        if (token != null) {
            cabecalhos.add("Authorization", "Bearer " + token);
        }
        Cliente c = new Cliente();
        c.sessao = new StandardWebSocketClient().execute(c, cabecalhos,
                URI.create("ws://localhost:" + porta + ConfiguracaoDasSombras.ENDERECO + consulta)).get(ESPERA_MS,
                TimeUnit.MILLISECONDS);
        abertos.add(c);
        return c;
    }

    /**
     * Espera o servidor chegar a este número de pilotos: o aperto de mão termina antes do registro, e o fechamento
     * chega ao cliente antes de o servidor tirar o piloto da lista.
     */
    private void esperarConectados(int quantos) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (canal.conectados() != quantos && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
        assertThat(canal.conectados()).isEqualTo(quantos);
    }

    private static String posicao(double x, double y, double angulo, boolean motor) {
        return "{\"tipo\":\"posicao\",\"x\":" + x + ",\"y\":" + y + ",\"angulo\":" + angulo + ",\"motor\":" + motor
                + "}";
    }

    private void avancarRelogio() {
        relogio.avancar(Duration.ofMillis(CanalDeSombras.INTERVALO_MINIMO_MS));
    }

    @Test
    void semTokenOApertoDeMaoERecusado() {
        assertThatThrownBy(() -> conectar(null)).hasMessageContaining("401");
    }

    @Test
    void comTokenInvalidoOApertoDeMaoERecusado() {
        assertThatThrownBy(() -> conectar("nao-e-um-token")).hasMessageContaining("401");
    }

    @Test
    void aPosicaoDeUmPilotoChegaAosOutrosComONomeDoToken() throws Exception {
        String ana = nomeUnico("Ana");
        Cliente a = conectar(token(ana));
        Cliente b = conectar(token(nomeUnico("Bia")));
        esperarConectados(2);

        a.enviar("{\"tipo\":\"posicao\",\"x\":1940.5,\"y\":300,\"angulo\":-12.5,\"motor\":true,\"nome\":\"Falso\"}");

        String sombra = b.proxima();
        assertThat((String) JsonPath.read(sombra, "$.tipo")).isEqualTo("sombra");
        assertThat((String) JsonPath.read(sombra, "$.nome")).isEqualTo(ana);   // do token, não da mensagem
        assertThat((Double) JsonPath.read(sombra, "$.x")).isEqualTo(1940.5);
        assertThat((Double) JsonPath.read(sombra, "$.y")).isEqualTo(300.0);
        assertThat((Double) JsonPath.read(sombra, "$.angulo")).isEqualTo(-12.5);
        assertThat((Boolean) JsonPath.read(sombra, "$.motor")).isTrue();
        assertThat((Integer) JsonPath.read(sombra, "$.piloto")).isPositive();
        a.nadaChegou();   // ninguém recebe a própria sombra
    }

    /** Fase 8: compra e equipa uma skin pela API (com um saldo dado direto no extrato). */
    private void equipar(String token, String skin, int preco) throws Exception {
        String eu = mvc.perform(get("/api/jogadores/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        moedas.creditar(((Number) JsonPath.read(eu, "$.id")).longValue(), Origem.SEMANA, "teste:" + skin, preco);
        mvc.perform(post("/api/loja/compras").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"" + skin + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/jogadores/eu/skin").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"skin\":\"" + skin + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void aSombraLevaASkinEquipadaDoPerfilENuncaADaMensagem() throws Exception {     // CT-16
        String tokenDaAna = token(nomeUnico("Ana"));
        equipar(tokenDaAna, "guara", 60);
        Cliente a = conectar(tokenDaAna);
        Cliente b = conectar(token(nomeUnico("Bia")));
        esperarConectados(2);

        a.enviar("{\"tipo\":\"posicao\",\"x\":100,\"y\":100,\"angulo\":0,\"motor\":false,\"skin\":\"padrao_5\"}");
        assertThat((String) JsonPath.read(b.proxima(), "$.skin")).isEqualTo("guara");   // do perfil, não da mensagem

        b.enviar(posicao(200, 100, 0, false));   // quem não comprou nada voa com a padrão
        assertThat((String) JsonPath.read(a.proxima(), "$.skin")).isEqualTo("padrao");
    }

    @Test
    void mensagensInvalidasSaoIgnoradasSemDerrubarAConexao() throws Exception {
        Cliente a = conectar(token(nomeUnico("Caio")));
        Cliente b = conectar(token(nomeUnico("Davi")));
        esperarConectados(2);

        for (String ruim : List.of("não é json", "[]", "{\"tipo\":\"outro\"}",
                posicao(-1, 100, 0, false), posicao(3841, 100, 0, false), posicao(100, 1441, 0, false),
                posicao(100, 100, 181, false), "{\"tipo\":\"posicao\",\"x\":\"10\",\"y\":10,\"angulo\":0,\"motor\":false}",
                "{\"tipo\":\"posicao\",\"x\":10,\"y\":10,\"angulo\":0}")) {
            a.enviar(ruim);
            avancarRelogio();
        }
        b.nadaChegou();

        a.enviar(posicao(10, 20, 0, false));   // a conexão continua valendo
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(10.0);
    }

    @Test
    void posicoesRapidasDemaisSaoDescartadas() throws Exception {
        Cliente a = conectar(token(nomeUnico("Eva")));
        Cliente b = conectar(token(nomeUnico("Fabio")));
        esperarConectados(2);

        a.enviar(posicao(100, 100, 0, false));
        a.enviar(posicao(200, 100, 0, false));   // mesmo instante: descartada
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(100.0);
        b.nadaChegou();

        avancarRelogio();
        a.enviar(posicao(300, 100, 0, false));
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(300.0);
    }

    private static String explodiu(double x, double y) {
        return "{\"tipo\":\"explodiu\",\"x\":" + x + ",\"y\":" + y + "}";
    }

    @Test
    void aExplosaoDeUmPilotoChegaAosOutrosNaPosicaoInformada() throws Exception {
        Cliente a = conectar(token(nomeUnico("Jade")));
        Cliente b = conectar(token(nomeUnico("Kai")));
        esperarConectados(2);
        a.enviar(posicao(500, 300, 0, false));
        int piloto = JsonPath.read(b.proxima(), "$.piloto");
        avancarRelogio();

        a.enviar(explodiu(512.5, 290));

        String explosao = b.proxima();
        assertThat((String) JsonPath.read(explosao, "$.tipo")).isEqualTo("explosao");
        assertThat((Integer) JsonPath.read(explosao, "$.piloto")).isEqualTo(piloto);
        assertThat((Double) JsonPath.read(explosao, "$.x")).isEqualTo(512.5);
        assertThat((Double) JsonPath.read(explosao, "$.y")).isEqualTo(290.0);
        a.nadaChegou();   // quem explodiu não recebe a própria explosão
    }

    @Test
    void aExplosaoLogoDepoisDeUmaPosicaoNaoEDescartada() throws Exception {
        // o jogo manda a posição a cada 100 ms, e a batida acontece a qualquer instante: a explosão não pode
        // cair no limite de 40 ms das posições
        Cliente a = conectar(token(nomeUnico("Nina")));
        Cliente b = conectar(token(nomeUnico("Otto")));
        esperarConectados(2);

        a.enviar(posicao(500, 300, 0, false));
        a.enviar(explodiu(500, 290));   // mesmo instante

        assertThat((String) JsonPath.read(b.proxima(), "$.tipo")).isEqualTo("sombra");
        assertThat((String) JsonPath.read(b.proxima(), "$.tipo")).isEqualTo("explosao");
    }

    @Test
    void explosoesSeguidasDemaisSaoDescartadas() throws Exception {
        Cliente a = conectar(token(nomeUnico("Pia")));
        Cliente b = conectar(token(nomeUnico("Rui")));
        esperarConectados(2);

        a.enviar(explodiu(100, 100));
        assertThat((String) JsonPath.read(b.proxima(), "$.tipo")).isEqualTo("explosao");
        relogio.avancar(Duration.ofMillis(CanalDeSombras.INTERVALO_MINIMO_ENTRE_EXPLOSOES_MS - 1));
        a.enviar(explodiu(200, 100));   // a nave ainda nem renasceu: descartada
        b.nadaChegou();

        relogio.avancar(Duration.ofMillis(1));
        a.enviar(explodiu(300, 100));
        assertThat((Double) JsonPath.read(b.proxima(), "$.x")).isEqualTo(300.0);
    }

    @Test
    void explosoesInvalidasSaoIgnoradas() throws Exception {
        Cliente a = conectar(token(nomeUnico("Lia")));
        Cliente b = conectar(token(nomeUnico("Moa")));
        esperarConectados(2);

        for (String ruim : List.of(explodiu(-1, 100), explodiu(3841, 100), explodiu(100, 1441),
                "{\"tipo\":\"explodiu\",\"x\":100}", "{\"tipo\":\"explodiu\",\"x\":\"100\",\"y\":100}")) {
            a.enviar(ruim);
            avancarRelogio();
        }
        b.nadaChegou();

        a.enviar(explodiu(100, 100));
        assertThat((String) JsonPath.read(b.proxima(), "$.tipo")).isEqualTo("explosao");
    }

    @Test
    void quemSaiEAvisadoAosOutros() throws Exception {
        Cliente a = conectar(token(nomeUnico("Gil")));
        Cliente b = conectar(token(nomeUnico("Hugo")));
        esperarConectados(2);
        a.enviar(posicao(100, 100, 0, false));
        int piloto = JsonPath.read(b.proxima(), "$.piloto");

        a.sessao.close();

        String saiu = b.proxima();
        assertThat((String) JsonPath.read(saiu, "$.tipo")).isEqualTo("saiu");
        assertThat((Integer) JsonPath.read(saiu, "$.piloto")).isEqualTo(piloto);
    }

    @Test
    void mensagemGrandeDemaisFechaAConexao() throws Exception {
        Cliente a = conectar(token(nomeUnico("Iara")));
        esperarConectados(1);

        a.enviar("{\"tipo\":\"posicao\",\"lixo\":\"" + "x".repeat(CanalDeSombras.TAMANHO_MAXIMO) + "\"}");

        assertThat(a.fechou.await(ESPERA_MS, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(a.statusDoFechamento.getCode()).isEqualTo(CloseStatus.TOO_BIG_TO_PROCESS.getCode());
    }

    @Test
    void salasIsolamSombrasEValidamPelosLimitesDaCaverna() throws Exception {
        relogio.agora = Instant.parse("2026-11-19T15:10:00Z");
        Horda horda = publicarPocoPolar();
        Cliente livre = conectar(token(nomeUnico("Livre")));
        Cliente hordaA = conectar(token(nomeUnico("Horda")), "?sala=horda-" + horda.numero());
        Cliente hordaB = conectar(token(nomeUnico("Horda")), "?sala=horda-" + horda.numero());
        esperarConectados(3);

        hordaA.enviar(posicao(2500, 100, 0, false)); // caberia na Marciana, mas ultrapassa a largura do Poço
        hordaA.enviar(posicao(2000, 1500, 0, false)); // só cabe na altura do Poço
        assertThat((Double) JsonPath.read(hordaB.proxima(), "$.y")).isEqualTo(1500.0);
        livre.nadaChegou();
        livre.enviar(posicao(100, 100, 0, false));
        hordaB.nadaChegou();
    }

    @Test
    void recusaSalaDesconhecidaInvalidaOuVencida() throws Exception {
        relogio.agora = Instant.parse("2026-11-18T15:00:00Z");
        String token = token(nomeUnico("Sala"));
        long numero = ServicoDeHordas.numeroEm(relogio.instant());
        recusa(conectar(token, "?sala=horda-" + numero));
        recusa(conectar(token, "?sala=qualquer"));
        hordas.atual(relogio.instant());
        relogio.avancar(Duration.ofMinutes(151));
        recusa(conectar(token, "?sala=horda-" + numero));
        esperarConectados(0);
    }

    @Test
    void painelUsaNomeDoBancoEExigeTokenDeAdministrador() throws Exception {
        relogio.agora = Instant.parse("2026-11-20T12:00:00Z");
        String admin = token("Admin-Fase8");
        String piloto = token(nomeUnico("Piloto"));
        disparar(null, "CHUVA_DE_METEOROS").andExpect(status().isUnauthorized());
        disparar(piloto, "CHUVA_DE_METEOROS").andExpect(status().isForbidden());
        disparar(admin, "CHUVA_DE_METEOROS").andExpect(status().isAccepted());
        jdbc.update("update jogadores set nome = 'ExAdmin' where nome = 'Admin-Fase8'");
        disparar(admin, "CHUVA_DE_METEOROS").andExpect(status().isForbidden());
    }

    @Test
    void vipChegaAsDuasSalasComPontosDelasEPrimeiroPilotoGanha() throws Exception {
        relogio.agora = Instant.parse("2026-11-21T15:10:00Z");
        Horda horda = publicarPocoPolar();
        String admin = token("Admin-VIP");
        String vencedor = nomeUnico("Vencedor");
        Cliente livre = conectar(token(vencedor));
        Cliente rival = conectar(token(nomeUnico("Rival")), "?sala=horda-" + horda.numero());
        esperarConectados(2);
        disparar(admin, "PASSAGEIRO_VIP").andExpect(status().isAccepted());
        agendador.verificar();

        Map<String, Object> aviso = JsonPath.read(livre.proxima(), "$");
        String evento = (String) aviso.get("evento");
        assertThat(evento).startsWith("e-" + horda.numero() + "-");
        assertThat(aviso).containsExactlyInAnyOrderEntriesOf(Map.of(
                "tipo", "evento-aviso", "evento", evento, "nome", "PASSAGEIRO_VIP", "em", 15));
        assertThat(JsonPath.<Map<String, Object>>read(rival.proxima(), "$")).isEqualTo(aviso);

        relogio.avancar(Duration.ofSeconds(15));
        agendador.verificar();
        conferirInicioVip(livre.proxima(), evento, catalogo.porId("marciana").orElseThrow());
        conferirInicioVip(rival.proxima(), evento, horda.descricao().caverna());

        livre.enviar("{\"tipo\":\"vip\",\"evento\":\"" + evento + "\"}");
        Map<String, Object> pego = Map.of("tipo", "vip-pego", "evento", evento, "nome", vencedor);
        assertThat(JsonPath.<Map<String, Object>>read(livre.proxima(), "$")).isEqualTo(pego);
        assertThat(JsonPath.<Map<String, Object>>read(rival.proxima(), "$")).isEqualTo(pego);
        rival.enviar("{\"tipo\":\"vip\",\"evento\":\"" + evento + "\"}");
        rival.nadaChegou();
        assertThat(jdbc.queryForList("select pontos from pontos_de_evento where evento = ?", Integer.class, evento))
                .containsExactly(5);
    }

    @Test
    void perigoPontuaUmaVezSoParaQuemEstavaConectadoNoInicio() throws Exception {
        relogio.agora = Instant.parse("2026-11-22T15:10:00Z");
        String admin = token("Admin-Perigo");
        String sobrevivente = nomeUnico("Sobreviveu");
        Cliente a = conectar(token(sobrevivente));
        esperarConectados(1);
        disparar(admin, "CHUVA_DE_METEOROS").andExpect(status().isAccepted());
        agendador.verificar();
        String evento = JsonPath.read(a.proxima(), "$.evento");
        relogio.avancar(Duration.ofSeconds(15));
        agendador.verificar();
        Map<String, Object> inicio = JsonPath.read(a.proxima(), "$");
        assertThat(inicio).containsEntry("tipo", "evento-inicio").containsEntry("evento", evento)
                .containsEntry("nome", "CHUVA_DE_METEOROS").containsEntry("duracao", 20).doesNotContainKey("vip");
        assertThat(inicio.get("semente")).isInstanceOf(Number.class);
        Cliente atrasado = conectar(token(nomeUnico("Atrasado")));
        esperarConectados(2);
        String resultado = "{\"tipo\":\"evento-resultado\",\"evento\":\"" + evento + "\",\"sobreviveu\":true}";
        relogio.avancar(Duration.ofSeconds(20));

        a.enviar(resultado);
        a.enviar(resultado);
        a.enviar(posicao(100, 100, 0, false));
        assertThat((String) JsonPath.read(atrasado.proxima(), "$.tipo")).isEqualTo("sombra");
        atrasado.enviar(resultado);
        atrasado.enviar(posicao(100, 100, 0, false));
        assertThat((String) JsonPath.read(a.proxima(), "$.tipo")).isEqualTo("sombra");

        assertThat(jdbc.queryForList("""
                select j.nome, p.pontos from pontos_de_evento p join jogadores j on j.id = p.jogador_id
                where p.evento = ?
                """, evento)).containsExactly(Map.of("nome", sobrevivente, "pontos", 3));
    }

    @Test
    void agendaAutomaticaSoPublicaQuandoHaPilotosNoAviso() throws Exception {
        relogio.agora = Instant.parse("2026-11-23T15:00:00Z");
        Horda horda = hordas.atual(relogio.instant());
        var agenda = AgendaDeEventos.daHorda(horda.numero(), horda.inicio(), horda.semente());
        var primeiro = agenda.get(0);
        var segundo = agenda.get(1);
        relogio.agora = primeiro.aviso();
        agendador.verificar();
        relogio.agora = primeiro.inicio();
        agendador.verificar();
        assertThat(jdbc.queryForObject("select count(*) from eventos_ao_vivo where evento = ?",
                Integer.class, primeiro.id())).isZero();

        Cliente piloto = conectar(token(nomeUnico("Agenda")));
        esperarConectados(1);
        relogio.agora = segundo.aviso();
        agendador.verificar();
        agendador.verificar();
        assertThat(JsonPath.<String>read(piloto.proxima(), "$.evento")).isEqualTo(segundo.id());
        assertThat(jdbc.queryForObject("select count(*) from eventos_ao_vivo where evento = ?",
                Integer.class, segundo.id())).isEqualTo(1);
        relogio.agora = segundo.inicio();
        agendador.verificar();
        var inicio = JsonPath.<Map<String, Object>>read(piloto.proxima(), "$");
        assertThat(inicio).containsEntry("tipo", "evento-inicio").containsEntry("nome", segundo.tipo().name());
        assertThat(((Number) inicio.get("semente")).longValue()).isEqualTo(segundo.semente());
        piloto.nadaChegou();
    }

    private static void recusa(Cliente cliente) throws Exception {
        assertThat(cliente.fechou.await(ESPERA_MS, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(cliente.statusDoFechamento.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    private Horda publicarPocoPolar() {
        var c = catalogo.porId("poco-polar").orElseThrow();
        var liberada = new DescricaoDeCaverna(c.id(), c.nome(), c.largura(), c.altura(), c.rochas(), c.base(),
                c.pontosDePouso(), c.asteroides(), true);
        doReturn(List.of(liberada)).when(catalogo).liberadas();
        return hordas.atual(relogio.instant());
    }

    private ResultActions disparar(String token, String tipo) throws Exception {
        var pedido = post("/api/admin/eventos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"" + tipo + "\"}");
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(pedido);
    }

    private static void conferirInicioVip(String mensagem, String evento, DescricaoDeCaverna caverna) {
        Map<String, Object> inicio = JsonPath.read(mensagem, "$");
        assertThat(inicio).containsEntry("tipo", "evento-inicio").containsEntry("evento", evento)
                .containsEntry("nome", "PASSAGEIRO_VIP").containsEntry("duracao", 45);
        assertThat(inicio.get("semente")).isInstanceOf(Number.class);
        Number x = JsonPath.read(mensagem, "$.vip.x");
        Number y = JsonPath.read(mensagem, "$.vip.superficie");
        assertThat(caverna.pontosDePouso()).contains(new PontoDePouso(x.doubleValue(), y.doubleValue()));
    }
}
