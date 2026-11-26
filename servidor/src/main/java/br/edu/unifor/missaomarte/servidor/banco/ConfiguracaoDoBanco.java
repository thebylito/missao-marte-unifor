package br.edu.unifor.missaomarte.servidor.banco;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fase 8 (DA-41): a subida do banco. Primeiro as migrações do Flyway; depois, se {@code MISSAOMARTE_IMPORTAR_H2}
 * apontar para o H2 da Fase 7, a importação ({@link ImportadorDoH2}).
 *
 * <p>Por que aqui, e não num {@code ApplicationRunner}: a estratégia de migração roda dentro da inicialização do
 * Flyway, da qual o JPA depende. Então a importação termina antes de o Hibernate conferir o esquema e muito antes de
 * o Tomcat começar a aceitar conexões, que é o último passo da subida. Nenhum pedido vê o banco pela metade, e uma
 * importação que falha derruba a subida em vez de deixar o servidor no ar sem os dados.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoDoBanco {

    @Bean
    FlywayMigrationStrategy migrarEImportar(DataSource banco, @Value("${missaomarte.importar-h2:}") String origem) {
        return flyway -> {
            flyway.migrate();
            if (!origem.isBlank()) {
                new ImportadorDoH2(banco).importar(Path.of(origem.strip()));
            }
        };
    }
}
