package com.br.adegueto.dto;

import java.util.List;

/** Resultado de consulta: lista e, quando vazia, mensagem informativa (RN005). */
public record ResultadoBusca<T>(List<T> itens, String mensagem) {

    public static <T> ResultadoBusca<T> de(List<T> itens, String mensagemSeVazio) {
        return new ResultadoBusca<>(itens, itens.isEmpty() ? mensagemSeVazio : null);
    }
}
