package br.edu.unifor.missaomarte.servidor.loja;

import jakarta.validation.constraints.NotBlank;

/** Fase 8: o pedido de compra ({@code POST /api/loja/compras}) ou de troca de skin ({@code PUT /api/jogadores/eu/skin}). */
public record PedidoDeSkin(@NotBlank(message = "informe a skin (o id do catálogo, ex.: guara)") String skin) {
}
