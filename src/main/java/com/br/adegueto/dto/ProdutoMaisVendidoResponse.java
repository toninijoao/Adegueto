package com.br.adegueto.dto;

import com.br.adegueto.model.CategoriaProduto;
import java.math.BigDecimal;

public record ProdutoMaisVendidoResponse(
        Long produtoId,
        String produtoNome,
        CategoriaProduto categoria,
        long quantidadeVendida,
        BigDecimal receita) {
}
