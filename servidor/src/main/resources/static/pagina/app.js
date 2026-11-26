// Página do Missão Marte: ranking ao vivo (GET /api/ranking, mesma origem) e tamanho dos downloads (HEAD).
// Os nomes vêm de jogadores: entram na página só como texto (textContent), nunca como HTML.
"use strict";

// O texto da Pilotagem também está no index.html (é o que aparece antes do JavaScript rodar).
const DESCRICOES = {
  HORDA: "Um novo desafio a cada 2 horas, igual para todos, com Top 10 próprio. "
    + "Eventos surpreendem quem está voando.",
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

function tempo(segundos) {
  const m = Math.floor(segundos / 60);
  const s = String(segundos % 60).padStart(2, "0");
  return `${m}:${s}`;
}

function quando(iso) {
  const data = new Date(iso);
  return Number.isNaN(data.getTime()) ? "" : data.toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
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
    tr.append(
      celula(`${p.posicao}º`),
      piloto,
      celula(String(p.pontos), "num"),
      celula(DIFICULDADES[p.dificuldade] ?? p.dificuldade),
      celula(String(p.passageiros), "num"),
      celula(tempo(p.tempoSegundos), "num"),
      celula(quando(p.registradaEm)),
    );
    return tr;
  }));
  const hora = new Date().toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  estado.textContent = posicoes.length === 0
    ? "Ainda não há vitórias neste modo."
    : `A lista se atualiza a cada 30 segundos. Última leitura às ${hora}.`;
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
    const proxima = new Date(horda.proximaAs).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
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
  conferirDownloads();
  setInterval(() => { if (!document.hidden) carregarRanking(); }, ATUALIZAR_A_CADA_MS);
});
