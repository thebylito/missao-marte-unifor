package br.edu.unifor.missaomarte.servidor.banco;

import org.hibernate.dialect.H2Dialect;

/**
 * Fase 8 (DA-40): o dialeto do H2 sem a lista fechada de valores nas colunas de enum.
 *
 * <p>Mesmo com a coluna em texto ({@code @JdbcTypeCode(SqlTypes.VARCHAR)}), o Hibernate 7.4 cria cada coluna
 * {@code @Enumerated(EnumType.STRING)} com {@code check (modo in ('CLASSICO','PILOTAGEM'))}, e o
 * {@code ddl-auto=update} nunca mais muda essa lista. Num banco criado do zero, um valor novo do enum (a Horda, um
 * tipo novo de moeda) seria recusado para sempre. Sem a condição, quem valida o valor é o enum do Java, na entrada da
 * API. Ligado por {@code spring.jpa.database-platform} no {@code application.properties}.
 */
public class H2SemCheckDeEnum extends H2Dialect {

    @Override
    public String getCheckCondition(String coluna, Class<? extends Enum<?>> tipo) {
        return null;
    }

    @Override
    public String getCheckCondition(String coluna, String[] valores) {
        return null;
    }
}
