package com.br.adegueto.dto;

import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Estoque;

/** Posição de estoque de um produto. */
public record EstoqueResponse(
        Long produtoId,
        String produtoNome,
        CategoriaProduto categoria,
        int quantidade,
        int estoqueMin,
        boolean estoqueBaixo) {

    public static EstoqueResponse de(Estoque e) {
        return new EstoqueResponse(
                e.getProduto().getId(),
                e.getProduto().getNome(),
                e.getProduto().getCategoria(),
                e.getQuantidade(),
                e.getEstoqueMin(),
                e.checaMin());
    }
}
