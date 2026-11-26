package br.edu.unifor.missaomarte.servidor.ajustes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.unifor.missaomarte.pilotagem.AjustesDaDificuldade;
import br.edu.unifor.missaomarte.pilotagem.AjustesDeVoo;
import br.edu.unifor.missaomarte.pilotagem.AjustesDosEventos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;

/** Fase 8 (DA-48): as variáveis MISSAOMARTE_AJUSTE_* viram os ajustes do jogo, ou impedem o servidor de subir. */
class AjustesDoAmbienteTeste {

    private record Caso(String nome, String valor, ToDoubleFunction<AjustesDeVoo> campo, double esperado) {}

    private static final List<Caso> CASOS = List.of(
            new Caso("MISSAOMARTE_AJUSTE_FACIL_TANQUE", "140", a -> a.facil().tanque(), 140),
            new Caso("MISSAOMARTE_AJUSTE_FACIL_QUEDA", "95", a -> a.facil().quedaMaxima(), 95),
            new Caso("MISSAOMARTE_AJUSTE_FACIL_DESLIZE", "65", a -> a.facil().deslizeMaximo(), 65),
            new Caso("MISSAOMARTE_AJUSTE_FACIL_INCLINACAO", "28", a -> a.facil().inclinacaoMaxima(), 28),
            new Caso("MISSAOMARTE_AJUSTE_FACIL_ASTEROIDES", "0.8", a -> a.facil().velocidadeDosAsteroides(), 0.8),
            new Caso("MISSAOMARTE_AJUSTE_MEDIO_TANQUE", "110", a -> a.medio().tanque(), 110),
            new Caso("MISSAOMARTE_AJUSTE_MEDIO_QUEDA", "80", a -> a.medio().quedaMaxima(), 80),
            new Caso("MISSAOMARTE_AJUSTE_MEDIO_DESLIZE", "55", a -> a.medio().deslizeMaximo(), 55),
            new Caso("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO", "22", a -> a.medio().inclinacaoMaxima(), 22),
            new Caso("MISSAOMARTE_AJUSTE_MEDIO_ASTEROIDES", "1.1", a -> a.medio().velocidadeDosAsteroides(), 1.1),
            new Caso("MISSAOMARTE_AJUSTE_DIFICIL_TANQUE", "120", a -> a.dificil().tanque(), 120),
            new Caso("MISSAOMARTE_AJUSTE_DIFICIL_QUEDA", "70", a -> a.dificil().quedaMaxima(), 70),
            new Caso("MISSAOMARTE_AJUSTE_DIFICIL_DESLIZE", "45", a -> a.dificil().deslizeMaximo(), 45),
            new Caso("MISSAOMARTE_AJUSTE_DIFICIL_INCLINACAO", "18", a -> a.dificil().inclinacaoMaxima(), 18),
            new Caso("MISSAOMARTE_AJUSTE_DIFICIL_ASTEROIDES", "1.2", a -> a.dificil().velocidadeDosAsteroides(), 1.2),
            new Caso("MISSAOMARTE_AJUSTE_METEOROS_MIN", "160", a -> a.eventos().velocidadeMinimaDosMeteoros(), 160),
            new Caso("MISSAOMARTE_AJUSTE_METEOROS_MAX", "260", a -> a.eventos().velocidadeMaximaDosMeteoros(), 260),
            new Caso("MISSAOMARTE_AJUSTE_DURACAO_CHUVA", "30", a -> a.eventos().duracaoDaChuva(), 30),
            new Caso("MISSAOMARTE_AJUSTE_DURACAO_TEMPESTADE", "35", a -> a.eventos().duracaoDaTempestade(), 35),
            new Caso("MISSAOMARTE_AJUSTE_DURACAO_PANE", "25", a -> a.eventos().duracaoDaPane(), 25),
            new Caso("MISSAOMARTE_AJUSTE_DURACAO_VIP", "60", a -> a.eventos().duracaoDoVip(), 60));

    @Test
    void semVariaveisDeAjusteFicaOPadrao() {
        assertThat(AjustesDoAmbiente.ler(Map.of("PATH", "/bin", "MISSAOMARTE_DB_URL", "jdbc:postgresql://x/y",
                "MISSAOMARTE_ADMINS", "thebylito"))).isEqualTo(AjustesDeVoo.PADRAO);
    }

