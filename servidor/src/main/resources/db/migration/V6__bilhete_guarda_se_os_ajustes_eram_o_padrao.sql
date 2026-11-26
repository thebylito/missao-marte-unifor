-- DA-48: o bilhete guarda se os ajustes do servidor eram o padrão quando foi emitido. Na Pilotagem Livre, com
-- ajustes diferentes, o resultado do jogo antigo só vale se ele começou a partida com o padrão. Os bilhetes
-- anteriores a esta migração foram emitidos sem ajustes, então valem como padrão.
alter table bilhetes add column ajustes_padrao boolean not null default true;
