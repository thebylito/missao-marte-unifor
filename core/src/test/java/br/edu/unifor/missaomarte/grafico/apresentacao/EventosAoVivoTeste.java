package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.grafico.online.ConexaoDeSombras;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.Comandos;
import br.edu.unifor.missaomarte.pilotagem.ConfigDeVoo;
import br.edu.unifor.missaomarte.pilotagem.EventoDeVoo;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.PlataformaDePouso;
import br.edu.unifor.missaomarte.pilotagem.horda.PerigoDoRoteiro;
import br.edu.unifor.missaomarte.pilotagem.horda.TipoDeEvento;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventosAoVivoTeste {
    private static final class Canal implements ConexaoDeSombras {
        String sala;
        Ouvinte ouvinte;
        List<JsonValue> mensagens = new ArrayList<>();
        public void abrir(String token, Ouvinte ouvinte) { fail("A sala deve ser explícita"); }
        public void abrir(String token, String sala, Ouvinte ouvinte) {
            this.sala = sala;
            this.ouvinte = ouvinte;
            ouvinte.aberta();
        }
        public void fechar() {}
        public void enviar(String texto) { mensagens.add(new JsonReader().parse(texto)); }
        List<JsonValue> enviadas(String tipo) {
            return mensagens.stream().filter(m -> tipo.equals(m.getString("tipo"))).toList();
        }
    }

    private final Canal canal = new Canal();
    private final PartidaDePilotagem voo = new PartidaDePilotagem("Ana", Dificuldade.MEDIO,
            new Caverna(3000, 1500).base(100, 70), ConfigDeVoo.medio());
    private final Optional<Sessao> sessao = Optional.of(new Sessao("Ana", "token"));
    private SombrasDaPilotagem sombras = new SombrasDaPilotagem(sessao, Optional.of(canal));

    private void conectar() { sombras.atualizar(0, voo); }
    private void passos(int quantidade) {
        for (int i = 0; i < quantidade; i++) voo.avancarUmPasso(Comandos.NENHUM);
        sombras.tratarEventos(voo.consumirEventos());
    }
    private void iniciar(String id, TipoDeEvento tipo) {
        String vip = tipo.perigo() ? "" : ",\"vip\":{\"x\":2000,\"superficie\":116}";
        canal.ouvinte.mensagem("{\"tipo\":\"evento-inicio\",\"evento\":\"" + id + "\",\"nome\":\"" + tipo
                + "\",\"semente\":" + (tipo.perigo() ? 987654321 : 0) + ",\"duracao\":" + tipo.duracaoSegundos()
                + vip + "}");
    }

    @Test void salaLivrePorPadraoEHordaExplicitaInclusiveNaReconexao() {
        conectar();
        assertEquals("livre", canal.sala);
        sombras = new SombrasDaPilotagem(sessao, Optional.of(canal), "horda-123");
        conectar();
        assertEquals("horda-123", canal.sala);
        canal.ouvinte.fechada("1006");
        canal.sala = null;
        sombras.atualizar(4, voo);
        assertEquals("horda-123", canal.sala);
    }

    @Test void avisoDeQuinzeSegundosTemContagemESomUmaVez() {
        conectar();
        String aviso = "{\"tipo\": \"evento-aviso\", \"evento\": \"e-1\", \"nome\": \"CHUVA_DE_METEOROS\", \"em\": 15}";
        canal.ouvinte.mensagem(aviso);
        assertTrue(sombras.consumirAlerta());
        assertTrue(sombras.mensagemDoEvento().orElseThrow().contains("15"));
        sombras.atualizar(3, voo);
        canal.ouvinte.mensagem(aviso);
        assertFalse(sombras.consumirAlerta());
        assertEquals("CHUVA DE METEOROS EM 12 s", sombras.mensagemDoEvento().orElseThrow());
        iniciar("e-1", TipoDeEvento.CHUVA_DE_METEOROS);
        assertFalse(sombras.mensagemDoEvento().orElse("").contains("12"));
    }

    @Test void tresPerigosAplicadosNoProximoPassoComASementeEResultadoDeCadaId() {
        conectar();
        for (TipoDeEvento tipo : TipoDeEvento.values()) {
            if (!tipo.perigo()) continue;
            iniciar(tipo.name(), tipo);
            iniciar(tipo.name(), tipo);
        }
        passos(1);
        assertEquals(3, voo.eventosAplicados().size());
        assertTrue(voo.eventosAplicados().stream().allMatch(e -> e.passo() == 1 && e.semente() == 987654321));
        passos(3000);
        List<JsonValue> resultados = canal.enviadas("evento-resultado");
        assertEquals(List.of("CHUVA_DE_METEOROS", "PANE_DE_GRAVIDADE", "TEMPESTADE"),
                resultados.stream().map(m -> m.getString("evento")).toList());
        assertTrue(resultados.stream().allMatch(m -> m.has("sobreviveu")));
    }

    @Test void fimDoPerigoDoRoteiroNaoCreditaEventoAoVivoDoMesmoTipo() {
        voo.definirRoteiro(List.of(new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 0, 2)));
        conectar();
        passos(120);
        iniciar("e-2", TipoDeEvento.TEMPESTADE);
        passos(2881);
        assertTrue(canal.enviadas("evento-resultado").isEmpty());
        passos(120);
        JsonValue resultado = canal.enviadas("evento-resultado").get(0);
        assertEquals("e-2", resultado.getString("evento"));
        assertTrue(resultado.getBoolean("sobreviveu"));
    }

    @Test void naCoincidenciaDeTipoSementeEPassoSoASegundaOcorrenciaPertenceAoCanal() {
        voo.definirRoteiro(List.of(new PerigoDoRoteiro(TipoDeEvento.TEMPESTADE, 0, 987654321)));
        conectar();
        iniciar("e-tempestade", TipoDeEvento.TEMPESTADE);
        for (int i = 0; i < 3001; i++) voo.avancarUmPasso(Comandos.NENHUM);
        List<EventoDeVoo> fins = voo.consumirEventos().stream()
                .filter(e -> e.tipo() == EventoDeVoo.Tipo.PERIGO_TERMINOU).toList();
        assertEquals(2, fins.size());
        sombras.tratarEventos(List.of(fins.get(0)));
        assertTrue(canal.enviadas("evento-resultado").isEmpty());
        sombras.tratarEventos(List.of(fins.get(1)));
        assertEquals("e-tempestade", canal.enviadas("evento-resultado").get(0).getString("evento"));
    }

    @Test void naveQueExplodiuDuranteOPerigoNaoSobreviveMesmoRenascendoAntesDoFim() {
        conectar();
        iniciar("pane", TipoDeEvento.PANE_DE_GRAVIDADE);
        int vidasIniciais = voo.getVidas();
        for (int i = 0; i < 2400 && voo.getVidas() == vidasIniciais; i++) {
            voo.avancarUmPasso(Comandos.MOTOR);
        }
        assertTrue(voo.getVidas() < vidasIniciais);
        passos(2401);
        assertFalse(canal.enviadas("evento-resultado").get(0).getBoolean("sobreviveu"));
    }

    @Test void aDuracaoDoNucleoGovernaOResultadoMesmoQueOServidorAnuncieOutra() {
        conectar();
        canal.ouvinte.mensagem("{\"tipo\":\"evento-inicio\",\"evento\":\"e-pane\",\"nome\":\"PANE_DE_GRAVIDADE\","
                + "\"semente\":42,\"duracao\":19}");
        passos(2401);
        assertEquals("e-pane", canal.enviadas("evento-resultado").get(0).getString("evento"));
    }

    @Test void pousarMarcaUmaVezEQuemGanhaPodeFicarNaCapsula() {
        conectar();
        for (int i = 0; i < 48; i++) voo.avancarUmPasso(Comandos.MOTOR);
        double esquerda = voo.getNave().getX() - 80;
        canal.ouvinte.mensagem("{\"tipo\":\"evento-inicio\",\"evento\":\"vip-ana\",\"nome\":\"PASSAGEIRO_VIP\","
                + "\"semente\":0,\"duracao\":45,\"vip\":{\"x\":" + esquerda + ",\"superficie\":74}}");
        passos(480);
        assertEquals(List.of("vip-ana"), canal.enviadas("vip").stream().map(m -> m.getString("evento")).toList());
        canal.ouvinte.mensagem("{\"tipo\":\"vip-pego\",\"evento\":\"vip-ana\",\"nome\":\"Ana\"}");
        assertEquals("Ana pegou o VIP", sombras.mensagemDoEvento().orElseThrow());
        assertEquals(voo.getCapsulaVip().orElseThrow(), voo.getNave().getPlataforma());
        assertEquals(0, voo.getPousos());
        assertEquals(0, voo.getPontos());
    }

    @Test void vipUsaCoordenadasDoServidorEConfirmaVencedorDoEventoCorreto() {
        conectar();
        iniciar("vip-12", TipoDeEvento.PASSAGEIRO_VIP);
        passos(1);
        PlataformaDePouso vip = voo.getCapsulaVip().orElseThrow();
        assertEquals(2000, vip.getX());
        assertEquals(116, vip.getSuperficie());
        canal.ouvinte.mensagem("{\"tipo\": \"vip-pego\", \"evento\": \"vip-antigo\", \"nome\": \"Bia\"}");
        assertTrue(voo.getCapsulaVip().isPresent());
        EventoDeVoo pouso = new EventoDeVoo(EventoDeVoo.Tipo.VIP_PEGO, "", 0, 2080, 156);
        sombras.tratarEventos(List.of(pouso, pouso));
        assertEquals(List.of("vip-12"), canal.enviadas("vip").stream().map(m -> m.getString("evento")).toList());
        canal.ouvinte.mensagem("{\"tipo\": \"vip-pego\", \"evento\": \"vip-12\", \"nome\": \"Bia\"}");
        assertTrue(voo.getCapsulaVip().isEmpty());
        assertEquals("Bia pegou o VIP", sombras.mensagemDoEvento().orElseThrow());
        assertEquals(0, voo.getPontos());
    }
}
