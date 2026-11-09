package com.br.adegueto.dto;

import com.br.adegueto.model.CategoriaProduto;
import java.math.BigDecimal;

/**
 * Dados de cadastro/alteração de produto. Os campos específicos dependem da categoria:
 * Vinho (tipo, origem, uva, safra), Whisky (tipo, origem, idade), Gin (estilo, origem), Vodka (origem).
 */
public record ProdutoRequest(
        CategoriaProduto categoria,
        String nome,
        BigDecimal preco,
        Integer quantidadeInicial,
        Integer estoqueMin,
        String tipo,
        String origem,
        String uva,
        Integer safra,
        Integer idade,
        String estilo) {
}
