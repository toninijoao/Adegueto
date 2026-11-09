package com.br.adegueto.exception;

/** Tentativa de cadastrar um login que já existe. */
public class LoginDuplicadoException extends AdeguetoException {

    public LoginDuplicadoException(String login) {
        super("já existe um usuário com o login \"" + login + "\"");
    }
}
