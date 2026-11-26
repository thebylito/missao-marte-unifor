// Página do Missão Marte: ranking ao vivo (GET /api/ranking, mesma origem) e tamanho dos downloads (HEAD).
// Os nomes vêm de jogadores: entram na página só como texto (textContent), nunca como HTML.
"use strict";

// O texto da Pilotagem também está no index.html (é o que aparece antes do JavaScript rodar).
const DESCRICOES = {
  HORDA: "Um novo desafio a cada 2 horas, igual para todos, com Top 10 próprio. "
    + "Eventos surpreendem quem está voando.",
  SEMANA: "Cada horda fechada dá 10, 8, 6, 5, 4, 3, 2, 1, 1 e 1 ponto do primeiro ao décimo lugar. "
    + "Sobreviver a um perigo soma 3 pontos. Pegar o VIP primeiro soma 5.",
  PILOTAGEM: "Na pilotagem, a gravidade puxa a nave para baixo e o motor gasta combustível. "
    + "Pouse devagar na plataforma, embarque até 2 passageiros e leve-os à base, que também reabastece. "
    + "O combustível que sobra no fim vale até 30 pontos de bônus. Com conta, você vê as naves de quem está "
    + "jogando ao mesmo tempo.",
  CLASSICO: "O jogo original, em turnos numa grade. Cada movimento custa 1 ponto, e você perde se os pontos "
    + "acabarem ou se bater 3 vezes em asteroides e inimigos.",
};
const DIFICULDADES = { FACIL: "Fácil", MEDIO: "Médio", DIFICIL: "Difícil" };
const ATUALIZAR_A_CADA_MS = 30000;

let modoAtual = "PILOTAGEM";
let pedidoRanking = 0;
let placarSemanal;
let pedidoSemana = 0;

function tempo(segundos) {
  const m = Math.floor(segundos / 60);
  const s = String(segundos % 60).padStart(2, "0");
  return `${m}:${s}`;
}

/** AAAA-MM-DD HH:MM no fuso de quem está vendo: com a hora, dá para saber quem chegou primeiro num empate. */
function quando(iso) {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return "";
  const dois = (n) => String(n).padStart(2, "0");
  return `${data.getFullYear()}-${dois(data.getMonth() + 1)}-${dois(data.getDate())} `
    + `${dois(data.getHours())}:${dois(data.getMinutes())}`;
}

