-- Fase 8 (DA-40): as colunas de enum da tabela de partidas passam de ENUM nativo do H2 para texto.
-- O Hibernate 7.4 criou MODO, RESULTADO e DIFICULDADE como ENUM('...'), que recusa qualquer valor novo (a Horda),
-- e o ddl-auto=update nunca muda o tipo de uma coluna que já existe. Este script roda em toda subida, ANTES do
-- Hibernate (spring.sql.init), e pode rodar de novo sem efeito: num banco novo a tabela ainda não existe (IF EXISTS),
-- e numa coluna que já é texto a conversão não muda nada. Os valores gravados, o default e o NOT NULL ficam.
ALTER TABLE IF EXISTS PARTIDAS ALTER COLUMN IF EXISTS MODO SET DATA TYPE CHARACTER VARYING(20);
ALTER TABLE IF EXISTS PARTIDAS ALTER COLUMN IF EXISTS RESULTADO SET DATA TYPE CHARACTER VARYING(20);
ALTER TABLE IF EXISTS PARTIDAS ALTER COLUMN IF EXISTS DIFICULDADE SET DATA TYPE CHARACTER VARYING(20);
