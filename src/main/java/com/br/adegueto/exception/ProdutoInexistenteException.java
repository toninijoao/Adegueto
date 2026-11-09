package com.br.adegueto.exception;

/** Produto não encontrado pelo identificador informado. */
public class ProdutoInexistenteException extends AdeguetoException {

    public ProdutoInexistenteException() {
        super("produto não encontrado");
    }
}
