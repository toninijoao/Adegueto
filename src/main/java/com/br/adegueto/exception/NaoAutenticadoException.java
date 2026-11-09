package com.br.adegueto.exception;

/** Requisição sem sessão autenticada. */
public class NaoAutenticadoException extends AdeguetoException {

    public NaoAutenticadoException() {
        super("autenticação necessária");
    }
}
