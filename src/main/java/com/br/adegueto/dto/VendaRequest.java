package com.br.adegueto.dto;

import java.util.List;

public record VendaRequest(List<ItemVendaRequest> itens) {
}
