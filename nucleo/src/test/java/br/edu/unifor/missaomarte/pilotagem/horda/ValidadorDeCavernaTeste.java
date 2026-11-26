package br.edu.unifor.missaomarte.pilotagem.horda;

import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.PONTO_DO_PROFESSOR;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comAsteroides;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comBase;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comId;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comPontos;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comRochas;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.comTamanho;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.marcianaDescrita;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.mais;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.sem;
import static br.edu.unifor.missaomarte.pilotagem.horda.CavernasDeTeste.trocar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unifor.missaomarte.dominio.Posicao;
import br.edu.unifor.missaomarte.dominio.Professor;
import br.edu.unifor.missaomarte.pilotagem.Caverna;
import br.edu.unifor.missaomarte.pilotagem.PlataformaDePouso;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * As regras do validador de cavernas (CT-03 da análise da Fase 8). Cada teste quebra de propósito uma regra na
 * caverna marciana descrita, que é válida. Os índices das mensagens são os das listas da descrição (os mesmos do
 * JSON).
 */
class ValidadorDeCavernaTeste {

    private static final DescricaoDeCaverna MARCIANA = marcianaDescrita();

    /** A caverna tem exatamente estes problemas. */
    private static void soEstes(DescricaoDeCaverna c, String... problemas) {
        assertEquals(List.of(problemas), ValidadorDeCaverna.problemas(c));
    }

    /** A caverna tem, entre outros, este problema. */
    private static void temProblema(DescricaoDeCaverna c, String problema) {
        List<String> problemas = ValidadorDeCaverna.problemas(c);
        assertTrue(problemas.contains(problema), () -> "esperava \"" + problema + "\" em " + problemas);
    }

    // ------------------------------------------------------------------ a caverna válida

    @Test
    void aMarcianaDescritaEValida() {
        soEstes(MARCIANA);
    }

    @Test
    void aPlanicieDeTesteEValida() {
        soEstes(CavernasDeTeste.planicie());
    }

    @Test
    void osTamanhosDasPlataformasSaoOsDaCaverna() {
        Caverna c = new Caverna(1000, 1000).base(0, 100);
        PlataformaDePouso base = c.getBase();
        assertEquals(base.getLargura(), ValidadorDeCaverna.LARGURA_DA_BASE);
        assertEquals(base.getAltura(), ValidadorDeCaverna.ALTURA_DA_BASE);
        c.passageiro(new Professor("Dr. Silva", Posicao.ORIGEM), 500, 100);
        PlataformaDePouso passageiro = c.getPlataformas().get(1);
        assertEquals(passageiro.getLargura(), ValidadorDeCaverna.LARGURA_DA_PLATAFORMA);
        assertEquals(passageiro.getAltura(), ValidadorDeCaverna.ALTURA_DA_PLATAFORMA);
    }

    // ------------------------------------------------------------------ cabeçalho

    @Test
    void aLarguraPrecisaSerPositiva() {
        temProblema(comTamanho(MARCIANA, 0, 1440), "a largura da caverna precisa ser maior que zero (veio 0)");
    }

    @Test
    void aAlturaPrecisaSerPositiva() {
        temProblema(comTamanho(MARCIANA, 3840, -1), "a altura da caverna precisa ser maior que zero (veio -1)");
    }

    @Test
    void oIdNaoPodeSerVazio() {
        soEstes(comId(MARCIANA, " ", "Caverna Marciana"), "o id da caverna está vazio");
        soEstes(comId(MARCIANA, null, "Caverna Marciana"), "o id da caverna está vazio");
    }

    @Test
    void oNomeNaoPodeSerVazio() {
        soEstes(comId(MARCIANA, "marciana", ""), "o nome da caverna está vazio");
        soEstes(comId(MARCIANA, "marciana", null), "o nome da caverna está vazio");
    }

    @Test
    void precisaDePeloMenosTresPontosDePouso() {
        soEstes(comPontos(MARCIANA, MARCIANA.pontosDePouso().subList(0, 2)),
                "a caverna precisa de pelo menos 3 pontos de pouso (tem 2)");
    }

    @Test
    void precisaDeBase() {
        soEstes(comBase(MARCIANA, null), "a caverna não tem base");
    }

    // ------------------------------------------------------------------ rochas

