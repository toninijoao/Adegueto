package com.br.adegueto.dto;

import com.br.adegueto.model.Venda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendaResponse(
        Long id,
        LocalDateTime data,
        BigDecimal valorTotal,
        String vendedor,
        List<ItemVendaResponse> itens) {

    public static VendaResponse de(Venda venda) {
        return new VendaResponse(
                venda.getId(),
                venda.getData(),
                venda.getValorTotal(),
                venda.getUsuario().getNome(),
                venda.getItens().stream().map(ItemVendaResponse::de).toList());
    }
}
