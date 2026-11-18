package br.edu.unifor.missaomarte.grafico.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Acao;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Etapa;
import br.edu.unifor.missaomarte.grafico.apresentacao.ControladorDaEntrada.Modo;
import br.edu.unifor.missaomarte.grafico.online.ConfiguracaoDoServidor;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import com.badlogic.gdx.Input.Keys;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tela de entrada: convidado, login, cadastro e as falhas do servidor, sem janela e sem servidor de verdade. */
class ControladorDaEntradaTeste {

    private final Apoio.ServidorFalso servidor = new Apoio.ServidorFalso();
    private final ControladorDaEntrada entrada = new ControladorDaEntrada(servidor);

    /** Simula o teclado: cada caractere gera keyDown (ignorado pelo formulário) e keyTyped. */
    private void digitar(String texto) {
        for (char c : texto.toCharArray()) {
            entrada.digitar(c);
        }
    }

    private void abrir(int tecla) {
        entrada.teclaPressionada(tecla);
        entrada.digitar((char) ('0' + (tecla - Keys.NUM_0)));   // o caractere da própria tecla, que deve ser ignorado
    }

    private void preencherEEnviar(String nome, String senha) {
        digitar(nome);
        entrada.teclaPressionada(Keys.ENTER);    // do nome para a senha
        digitar(senha);
        entrada.teclaPressionada(Keys.ENTER);    // envia
    }

    @Test
    void tresEntraComoConvidadoSemFalarComOServidor() {
        assertEquals(Acao.CONVIDADO, entrada.teclaPressionada(Keys.NUM_3));
        assertTrue(servidor.pedidos.isEmpty());
    }

    @Test
    void outraTeclaNaEscolhaPedeUmaOpcaoValida() {
        assertEquals(Acao.NENHUMA, entrada.teclaPressionada(Keys.X));
        assertEquals("Escolha 1, 2, 3 ou 4.", entrada.getMensagem());
        assertEquals(Etapa.ESCOLHA, entrada.getEtapa());
    }

    @Test
    void aTeclaQueAbriuOFormularioNaoViraParteDoNome() {
        abrir(Keys.NUM_1);
        digitar("Ana");
        assertEquals("Ana", entrada.getNome());
        assertEquals(Modo.ENTRAR, entrada.getModo());
    }

    @Test
    void loginCertoDevolveASessao() {
        abrir(Keys.NUM_1);
        preencherEEnviar("Ana", "segredo1");
        assertEquals(List.of("entrar Ana segredo1"), servidor.pedidos);
        assertEquals(Etapa.AGUARDANDO, entrada.getEtapa());
        assertTrue(entrada.getSessao().isEmpty(), "ainda esperando o servidor");

        servidor.login.sucesso(new Sessao("Ana", "tok"));
        assertEquals(new Sessao("Ana", "tok"), entrada.getSessao().orElseThrow());
    }

    @Test
    void senhaApareceSoComoAsteriscos() {
        abrir(Keys.NUM_1);
        digitar("Ana");
        entrada.teclaPressionada(Keys.TAB);
        digitar("segredo");
        assertEquals("*******", entrada.getSenhaMascarada());
        assertTrue(entrada.isEditandoSenha());
    }

    @Test
    void loginErradoVoltaAoFormularioComAMensagemEApagaASenha() {
        abrir(Keys.NUM_1);
        preencherEEnviar("Ana", "errada");
        servidor.login.falha(new Falha(Falha.Motivo.NAO_AUTORIZADO, "Nome ou senha incorretos."));
        assertEquals(Etapa.FORMULARIO, entrada.getEtapa());
        assertEquals("Nome ou senha incorretos.", entrada.getMensagem());
        assertEquals("Ana", entrada.getNome());
        assertEquals("", entrada.getSenhaMascarada());
        assertTrue(entrada.getSessao().isEmpty());
    }

    @Test
    void servidorForaDoArSugereOModoConvidado() {
        abrir(Keys.NUM_1);
        preencherEEnviar("Ana", "segredo1");
        servidor.login.falha(Falha.semConexao());
        assertEquals("Servidor indisponível. Você pode jogar como convidado.", entrada.getMensagem());
        entrada.teclaPressionada(Keys.ESCAPE);
        assertEquals(Acao.CONVIDADO, entrada.teclaPressionada(Keys.NUM_3));
    }

    @Test
    void cadastroCertoJaEntraEmSeguida() {
        abrir(Keys.NUM_2);
        preencherEEnviar("Bia", "segredo2");
        assertEquals(List.of("cadastrar Bia segredo2"), servidor.pedidos);
        servidor.cadastro.sucesso(null);
        assertEquals(List.of("cadastrar Bia segredo2", "entrar Bia segredo2"), servidor.pedidos);
        servidor.login.sucesso(new Sessao("Bia", "tok-b"));
        assertEquals("Bia", entrada.getSessao().orElseThrow().nome());
    }

    @Test
    void nomeEmUsoNoCadastroMostraAMensagemDoServidor() {
        abrir(Keys.NUM_2);
        preencherEEnviar("Ana", "segredo1");
        servidor.cadastro.falha(new Falha(Falha.Motivo.CONFLITO, "O nome \"Ana\" já está em uso."));
        assertEquals("O nome \"Ana\" já está em uso.", entrada.getMensagem());
        assertEquals(1, servidor.pedidos.size(), "não tenta entrar depois de um cadastro recusado");
    }

