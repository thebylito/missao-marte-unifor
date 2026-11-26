alter table hordas add column fechada_em timestamp(6) with time zone;
alter table hordas add column semana date;
create index ix_hordas_fechamento on hordas (fechada_em, fim);
create index ix_hordas_semana on hordas (semana);

create table top_da_horda (
    horda_numero bigint not null references hordas (numero),
    posicao integer not null,
    jogador_id bigint not null references jogadores (id),
    partida_id bigint not null references partidas (id),
    pontos_semanais integer not null,
    primary key (horda_numero, posicao),
    constraint uk_top_da_horda_jogador unique (horda_numero, jogador_id)
);

create table semanas_fechadas (
    semana date primary key,
    fechada_em timestamp(6) with time zone not null
);

create sequence seq_evento_manual as bigint start with 100;

create table eventos_ao_vivo (
    evento varchar(64) primary key,
    tipo varchar(20) not null,
    semente bigint not null,
    inicio timestamp(6) with time zone not null,
    fim timestamp(6) with time zone not null,
    vip_jogador_id bigint references jogadores (id)
);
create index ix_eventos_ao_vivo_fim on eventos_ao_vivo (fim);

create table pontos_de_evento (
    jogador_id bigint not null references jogadores (id),
    evento varchar(64) not null references eventos_ao_vivo (evento),
    tipo varchar(20) not null,
    pontos integer not null,
    semana date not null,
    primary key (jogador_id, evento, tipo)
);
create index ix_pontos_de_evento_semana on pontos_de_evento (semana);
