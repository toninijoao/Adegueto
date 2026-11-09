package com.br.adegueto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resumo de vendas de um período. Sem vendas, os valores são zero e há uma mensagem informativa (RN005). */
public record RelatorioVendasResponse(
        LocalDate inicio,
        LocalDate fim,
        int quantidadeVendas,
        BigDecimal faturamento,
        BigDecimal ticketMedio,
        String mensagem) {
}
