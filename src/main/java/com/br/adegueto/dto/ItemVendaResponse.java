package com.br.adegueto.dto;

import com.br.adegueto.model.ItemVenda;
import java.math.BigDecimal;

public record ItemVendaResponse(
        Long produtoId,
        String produtoNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {

    public static ItemVendaResponse de(ItemVenda item) {
        return new ItemVendaResponse(
                item.getProduto().getId(),
                item.getProduto().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.calcularSubtotal());
    }
}
