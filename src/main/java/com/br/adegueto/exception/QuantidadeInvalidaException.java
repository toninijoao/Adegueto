package com.br.adegueto.exception;

/** Quantidade zero, negativa ou incompatível com a operação. */
public class QuantidadeInvalidaException extends AdeguetoException {

    public QuantidadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
