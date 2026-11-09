package com.br.adegueto.dto;

import java.time.LocalDateTime;

/** Formato padrão de erro da API. */
public record ErroResponse(LocalDateTime timestamp, int status, String erro, String mensagem) {
}
