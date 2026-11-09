package com.br.adegueto.exception;

/** Venda não encontrada pelo identificador informado. */
public class VendaInexistenteException extends AdeguetoException {

    public VendaInexistenteException() {
        super("venda não encontrada");
    }
}
