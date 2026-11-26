# Contrato da Horda e dos eventos ao vivo (Fase 8)

O jogo (etapas 5 e 6 no cliente) e o servidor (etapas 4 e 6 no servidor) seguem este documento. As regras estão em
[`docs/fase-08-analise.md`](../fase-08-analise.md), seções 2.1 a 2.3.

## HTTP

### `GET /api/hordas/atual` (público)
```json
{
  "numero": 123,
  "inicio": "2026-11-26T21:00:00Z",
  "fim": "2026-11-26T23:00:00Z",
  "segundosParaAcabar": 4210,
  "segundosParaProxima": 4210,
  "proximaAs": "20:00",
  "caverna": { "id": "marciana", "nome": "Caverna Marciana" },
  "dificuldade": "DIFICIL",
  "modificadores": [ { "tipo": "VENTO", "valor": 12.5 } ],
  "descricao": { "...": "a DescricaoDaHorda completa, no formato de docs/api/exemplos/horda.json" }
}
```
- Os segundos são **relativos**, para o jogo não depender do relógio do aparelho.
- `proximaAs` é a hora local de Fortaleza, em texto.
- A dificuldade no topo usa `FACIL`, `MEDIO` ou `DIFICIL`. Dentro de `descricao` vale a grafia do exemplo
  versionado, como `"Fácil"`.

### `POST /api/partidas/inicio` (com token)
- Na Horda, o corpo é `{"modo":"HORDA","dificuldade":"DIFICIL","horda":123}`. Ele devolve 201
  `{"bilhete":"<uuid>","validoAte":"<instant>"}`, como nos outros modos.
- Na Horda, o bilhete é **obrigatório**: sem ele, não há partida da Horda.

### `POST /api/partidas` (com token)
- Na Horda, o corpo é `{"modo":"HORDA","horda":123,"bilhete":"<uuid>", ...os campos de sempre}`.
- A resposta mantém todos os campos e acrescenta `"colocacao"`: a posição do jogador no Top 10 daquela horda depois
  do registro, ou `null` se ficou fora do Top 10. Os campos `moedasGanhas` e `saldo` continuam vindo.
- A horda é aceita até 30 min depois do fim, enquanto seu Top 10 ainda está aberto. Passado o prazo ou concluído
  o fechamento, a resposta é 409 com `"motivo":"HORDA_ENCERRADA"`.
- Reenviar os mesmos dados com o mesmo bilhete devolve 201 com o registro existente, mesmo depois do fechamento.
  O servidor credita as moedas uma vez. Para bilhetes usados antes da V5, o primeiro reenvio consulta o crédito do
  extrato e guarda o saldo e a colocação desse momento.
- Sem bilhete na Horda ou ao mudar os dados de uma partida já registrada com ele, a resposta é 400.

### `GET /api/ranking?modo=HORDA&horda=123` (público)
- Sem `horda`, vale a horda atual.
- O formato é o mesmo do ranking de hoje.

### `GET /api/ranking/semanal` (público)
```json
{ "semana": "2026-11-23", "posicoes": [ { "posicao": 1, "nome": "Ana", "pontos": 42, "hordas": 5, "eventos": 3 } ] }
```

### `POST /api/admin/eventos` (com token de administrador)

Recebe `{"tipo":"CHUVA_DE_METEOROS"}` e responde 202 sem corpo. O evento é anunciado pelo canal. Sem token
válido, responde 401. Se o jogador do token não estiver na lista de administradores, responde 403.

## WebSocket `/ws/sombras?sala=livre|horda-123`
- Sem `sala`, vale `livre`: os clientes antigos continuam funcionando.
- As sombras só circulam dentro da mesma sala. A mensagem `sombra` leva `skin`. Para uma sala desconhecida
  ou fora da janela, o servidor fecha a conexão com código 1008.
- Os eventos chegam a todas as salas. O VIP leva uma semente própria no início, e `vip.x` e `vip.superficie`
  são números que admitem casas decimais. O ponto de pouso é calculado para a caverna de cada sala.

| Direção | Mensagem |
|---|---|
| servidor → jogo | `{"tipo":"evento-aviso","evento":"e-123-1","nome":"CHUVA_DE_METEOROS","em":15}` (15 s antes) |
| servidor → jogo | `{"tipo":"evento-inicio","evento":"e-123-1","nome":"CHUVA_DE_METEOROS","semente":987654321,"duracao":20}` |
| servidor → jogo | `{"tipo":"evento-inicio","evento":"e-123-2","nome":"PASSAGEIRO_VIP","semente":734729831,"duracao":45,"vip":{"x":2000.0,"superficie":116.0}}` |
| servidor → jogo | `{"tipo":"vip-pego","evento":"e-123-2","nome":"Ana"}` |
| jogo → servidor | `{"tipo":"evento-resultado","evento":"e-123-1","sobreviveu":true}` (no fim de cada perigo) |
| jogo → servidor | `{"tipo":"vip","evento":"e-123-2"}` (o jogo marcou o VIP; o servidor decide quem foi o primeiro) |

- `nome` é um `TipoDeEvento` do `nucleo`: `CHUVA_DE_METEOROS`, `TEMPESTADE`, `PANE_DE_GRAVIDADE` ou
  `PASSAGEIRO_VIP`.
- Quem só está no menu não recebe eventos: o canal só fica aberto durante o voo.

- O campo `em` é a contagem de segundos até o início, arredondada para cima. Quem conecta durante os 15 segundos
  de aviso recebe o tempo restante.
- Para pontuar ao sobreviver, o piloto precisa ter enviado uma posição válida nos três segundos anteriores ao início
  do perigo. O servidor aceita um resultado por piloto e perigo. O VIP pode ser pego a partir de três segundos
  depois do início. Pedidos de VIP e resultados compartilham o limite de uma mensagem válida por segundo por piloto.