    @Test
    void camposVaziosNaoVaoAoServidor() {
        abrir(Keys.NUM_1);
        entrada.teclaPressionada(Keys.ENTER);
        entrada.teclaPressionada(Keys.ENTER);
        assertEquals("Preencha o nome e a senha.", entrada.getMensagem());
        assertTrue(servidor.pedidos.isEmpty());
    }

    @Test
    void enquantoAguardaAsTeclasSaoIgnoradas() {
        abrir(Keys.NUM_1);
        preencherEEnviar("Ana", "segredo1");
        assertEquals(Acao.NENHUMA, entrada.teclaPressionada(Keys.NUM_3));
        entrada.teclaPressionada(Keys.ENTER);
        assertEquals(1, servidor.pedidos.size());
    }

    @Test
    void escVoltaAEscolhaELimpaOsCampos() {
        abrir(Keys.NUM_1);
        digitar("Ana");
        entrada.teclaPressionada(Keys.ESCAPE);
        assertEquals(Etapa.ESCOLHA, entrada.getEtapa());
        abrir(Keys.NUM_2);
        assertEquals("", entrada.getNome());
    }

    @Test
    void backspaceApagaNoCampoAtualEOsLimitesSaoRespeitados() {
        abrir(Keys.NUM_1);
        digitar("Anaa");
        entrada.teclaPressionada(Keys.BACKSPACE);
        assertEquals("Ana", entrada.getNome());
        digitar("x".repeat(40));
        assertEquals(ControladorDaEntrada.NOME_MAXIMO, entrada.getNome().length());
    }

    // ------------------------------------------------------------------ Fase 6: servidor e toque

    @Test
    void quatroEditaOEnderecoDoServidorEEnterGrava() {
        ConfiguracaoDoServidor config = ConfiguracaoDoServidor.emMemoria("http://10.0.2.2:8080");
        ControladorDaEntrada e = new ControladorDaEntrada(servidor, config);
        e.teclaPressionada(Keys.NUM_4);
        e.digitar('4');                                   // o caractere da própria tecla é ignorado
        assertEquals(Etapa.SERVIDOR, e.getEtapa());
        assertEquals("http://10.0.2.2:8080", e.getEnderecoEditado());
        for (int i = 0; i < "10.0.2.2:8080".length(); i++) {
            e.teclaPressionada(Keys.BACKSPACE);
        }
        for (char c : "192.168.0.10:8080".toCharArray()) {
            e.digitar(c);
        }
        e.teclaPressionada(Keys.ENTER);
        assertEquals(Etapa.ESCOLHA, e.getEtapa());
        assertEquals("http://192.168.0.10:8080", config.endereco());
        assertEquals("Servidor: http://192.168.0.10:8080", e.getMensagem());
    }

    @Test
    void enderecoInvalidoContinuaEditandoComOMotivo() {
        ConfiguracaoDoServidor config = ConfiguracaoDoServidor.emMemoria("http://localhost:8080");
        ControladorDaEntrada e = new ControladorDaEntrada(servidor, config);
        e.salvarServidor("192.168.0.10:8080");
        assertEquals(Etapa.SERVIDOR, e.getEtapa());
        assertEquals("Use http:// ou https:// no começo do endereço.", e.getMensagem());
        assertEquals("http://localhost:8080", config.endereco());
        e.teclaPressionada(Keys.ESCAPE);
        assertEquals(Etapa.ESCOLHA, e.getEtapa());
    }

    @Test
    void enderecoDaLinhaDeComandoNaoAbreAEdicao() {
        ConfiguracaoDoServidor.Preferencias vazias = new ConfiguracaoDoServidor.Preferencias() {
            @Override public String ler(String chave) { return null; }
            @Override public void gravar(String chave, String valor) { }
        };
        ControladorDaEntrada e = new ControladorDaEntrada(servidor,
                new ConfiguracaoDoServidor(vazias, "http://localhost:8080", "http://forcado:9090"));
        e.teclaPressionada(Keys.NUM_4);
        assertEquals(Etapa.ESCOLHA, e.getEtapa());
        assertEquals("Servidor definido ao abrir o jogo: http://forcado:9090", e.getMensagem());
    }

    @Test
    void toqueCaixaDeTextoDoCelularPreencheOsCamposEConfirmarEnvia() {
        abrir(Keys.NUM_1);
        entrada.definirNome("  Ana  ");
        entrada.definirSenha("segredo1");
        assertEquals("Ana", entrada.getNome());
        assertEquals("********", entrada.getSenhaMascarada());
        entrada.confirmar();
        assertEquals(List.of("entrar Ana segredo1"), servidor.pedidos);
    }

    @Test
    void toqueNumCampoMudaOFocoDoTeclado() {
        abrir(Keys.NUM_1);
        entrada.focar(true);
        digitar("abc");
        assertEquals("***", entrada.getSenhaMascarada());
        entrada.focar(false);
        digitar("Bia");
        assertEquals("Bia", entrada.getNome());
    }
}
