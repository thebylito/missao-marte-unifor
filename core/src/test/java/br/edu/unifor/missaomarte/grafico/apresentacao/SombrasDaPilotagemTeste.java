package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem.Situacao;
import br.edu.unifor.missaomarte.grafico.apresentacao.SombrasDaPilotagem.Sombra;
import br.edu.unifor.missaomarte.grafico.online.ConexaoDeSombras;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SombrasDaPilotagemTeste {

    private static final float QUADRO = 1 / 60f;

    /** Conexão falsa: guarda o que o jogo pediu e deixa o teste fazer o papel do servidor. */
    private static final class ConexaoFalsa implements ConexaoDeSombras {
        final List<String> tokens = new ArrayList<>();
        final List<String> enviados = new ArrayList<>();
        Ouvinte ouvinte;
        int fechamentos;

        @Override
        public void abrir(String token, Ouvinte ouvinte) {
            tokens.add(token);
            this.ouvinte = ouvinte;
        }

        @Override
        public void enviar(String texto) {
            enviados.add(texto);
        }

        @Override
        public void fechar() {
            fechamentos++;
        }
    }

    private final PartidaDePilotagem voo = PartidaDePilotagem.nova("Ana", Dificuldade.MEDIO);
    private final ConexaoFalsa conexao = new ConexaoFalsa();
    private final SombrasDaPilotagem sombras = new SombrasDaPilotagem(
            Optional.of(new Sessao("Ana", "token-da-ana")), Optional.of(conexao));

    private void quadros(int n) {
        for (int i = 0; i < n; i++) {
            sombras.atualizar(QUADRO, voo.getNave(), voo.getCaverna());
        }
    }

    private void segundos(float s) {
        quadros(Math.round(s / QUADRO));
    }

    private static String sombra(int piloto, String nome, double x, double y, double angulo, boolean motor) {
        return "{\"tipo\":\"sombra\",\"piloto\":" + piloto + ",\"nome\":\"" + nome + "\",\"x\":" + x + ",\"y\":" + y
                + ",\"angulo\":" + angulo + ",\"motor\":" + motor + "}";
    }

    private Sombra unica() {
        List<Sombra> lista = sombras.getSombras();
        assertEquals(1, lista.size());
        return lista.get(0);
    }

    /** Conecta e recebe uma sombra parada em (x, y). */
    private void conectadoComSombra(double x, double y, double angulo) {
        quadros(1);
        conexao.ouvinte.aberta();
        conexao.ouvinte.mensagem(sombra(7, "Bia", x, y, angulo, false));
    }

    @Test
    void semServidorFicamDesligadasENaoAparecemNoPainel() {
        SombrasDaPilotagem desligadas = SombrasDaPilotagem.desligadas();
        desligadas.atualizar(QUADRO, voo.getNave(), voo.getCaverna());
        assertEquals(Situacao.DESLIGADAS, desligadas.getSituacao());
        assertTrue(desligadas.descricao().isEmpty());
        assertTrue(desligadas.getSombras().isEmpty());
    }

    @Test
    void convidadoNaoConectaEOPainelExplicaPorque() {
        SombrasDaPilotagem convidado = new SombrasDaPilotagem(Optional.empty(), Optional.of(conexao));
        convidado.atualizar(QUADRO, voo.getNave(), voo.getCaverna());
        assertTrue(conexao.tokens.isEmpty());
        assertEquals(Situacao.SEM_LOGIN, convidado.getSituacao());
        assertEquals("Sombras: entre com uma conta", convidado.descricao().orElseThrow());
    }

    @Test
    void conectaNoPrimeiroQuadroComOTokenDoJogador() {
        quadros(1);
        assertEquals(List.of("token-da-ana"), conexao.tokens);
        assertEquals(Situacao.CONECTANDO, sombras.getSituacao());
        assertEquals("Sombras: conectando...", sombras.descricao().orElseThrow());
        quadros(30);
        assertTrue(conexao.enviados.isEmpty());   // antes de abrir, nada sai
        assertEquals(1, conexao.tokens.size());   // e não abre de novo
    }

    @Test
    void depoisDeAbrirMandaAPosicaoDezVezesPorSegundo() {
        quadros(1);
        conexao.ouvinte.aberta();
        quadros(1);
        assertEquals(1, conexao.enviados.size());   // a primeira sai logo
        segundos(1);
        assertTrue(conexao.enviados.size() >= 10 && conexao.enviados.size() <= 12, "enviadas: "
                + conexao.enviados.size());
        String primeira = conexao.enviados.get(0);
        assertTrue(primeira.matches("\\{\"tipo\":\"posicao\",\"x\":-?\\d+\\.\\d,\"y\":-?\\d+\\.\\d,"
                + "\"angulo\":-?\\d+\\.\\d,\"motor\":(true|false)}"), primeira);   // ponto decimal em qualquer idioma
        assertEquals("Sombras: nenhum outro piloto", sombras.descricao().orElseThrow());
    }

    @Test
    void umaSombraNovaApareceOndeEstaEDepoisDeslizaAteANovaPosicao() {
        conectadoComSombra(1000, 500, 0);
        assertEquals(1000, unica().x(), 1e-9);
        assertEquals("Bia", unica().nome());
        assertEquals("Sombras: 1 piloto", sombras.descricao().orElseThrow());

        conexao.ouvinte.mensagem(sombra(7, "Bia", 1100, 500, 0, true));
        quadros(1);
        double x = unica().x();
        assertTrue(x > 1000 && x < 1100, "desliza, não pula: " + x);
        assertTrue(unica().motor());
        segundos(1);
        assertEquals(1100, unica().x(), 0.5);
    }

    @Test
    void oAnguloGiraPeloLadoMaisCurto() {
        conectadoComSombra(1000, 500, 170);
        conexao.ouvinte.mensagem(sombra(7, "Bia", 1000, 500, -170, false));
        quadros(3);
        double a = unica().angulo();
        assertTrue(a > 170 || a < -170, "passa por 180°, não por 0°: " + a);
        segundos(1);
        assertEquals(-170, unica().angulo(), 0.5);
    }

    @Test
    void quemSaiSomeNaHoraEQuemFicaSemNoticiaSomeDepoisDeTresSegundos() {
        conectadoComSombra(1000, 500, 0);
        conexao.ouvinte.mensagem(sombra(8, "Caio", 2000, 500, 0, false));
        assertEquals(2, sombras.getSombras().size());
        assertEquals("Sombras: 2 pilotos", sombras.descricao().orElseThrow());

        conexao.ouvinte.mensagem("{\"tipo\":\"saiu\",\"piloto\":8}");
        assertEquals(7, unica().piloto());

        segundos(SombrasDaPilotagem.TEMPO_SEM_NOTICIA - 0.5f);
        assertEquals(1, sombras.getSombras().size());
        segundos(1);
        assertTrue(sombras.getSombras().isEmpty());
    }

    @Test
    void mensagensQueNaoEntendeSaoIgnoradas() {
        quadros(1);
        conexao.ouvinte.aberta();
        for (String ruim : List.of("não é json", "[]", "42", "{\"tipo\":\"outro\"}", "{\"tipo\":\"sombra\"}",
                "{\"tipo\":\"sombra\",\"piloto\":\"sete\",\"x\":1,\"y\":1,\"angulo\":0}",
                "{\"tipo\":\"sombra\",\"piloto\":1,\"x\":1,\"y\":1}", "{\"tipo\":\"saiu\"}")) {
            conexao.ouvinte.mensagem(ruim);
        }
        assertTrue(sombras.getSombras().isEmpty());
        assertEquals(Situacao.CONECTADAS, sombras.getSituacao());
    }

    @Test
    void nomeCompridoECortado() {
        conectadoComSombra(1000, 500, 0);
        conexao.ouvinte.mensagem(sombra(7, "N".repeat(50), 1000, 500, 0, false));
        assertEquals(SombrasDaPilotagem.NOME_MAXIMO, unica().nome().length());
    }

    @Test
    void seAConexaoCaiEsqueceAsSombrasETentaDeNovoDepoisDeTresSegundos() {
        conectadoComSombra(1000, 500, 0);
        conexao.ouvinte.fechada("1006");
        assertEquals(Situacao.SEM_CONEXAO, sombras.getSituacao());
        assertEquals("Sombras: sem conexão", sombras.descricao().orElseThrow());
        assertTrue(sombras.getSombras().isEmpty());

        segundos(SombrasDaPilotagem.ESPERA_PARA_RECONECTAR - 0.5f);
        assertEquals(1, conexao.tokens.size());
        segundos(1);
        assertEquals(2, conexao.tokens.size());
        assertEquals(Situacao.CONECTANDO, sombras.getSituacao());
    }

    @Test
    void aoFecharDesligaAConexaoEIgnoraOQueChegarDepois() {
        conectadoComSombra(1000, 500, 0);
        ConexaoDeSombras.Ouvinte ouvinte = conexao.ouvinte;
        sombras.fechar();
        sombras.fechar();   // fechar duas vezes não fecha duas vezes
        assertEquals(1, conexao.fechamentos);
        assertTrue(sombras.getSombras().isEmpty());

        ouvinte.mensagem(sombra(9, "Davi", 10, 10, 0, false));
        ouvinte.fechada("1000");
        int enviadas = conexao.enviados.size();
        segundos(5);
        assertTrue(sombras.getSombras().isEmpty());
        assertEquals(enviadas, conexao.enviados.size());
        assertEquals(1, conexao.tokens.size());   // não reconecta
    }

    @Test
    void normalizarDeixaOAnguloEntreMenos180E180() {
        assertEquals(10, SombrasDaPilotagem.normalizar(370), 1e-9);
        assertEquals(-170, SombrasDaPilotagem.normalizar(190), 1e-9);
        assertEquals(180, SombrasDaPilotagem.normalizar(-180), 1e-9);
        assertEquals(0, SombrasDaPilotagem.normalizar(-360), 1e-9);
        assertFalse(Double.isNaN(SombrasDaPilotagem.normalizar(1e9)));
    }
}
