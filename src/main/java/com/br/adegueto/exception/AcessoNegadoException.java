package com.br.adegueto.exception;

/** Usuário autenticado sem permissão para a operação. */
public class AcessoNegadoException extends AdeguetoException {

    public AcessoNegadoException() {
        super("acesso negado: o perfil do usuário não permite esta operação");
    }
}
