// Página do Missão Marte: ranking ao vivo (GET /api/ranking, mesma origem) e tamanho dos downloads (HEAD).
// Os nomes vêm de jogadores: entram na página só como texto (textContent), nunca como HTML.
"use strict";

// O texto do Clássico também está no index.html (é o que aparece antes do JavaScript rodar).
const DESCRICOES = {
  CLASSICO: "O jogo original, em turnos numa grade. Cada movimento custa 1 ponto, e você perde se os pontos "
    + "acabarem ou se bater 3 vezes em asteroides e inimigos.",
  PILOTAGEM: "Na pilotagem, a gravidade puxa a nave para baixo e o motor gasta combustível. "
    + "Pouse devagar na plataforma, embarque até 2 passageiros e leve-os à base, que também reabastece. "
    + "O combustível que sobra no fim vale até 30 pontos de bônus. Com conta, você vê as naves de quem está "
    + "jogando ao mesmo tempo.",
};
const DIFICULDADES = { FACIL: "Fácil", MEDIO: "Médio", DIFICIL: "Difícil" };
const ATUALIZAR_A_CADA_MS = 30000;

let modoAtual = "CLASSICO";

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
  const modo = modoAtual;
  try {
    const resposta = await fetch(`api/ranking?modo=${encodeURIComponent(modo)}`, { headers: { Accept: "application/json" } });
    if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
    const posicoes = await resposta.json();
    if (modo === modoAtual) mostrar(posicoes);
  } catch (erro) {
    if (modo === modoAtual) {
      document.getElementById("estado").textContent = "O servidor não respondeu. A página tenta de novo em 30 segundos.";
    }
  }
}

function escolherModo(botao) {
  modoAtual = botao.dataset.modo;
  for (const aba of document.querySelectorAll(".abas [role=tab]")) {
    aba.setAttribute("aria-selected", String(aba === botao));
  }
  document.getElementById("descricao-modo").textContent = DESCRICOES[modoAtual];
  document.getElementById("linhas").replaceChildren();
  document.getElementById("estado").textContent = "Carregando o ranking…";
  carregarRanking();
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
  for (const aba of document.querySelectorAll(".abas [role=tab]")) {
    aba.addEventListener("click", () => escolherModo(aba));
  }
  carregarRanking();
  conferirDownloads();
  setInterval(() => { if (!document.hidden) carregarRanking(); }, ATUALIZAR_A_CADA_MS);
});
