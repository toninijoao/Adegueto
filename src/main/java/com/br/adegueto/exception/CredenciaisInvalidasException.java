package com.br.adegueto.exception;

/** Falha de autenticação (login inexistente ou senha incorreta). */
public class CredenciaisInvalidasException extends AdeguetoException {

    public CredenciaisInvalidasException() {
        super("login ou senha inválidos");
    }
}
