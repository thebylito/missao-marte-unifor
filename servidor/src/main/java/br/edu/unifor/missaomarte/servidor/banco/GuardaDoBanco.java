package br.edu.unifor.missaomarte.servidor.banco;

import java.util.Map;

/**
 * Fase 8 (DA-41, revisão da etapa 1b): na imagem Docker ({@code MISSAOMARTE_PERMITIR_H2=false}), o servidor só sobe
 * com o PostgreSQL configurado. Sem {@code MISSAOMARTE_DB_URL}, ele cairia no H2 em arquivo: antes da virada, abriria
 * o banco de produção da Fase 7; depois, criaria um banco novo e vazio, e cadastros novos ganhariam ids já usados (o
 * token antigo de um jogador, que só guarda o id, entraria na conta de outra pessoa). A checagem roda no
 * {@code main}, antes de qualquer conexão. Fora da imagem (./gradlew bootRun, testes), o H2 continua permitido.
 */
public final class GuardaDoBanco {

    private GuardaDoBanco() {
    }

    public static void verificar(Map<String, String> ambiente) {
        boolean semH2 = "false".equalsIgnoreCase(ambiente.getOrDefault("MISSAOMARTE_PERMITIR_H2", "").trim());
        boolean semUrl = ambiente.getOrDefault("MISSAOMARTE_DB_URL", "").isBlank();
        if (semH2 && semUrl) {
            throw new IllegalStateException("MISSAOMARTE_DB_URL não está definida, e esta imagem não usa o H2 "
                    + "(MISSAOMARTE_PERMITIR_H2=false). Defina MISSAOMARTE_DB_URL, MISSAOMARTE_DB_USUARIO e "
                    + "MISSAOMARTE_DB_SENHA (servidor/PUBLICACAO.md, seção 10)");
        }
    }
}
