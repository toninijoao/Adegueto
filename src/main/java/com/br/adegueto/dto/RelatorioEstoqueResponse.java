package com.br.adegueto.dto;

import java.util.List;

/** Posição geral do estoque, com a contagem de itens que atingiram o mínimo. */
public record RelatorioEstoqueResponse(
        int totalProdutos,
        int produtosComEstoqueBaixo,
        List<EstoqueResponse> itens) {
}
