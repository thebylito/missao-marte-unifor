package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.Hangar;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.grafico.online.SkinDaLoja;
import br.edu.unifor.missaomarte.grafico.recursos.CatalogoDeNaves;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Combina a loja do servidor com a arte instalada e usa o perfil confirmado para decidir a ação de cada item. */
public final class ControladorDoHangar {
    public enum Estado { EQUIPADA, POSSUIDA, A_COMPRAR, INDISPONIVEL }

    private static final List<String> NIVEIS_ROMANOS = List.of("I", "II", "III", "IV", "V");

    private final Map<String, String> nomesDasLinhas = new LinkedHashMap<>();
    private final ServidorDoJogo servidor;
    private final Optional<Sessao> sessao;
    private final CatalogoDeNaves catalogo;
    private final Consumer<Hangar> aoAtualizar;
    private final Consumer<String> aoEquipar;
    private final Consumer<Falha> aoRecusarSessao;
    private Hangar hangar;
    private List<SkinDaLoja> itens = List.of();
    private String mensagem = "";
    private int consultasPendentes;
    private boolean operando;

    public ControladorDoHangar(ServidorDoJogo servidor, Optional<Sessao> sessao, CatalogoDeNaves catalogo,
                               Consumer<Hangar> aoAtualizar, Consumer<String> aoEquipar,
                               Consumer<Falha> aoRecusarSessao) {
        this.servidor = servidor;
        this.sessao = sessao;
        this.catalogo = catalogo;
        for (var nave : catalogo.naves()) {
            if (Integer.valueOf(1).equals(nave.nivel()) && nave.linha() != null) {
                nomesDasLinhas.put(nave.linha(), nave.nome());
            }
        }
        this.aoAtualizar = aoAtualizar;
        this.aoEquipar = aoEquipar;
        this.aoRecusarSessao = aoRecusarSessao;
    }

    public void carregar() {
        if (sessao.isEmpty()) {
            mensagem = "Entre com uma conta para usar o Hangar.";
            return;
        }
        consultasPendentes = 2;
        servidor.hangar(sessao.get(), Resposta.de(perfil -> {
            consultasPendentes--;
            hangar = perfil;
            aoAtualizar.accept(perfil);
        }, this::falhaNaConsulta));
        servidor.loja(Resposta.de(loja -> {
            consultasPendentes--;
            itens = organizar(loja);
        }, this::falhaNaConsulta));
    }

    private List<SkinDaLoja> organizar(List<SkinDaLoja> loja) {
        Map<String, List<SkinDaLoja>> linhas = new LinkedHashMap<>();
        for (SkinDaLoja skin : loja) {
            if (catalogo.skinDisponivel(skin.id()).equals(skin.id())) {
                linhas.computeIfAbsent(skin.linha(), linha -> new ArrayList<>()).add(skin);
            }
        }
        List<SkinDaLoja> agrupadas = new ArrayList<>();
        for (List<SkinDaLoja> linha : linhas.values()) {
            linha.sort(Comparator.comparingInt(SkinDaLoja::nivel));
            agrupadas.addAll(linha);
        }
        return List.copyOf(agrupadas);
    }

    public String rotuloDaLinha(SkinDaLoja skin) {
        String linha = skin.linha();
        String nome = nomesDasLinhas.get(linha);
        if (nome == null || nome.isBlank()) {
            nome = linha.isEmpty() ? linha : Character.toUpperCase(linha.charAt(0)) + linha.substring(1);
        }
        int nivel = skin.nivel();
        String numero = nivel >= 1 && nivel <= NIVEIS_ROMANOS.size()
                ? NIVEIS_ROMANOS.get(nivel - 1) : Integer.toString(nivel);
        return "Linha " + nome + " · Nível " + numero;
    }

    public Estado estado(String skin) {
        if (hangar == null) {
            return Estado.INDISPONIVEL;
        }
        if (skin.equals(hangar.equipada())) {
            return Estado.EQUIPADA;
        }
        return hangar.skins().contains(skin) ? Estado.POSSUIDA : Estado.A_COMPRAR;
    }

    public void acionar(String skin) {
        if (ocupado() || itens.stream().noneMatch(item -> item.id().equals(skin))) {
            return;
        }
        switch (estado(skin)) {
            case POSSUIDA:
                operando = true;
                mensagem = "";
                servidor.equipar(sessao.orElseThrow(), skin, Resposta.de(equipada -> {
                    operando = false;
                    hangar = new Hangar(hangar.saldo(), hangar.skins(), equipada);
                    aoEquipar.accept(equipada);
                }, this::falhaNaOperacao));
                break;
            case A_COMPRAR:
                operando = true;
                mensagem = "";
                servidor.comprar(sessao.orElseThrow(), skin, Resposta.de(compra -> {
                    operando = false;
                    hangar = new Hangar(compra.saldo(), compra.skins(), hangar.equipada());
                    aoAtualizar.accept(compra);
                }, this::falhaNaOperacao));
                break;
            default:
                break;
        }
    }

    private void falhaNaOperacao(Falha falha) {
        operando = false;
        apresentarFalha(falha);
    }

    private void falhaNaConsulta(Falha falha) {
        consultasPendentes--;
        apresentarFalha(falha);
    }

    private void apresentarFalha(Falha falha) {
        if (falha.motivo() == Falha.Motivo.NAO_AUTORIZADO) {
            hangar = null;
            aoRecusarSessao.accept(falha);
        }
        if (falha.motivo() == Falha.Motivo.SEM_CONEXAO) {
            mensagem = "Sem conexão";
        } else if ("SALDO_INSUFICIENTE".equals(falha.codigo())) {
            mensagem = "Moedas insuficientes";
        } else if ("JA_POSSUI".equals(falha.codigo())) {
            mensagem = "Você já tem esta nave";
        } else {
            mensagem = falha.mensagem();
        }
    }

    public boolean ocupado() {
        return consultasPendentes > 0 || operando;
    }

    public String mensagem() {
        return mensagem;
    }

    public List<SkinDaLoja> itens() {
        return itens;
    }

    public Hangar hangar() {
        return hangar;
    }
}
