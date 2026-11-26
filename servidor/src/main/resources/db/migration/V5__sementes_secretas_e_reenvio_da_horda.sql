-- Hordas e eventos publicados por instâncias anteriores conservam suas sementes originais.
alter table hordas add column semente_agenda varchar(64);
alter table eventos_ao_vivo add column semente_vip varchar(64);

-- Guarda os campos variáveis da resposta original junto do bilhete usado.
alter table bilhetes add column moedas_ganhas_no_registro integer;
alter table bilhetes add column saldo_no_registro integer;
alter table bilhetes add column colocacao_no_registro integer;
