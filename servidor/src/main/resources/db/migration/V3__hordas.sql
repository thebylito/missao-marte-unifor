create table hordas (
    numero bigint primary key,
    inicio timestamp(6) with time zone not null,
    fim timestamp(6) with time zone not null,
    semente bigint not null,
    caverna varchar(64) not null,
    dificuldade varchar(20) not null,
    descricao text not null
);

-- Nulos para as partidas antigas. O ensaio local usa o número sem publicar uma linha em hordas.
alter table partidas add column horda_numero bigint;
alter table bilhetes add column horda_numero bigint;
create index ix_partidas_horda on partidas (horda_numero, resultado, pontos desc, registrada_em, id);