    @Test
    void rochaQuePassaDaDireitaSaiDosLimites() {
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(3700, 100, 200, 50))),
                "rochas[11] sai dos limites da caverna");
    }

    @Test
    void rochaComXNegativoSaiDosLimites() {
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(-10, 500, 20, 20))),
                "rochas[11] sai dos limites da caverna");
    }

    @Test
    void rochaPrecisaDeTamanhoPositivo() {
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(500, 500, 0, 10))),
                "rochas[11] precisa de largura e altura maiores que zero");
    }

    // ------------------------------------------------------------------ plataformas: limites e sobreposição

    @Test
    void baseForaDosLimites() {
        temProblema(comBase(MARCIANA, new PontoDePouso(-10, 128)), "base: a plataforma sai dos limites da caverna");
    }

    @Test
    void pontoForaDosLimites() {
        temProblema(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 2, new PontoDePouso(3700, 116))),
                "pontosDePouso[2]: a plataforma sai dos limites da caverna");
    }

    @Test
    void baseDentroDaRocha() {
        temProblema(comBase(MARCIANA, new PontoDePouso(700, 128)), "base: a plataforma invade rochas[4]");
    }

    @Test
    void pontoDentroDaRocha() {
        // o bloco do Astronauta tem topo em 480: com a superfície em 500, o corpo (464 a 500) entra 16 nele
        soEstes(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 0, new PontoDePouso(1480, 500))),
                "pontosDePouso[0]: a plataforma invade rochas[6]");
    }

    @Test
    void pontoEmCimaDaBase() {
        temProblema(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 3, new PontoDePouso(400, 116))),
                "pontosDePouso[3]: a plataforma invade a base");
    }

    @Test
    void pontoEmCimaDeOutroPonto() {
        soEstes(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 4, new PontoDePouso(2050, 116))),
                "pontosDePouso[4]: a plataforma invade pontosDePouso[3]");
    }

    @Test
    void plataformaApoiadaNaRochaNaoInvade() {
        // encostar (o corpo do Professor começa no topo do chão, em 80) não é invadir
        soEstes(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 3, new PontoDePouso(1000, 116))));
    }

    // ------------------------------------------------------------------ folga acima da plataforma

    @Test
    void rochaNaFolgaDeUmPonto() {
        // superfície do Professor em 116: a folga vai até 236, e a rocha começa em 216
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(3400, 216, 50, 50))),
                "pontosDePouso[2]: a folga de 120 acima da plataforma esbarra em rochas[11]");
    }

    @Test
    void rochaLogoAcimaDaFolgaNaoAtrapalha() {
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(3400, 236, 50, 50))));
    }

    @Test
    void rochaNaFolgaDaBase() {
        soEstes(comRochas(MARCIANA, mais(MARCIANA.rochas(), new RochaDescrita(300, 200, 50, 50))),
                "base: a folga de 120 acima da plataforma esbarra em rochas[11]");
    }

    @Test
    void folgaQueSaiPorCima() {
        // sem o teto (rochas[1]), um ponto com a superfície em 1400 não tem 120 até o limite (1440)
        DescricaoDeCaverna semTeto = comRochas(MARCIANA, sem(MARCIANA.rochas(), 1));
        soEstes(comPontos(semTeto, trocar(semTeto.pontosDePouso(), 3, new PontoDePouso(600, 1400))),
                "pontosDePouso[3]: a folga de 120 acima da plataforma sai dos limites da caverna");
    }

    @Test
    void plataformaNaFolgaDeOutra() {
        // um ponto flutuando 100 acima de outro: os corpos não se tocam, mas a nave não desce no de baixo
        soEstes(comPontos(MARCIANA, trocar(MARCIANA.pontosDePouso(), 4, new PontoDePouso(2000, 216))),
                "pontosDePouso[3]: a folga de 120 acima da plataforma esbarra em pontosDePouso[4]");
    }

    // ------------------------------------------------------------------ asteroides

    private static DescricaoDeCaverna comAsteroide(int posicao, AsteroideDescrito a) {
        return comAsteroides(MARCIANA, trocar(MARCIANA.asteroides(), posicao, a));
    }

    @Test
    void orbitaQueSaiDosLimites() {
        // amplitude vertical 600 em torno de 520: desce até -116
        soEstes(comAsteroide(0, new AsteroideDescrito(1040, 520, 36, 0, 600, 6, 0, 40)),
                "asteroides[0]: a órbita sai dos limites da caverna");
    }

    @Test
    void orbitaQuePassaPelaFolgaDeUmaPlataforma() {
        // o asteroide perto do Professor, com amplitude vertical 400, desce até 168: dentro da folga (116 a 236)
        soEstes(comAsteroide(2, new AsteroideDescrito(3240, 600, 32, 120, 400, 5, 2.4, 70)),
                "asteroides[2]: a órbita passa pela folga de pontosDePouso[" + PONTO_DO_PROFESSOR + "]");
    }

    @Test
    void orbitaQuePassaPeloCorpoDeUmaPlataforma() {
        // rente ao chão, na altura do corpo do Professor (80 a 116) e abaixo da folga
        soEstes(comAsteroide(2, new AsteroideDescrito(3300, 100, 10, 100, 0, 5, 0, 0)),
                "asteroides[2]: a órbita passa pela plataforma de pontosDePouso[" + PONTO_DO_PROFESSOR + "]");
    }

    @Test
    void orbitaQuePassaPelaBase() {
        soEstes(comAsteroide(0, new AsteroideDescrito(388, 400, 30, 0, 200, 6, 0, 40)),
                "asteroides[0]: a órbita passa pela folga da base");
    }

    @Test
    void aAmplitudeNegativaContaComoPositiva() {
        soEstes(comAsteroide(0, new AsteroideDescrito(1040, 520, 36, 0, -600, 6, 0, 40)),
                "asteroides[0]: a órbita sai dos limites da caverna");
    }

    @Test
    void oPeriodoPrecisaSerPositivo() {
        soEstes(comAsteroide(1, new AsteroideDescrito(2480, 560, 40, 220, 0, 0, 1.2, -55)),
                "asteroides[1]: o período precisa ser maior que zero (veio 0)");
    }

    // ------------------------------------------------------------------ vários problemas

    @Test
    void listaTodosOsProblemas() {
        DescricaoDeCaverna c = comAsteroides(comId(MARCIANA, ""),
                trocar(MARCIANA.asteroides(), 1, new AsteroideDescrito(2480, 560, 40, 220, 0, -2, 1.2, -55)));
        soEstes(c, "o id da caverna está vazio", "asteroides[1]: o período precisa ser maior que zero (veio -2)");
    }
}
