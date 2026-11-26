package br.edu.unifor.missaomarte.servidor.loja;

import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.erro.RecusaDaLoja;
import br.edu.unifor.missaomarte.servidor.jogador.Jogador;
import br.edu.unifor.missaomarte.servidor.jogador.RepositorioDeJogadores;
import br.edu.unifor.missaomarte.servidor.moedas.Origem;
import br.edu.unifor.missaomarte.servidor.moedas.ServicoDeMoedas;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 8: a loja e o Hangar. Comprar precisa de saldo e de uma skin que o jogador ainda não tem; a {@code padrao} é
 * de todo jogador. A compra trava a linha do jogador antes de conferir o saldo, então duas compras ao mesmo tempo
 * nunca deixam o saldo negativo. A compra é livre: não exige o nível anterior da linha.
 */
@Service
public class ServicoDaLoja {

    private final CatalogoDeSkins catalogo;
    private final RepositorioDeSkinsDoJogador skins;
    private final RepositorioDeJogadores jogadores;
    private final ServicoDeMoedas moedas;
    private final Clock relogio;

    public ServicoDaLoja(CatalogoDeSkins catalogo, RepositorioDeSkinsDoJogador skins, RepositorioDeJogadores jogadores,
                         ServicoDeMoedas moedas, Clock relogio) {
        this.catalogo = catalogo;
        this.skins = skins;
        this.jogadores = jogadores;
        this.moedas = moedas;
        this.relogio = relogio;
    }

    public List<Skin> catalogo() {
        return catalogo.todas();
    }

    @Transactional(readOnly = true)
    public Hangar hangar(long jogadorId) {
        Jogador jogador = jogadores.findById(jogadorId).orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        return new Hangar(moedas.saldo(jogadorId), skinsDe(jogadorId), jogador.getSkin());
    }

    @Transactional
    public CompraResposta comprar(long jogadorId, String skinId) {
        Skin skin = doCatalogo(skinId);
        Jogador jogador = jogadores.travar(jogadorId).orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        if (possui(jogadorId, skin.id())) {
            throw new RecusaDaLoja(RecusaDaLoja.Motivo.JA_POSSUI, "você já tem a skin \"" + skin.nome() + "\"");
        }
        int saldo = moedas.saldo(jogadorId);
        if (saldo < skin.preco()) {
            throw new RecusaDaLoja(RecusaDaLoja.Motivo.SALDO_INSUFICIENTE, "saldo insuficiente: a skin \""
                    + skin.nome() + "\" custa " + skin.preco() + " moedas e você tem " + saldo,
                    Map.of("preco", skin.preco(), "saldo", saldo));
        }
        moedas.debitar(jogadorId, Origem.COMPRA, "skin:" + skin.id(), skin.preco());
        skins.save(new SkinDoJogador(jogador, skin.id(), relogio.instant()));
        return new CompraResposta(saldo - skin.preco(), skinsDe(jogadorId));
    }

    @Transactional
    public SkinEquipada equipar(long jogadorId, String skinId) {
        Skin skin = doCatalogo(skinId);
        Jogador jogador = jogadores.findById(jogadorId).orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
        if (!possui(jogadorId, skin.id())) {
            throw new RecusaDaLoja(RecusaDaLoja.Motivo.NAO_POSSUI, "você ainda não tem a skin \"" + skin.nome()
                    + "\": compre na loja antes de equipar");
        }
        jogador.equipar(skin.id());
        return new SkinEquipada(skin.id());
    }

    private Skin doCatalogo(String skinId) {
        String id = skinId.trim();
        return catalogo.buscar(id).orElseThrow(() -> new NaoEncontrado("skin não encontrada: \"" + id + "\""));
    }

    private boolean possui(long jogadorId, String skinId) {
        return skinId.equals(Skin.PADRAO) || skins.existsByJogadorIdAndSkin(jogadorId, skinId);
    }

    /** A {@code padrao} primeiro e depois as compradas, na ordem da compra. */
    private List<String> skinsDe(long jogadorId) {
        List<String> ids = new ArrayList<>();
        ids.add(Skin.PADRAO);
        for (SkinDoJogador s : skins.findByJogadorIdOrderByCompradaEmAscIdAsc(jogadorId)) {
            if (!ids.contains(s.getSkin())) {
                ids.add(s.getSkin());
            }
        }
        return ids;
    }
}
