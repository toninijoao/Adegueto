package com.br.adegueto.exception;

/** Base das exceções de negócio do Adegueto. */
public abstract class AdeguetoException extends RuntimeException {

    protected AdeguetoException(String mensagem) {
        super(mensagem);
    }
}
