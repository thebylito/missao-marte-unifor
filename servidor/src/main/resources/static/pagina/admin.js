"use strict";

const login = document.querySelector("#login");
const evento = document.querySelector("#evento");
const resultado = document.querySelector("#resultado");
const entrar = login.querySelector("button");
const disparar = evento.querySelector("[type=submit]");
let token;

function enviar(rota, corpo, autorizacao) {
  const headers = { "Content-Type": "application/json" };
  if (autorizacao) {
    headers.Authorization = "Bearer " + autorizacao;
  }
  return fetch(rota, { method: "POST", headers, body: JSON.stringify(corpo) });
}

function encerrar(mensagem) {
  token = undefined;
  login.hidden = false;
  evento.hidden = true;
  resultado.textContent = mensagem;
  login.elements.nome.focus();
}

login.addEventListener("submit", async (acao) => {
  acao.preventDefault();
  entrar.disabled = true;
  resultado.textContent = "Entrando…";
  try {
    const resposta = await enviar("/api/sessoes", {
      nome: login.elements.nome.value,
      senha: login.elements.senha.value
    });
    if (!resposta.ok) {
      resultado.textContent = resposta.status === 401 ? "Nome ou senha inválidos." : "Não foi possível entrar.";
      return;
    }
    const sessao = await resposta.json();
    token = sessao.token;
    login.reset();
    login.hidden = true;
    evento.hidden = false;
    evento.elements.tipo.focus();
    resultado.textContent = "Sessão iniciada.";
  } catch {
    resultado.textContent = "Não foi possível conectar ao servidor.";
  } finally {
    login.elements.senha.value = "";
    entrar.disabled = false;
  }
});

evento.addEventListener("submit", async (acao) => {
  acao.preventDefault();
  disparar.disabled = true;
  resultado.textContent = "Agendando…";
  const sessaoAtual = token;
  try {
    const resposta = await enviar("/api/admin/eventos", { tipo: evento.elements.tipo.value }, sessaoAtual);
    if (token !== sessaoAtual) {
      return;
    }
    if (resposta.status === 202) {
      resultado.textContent = "Evento agendado. Os pilotos receberão um aviso 15 segundos antes do início.";
    } else if (resposta.status === 401) {
      encerrar("Sessão expirada. Entre novamente.");
    } else if (resposta.status === 403) {
      resultado.textContent = "Esta conta não tem acesso ao painel de eventos.";
    } else {
      resultado.textContent = "Não foi possível agendar o evento.";
    }
  } catch {
    if (token === sessaoAtual) {
      resultado.textContent = "Não foi possível conectar ao servidor.";
    }
  } finally {
    disparar.disabled = false;
  }
});

document.querySelector("#sair").addEventListener("click", () => encerrar("Sessão encerrada."));