    @Test
    void cadaVariavelMudaSoOCampoDela() {
        for (Caso caso : CASOS) {
            AjustesDeVoo lidos = AjustesDoAmbiente.ler(Map.of(caso.nome(), caso.valor()));

            assertThat(caso.campo().applyAsDouble(lidos)).as(caso.nome()).isEqualTo(caso.esperado());
            List<Double> antes = folhas(AjustesDeVoo.PADRAO);
            List<Double> depois = folhas(lidos);
            int diferentes = 0;
            for (int i = 0; i < antes.size(); i++) {
                diferentes += antes.get(i).equals(depois.get(i)) ? 0 : 1;
            }
            assertThat(diferentes).as(caso.nome() + " muda só um número").isEqualTo(1);
        }
    }

    @Test
    void osNomesAceitosSaoOsVinteEUmDaTabela() {
        assertThat(AjustesDoAmbiente.NOMES).containsExactlyElementsOf(CASOS.stream().map(Caso::nome).toList());
    }

    @Test
    void aceitaVirgulaEEspacosEIgnoraValorVazio() {
        AjustesDeVoo lidos = AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_FACIL_ASTEROIDES", " 0,9 ",
                "MISSAOMARTE_AJUSTE_MEDIO_TANQUE", "  "));

        assertThat(lidos.facil().velocidadeDosAsteroides()).isEqualTo(0.9);
        assertThat(lidos.medio().tanque()).isEqualTo(100);
    }

    @Test
    void nomeDesconhecidoImpedeASubida() {
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAOO", "30")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAOO")
                .hasMessageContaining("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO,")
                .hasMessageContaining("PUBLICACAO.md");
    }

    /** Revisão do PR #33: um erro no começo do nome também precisa impedir a subida, e não ser ignorado. */
    @Test
    void prefixoComErroDeDigitacaoTambemImpedeASubida() {
        for (String nome : java.util.List.of("MISSAOMARTE_AJUSTES_MEDIO_TANQUE", "missaomarte_ajuste_medio_tanque",
                "MISSAO_MARTE_AJUSTE_MEDIO_TANQUE")) {
            assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of(nome, "110")))
                    .as(nome).isInstanceOf(IllegalStateException.class).hasMessageContaining(nome);
        }
        assertThat(AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_ADMINS", "x", "MISSAOMARTE_DB_URL", "y")))
                .isEqualTo(AjustesDeVoo.PADRAO);
    }

    @Test
    void valorQueNaoENumeroImpedeASubida() {
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_MEDIO_QUEDA", "rápido")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_MEDIO_QUEDA")
                .hasMessageContaining("rápido");
    }

    @Test
    void valorForaDaFaixaImpedeASubidaComONomeDaVariavel() {
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO", "46")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_MEDIO_INCLINACAO")
                .hasMessageContaining("entre 5 e 45 (veio 46)");
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_DIFICIL_TANQUE", "NaN")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_DIFICIL_TANQUE");
    }

    @Test
    void meteoroMinimoAcimaDoMaximoImpedeASubida() {
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_METEOROS_MIN", "300")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_METEOROS_MIN")
                .hasMessageContaining("menor que a máxima");
    }

    /** O canal manda a duração como número inteiro de segundos (o mesmo formato desde a etapa 6). */
    @Test
    void duracaoPrecisaSerUmNumeroInteiroDeSegundos() {
        assertThatThrownBy(() -> AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_DURACAO_CHUVA", "22.5")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSAOMARTE_AJUSTE_DURACAO_CHUVA")
                .hasMessageContaining("inteiro");
        assertThat(AjustesDoAmbiente.ler(Map.of("MISSAOMARTE_AJUSTE_DURACAO_CHUVA", "22")).eventos().duracaoDaChuva())
                .isEqualTo(22);
    }

    private static List<Double> folhas(AjustesDeVoo a) {
        List<Double> folhas = new ArrayList<>();
        for (AjustesDaDificuldade d : List.of(a.facil(), a.medio(), a.dificil())) {
            folhas.addAll(List.of(d.tanque(), d.quedaMaxima(), d.deslizeMaximo(), d.inclinacaoMaxima(),
                    d.velocidadeDosAsteroides()));
        }
        AjustesDosEventos e = a.eventos();
        folhas.addAll(List.of(e.velocidadeMinimaDosMeteoros(), e.velocidadeMaximaDosMeteoros(), e.duracaoDaChuva(),
                e.duracaoDaTempestade(), e.duracaoDaPane(), e.duracaoDoVip()));
        return folhas;
    }
}