/** O `proximaAs` do servidor já vem como "HH:MM" (Fortaleza); um instante ISO, se vier, vira HH:MM local. */
function horaDaProxima(valor) {
  if (/^\d{2}:\d{2}$/.test(valor ?? "")) return valor;
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? "" : data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function celula(texto, classe) {
  const td = document.createElement("td");
  td.textContent = texto;
  if (classe) td.className = classe;
  return td;
}

function mostrar(posicoes) {
  const corpo = document.getElementById("linhas");
  const estado = document.getElementById("estado");
  corpo.replaceChildren(...posicoes.map((p) => {
    const tr = document.createElement("tr");
    if (p.posicao <= 3) tr.className = `podio-${p.posicao}`;
    const piloto = celula(p.nome, "piloto");
    piloto.title = p.nome;
    tr.append(celula(`${p.posicao}º`), piloto, celula(String(p.pontos), "num"));
    if (modoAtual === "SEMANA") {
      tr.append(celula(String(p.hordas), "num"), celula(String(p.eventos), "num"));
    } else {
      tr.append(
        celula(DIFICULDADES[p.dificuldade] ?? p.dificuldade),
        celula(String(p.passageiros), "num"),
        celula(tempo(p.tempoSegundos), "num"),
        celula(quando(p.registradaEm)),
      );
    }
    return tr;
  }));
  const hora = new Date().toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  estado.textContent = posicoes.length === 0
    ? (modoAtual === "SEMANA" ? "Ainda não há pilotos no placar desta semana." : "Ainda não há vitórias neste modo.")
    : `A lista se atualiza a cada 30 segundos. Última leitura às ${hora}.`;
}

function mostrarSemana() {
  const segunda = new Date(`${placarSemanal.semana}T00:00:00Z`);
  const domingo = new Date(segunda);
  domingo.setUTCDate(segunda.getUTCDate() + 6);
  const diaMes = (data) => data.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", timeZone: "UTC" });
  document.getElementById("periodo-semana").textContent =
    `Semana de ${diaMes(segunda)} a ${diaMes(domingo)} · os 3 primeiros ganham 50, 30 e 20 moedas`;
  mostrar(placarSemanal.posicoes);
}

async function carregarSemana() {
  const pedido = ++pedidoSemana;
  let placar;
  try {
    const resposta = await fetch("api/ranking/semanal", { headers: { Accept: "application/json" } });
    if (resposta.status === 200) placar = await resposta.json();
  } catch {
    // A próxima atualização volta a verificar a disponibilidade.
  }
  if (pedido !== pedidoSemana) return;
  placarSemanal = placar;
  const aba = document.getElementById("aba-semana");
  if (!placar && modoAtual === "SEMANA") {
    const alternativa = document.querySelector(".abas [role=tab]:not([hidden]):not(#aba-semana)");
    const tinhaFoco = document.activeElement === aba;
    escolherModo(alternativa);
    if (tinhaFoco) alternativa.focus();
  }
  aba.hidden = !placar;
  if (modoAtual === "SEMANA") mostrarSemana();
}

async function carregarRanking() {
  const pedido = ++pedidoRanking;
  const modo = modoAtual;
  try {
    const resposta = await fetch(`api/ranking?modo=${encodeURIComponent(modo)}`, { headers: { Accept: "application/json" } });
    if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
    const posicoes = await resposta.json();
    if (pedido === pedidoRanking) mostrar(posicoes);
  } catch (erro) {
    if (pedido === pedidoRanking) {
      document.getElementById("estado").textContent = "O servidor não respondeu. A página tenta de novo em 30 segundos.";
    }
  }
}

function escolherModo(botao) {
  modoAtual = botao.dataset.modo;
  for (const aba of document.querySelectorAll(".abas [role=tab]")) {
    aba.setAttribute("aria-selected", String(aba === botao));
    aba.tabIndex = aba === botao ? 0 : -1;
  }
  document.getElementById("painel-ranking").setAttribute("aria-labelledby", botao.id);
  document.getElementById("descricao-modo").textContent = DESCRICOES[modoAtual];
  document.getElementById("horda-atual").hidden = modoAtual !== "HORDA";
  const semanal = modoAtual === "SEMANA";
  document.getElementById("colunas-ranking").hidden = semanal;
  document.getElementById("colunas-semana").hidden = !semanal;
  document.getElementById("periodo-semana").hidden = !semanal;
  if (semanal) {
    ++pedidoRanking;
    mostrarSemana();
    return;
  }
  document.getElementById("linhas").replaceChildren();
  document.getElementById("estado").textContent = "Carregando o ranking…";
  carregarRanking();
}

async function carregarHorda() {
  try {
    const resposta = await fetch("api/hordas/atual", { headers: { Accept: "application/json" } });
    if (resposta.status !== 200) return false;

    const horda = await resposta.json();
    const fim = Date.now() + horda.segundosParaAcabar * 1000;
    const proxima = horaDaProxima(horda.proximaAs);
    const dificuldade = DIFICULDADES[horda.dificuldade] ?? horda.dificuldade;
    const resumo = `${horda.caverna.nome} · ${dificuldade}`;
    const linha = document.getElementById("horda-atual");

    function atualizarContagem() {
      const restante = Math.max(0, fim - Date.now());
      const minutos = Math.floor(restante / 60000);
      linha.textContent = `${resumo} · termina em ${Math.floor(minutos / 60)}h ${minutos % 60}min · próxima às ${proxima}`;
      if (restante === 0) clearInterval(relogio);
    }

    const relogio = setInterval(atualizarContagem, 1000);
    atualizarContagem();
    return true;
  } catch {
    return false;
  }
}

function abaPelaTecla(tecla, aba) {
  const abas = [...document.querySelectorAll(".abas [role=tab]:not([hidden])")];
  const indice = abas.indexOf(aba);
  switch (tecla) {
    case "ArrowLeft": return abas[(indice - 1 + abas.length) % abas.length];
    case "ArrowRight": return abas[(indice + 1) % abas.length];
    case "Home": return abas[0];
    case "End": return abas[abas.length - 1];
  }
}

function megabytes(bytes) {
  return `${(bytes / (1024 * 1024)).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} MB`;
}

async function conferirDownloads() {
  for (const link of document.querySelectorAll("a.download")) {
    const arquivo = link.dataset.arquivo;
    const legenda = document.querySelector(`[data-tamanho-de="${arquivo}"]`);
    try {
      const resposta = await fetch(`downloads/${arquivo}`, { method: "HEAD" });
      if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
      const bytes = Number(resposta.headers.get("Content-Length"));
      legenda.textContent = bytes > 0 ? megabytes(bytes) : "";
    } catch (erro) {
      link.classList.add("indisponivel");
      link.setAttribute("aria-disabled", "true");
      legenda.textContent = "Este arquivo ainda não foi publicado neste servidor.";
    }
  }
}

document.addEventListener("DOMContentLoaded", () => {
  let modoEscolhido = false;
  function selecionar(aba) {
    modoEscolhido = true;
    escolherModo(aba);
  }

  for (const aba of document.querySelectorAll(".abas [role=tab]")) {
    aba.addEventListener("click", () => selecionar(aba));
    aba.addEventListener("keydown", (evento) => {
      const proximaAba = abaPelaTecla(evento.key, aba);
      if (!proximaAba) return;
      evento.preventDefault();
      selecionar(proximaAba);
      proximaAba.focus();
    });
  }
  carregarHorda().then((disponivel) => {
    if (!disponivel) return;
    const aba = document.getElementById("aba-horda");
    aba.hidden = false;
    if (!modoEscolhido) escolherModo(aba);
  });
  carregarRanking();
  carregarSemana();
  conferirDownloads();
  setInterval(() => {
    if (document.hidden) return;
    if (modoAtual !== "SEMANA") carregarRanking();
    carregarSemana();
  }, ATUALIZAR_A_CADA_MS);
});
