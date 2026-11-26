package br.edu.unifor.missaomarte.servidor.loja;

/**
 * Fase 8: uma skin do catálogo da loja ({@code skins.json}). As skins formam linhas de evolução: {@code linha} diz
 * a que linha a skin pertence e {@code nivel}, a posição dela na linha (1, 2, 3...). A compra é livre: não exige o
 * nível anterior. Só cosmética: a colisão e o voo não mudam. A arte fica no jogo, que mostra só as skins cuja arte
 * ele tem.
 */
public record Skin(String id, String nome, String linha, int nivel, int preco) {

    /** A nave de sempre: de graça, de todo jogador e equipada por padrão. */
    public static final String PADRAO = "padrao";
}
