package com.br.adegueto.exception;

/** Tentativa de remover mais itens do que o saldo disponível. */
public class EstoqueInsuficienteException extends AdeguetoException {

    public EstoqueInsuficienteException(int solicitado, int disponivel) {
        super("estoque insuficiente: solicitado " + solicitado + ", disponível " + disponivel);
    }

    /** Variante que identifica o produto (útil em vendas com vários itens). */
    public EstoqueInsuficienteException(String produto, int solicitado, int disponivel) {
        super("estoque insuficiente para \"" + produto + "\": solicitado " + solicitado + ", disponível " + disponivel);
    }
}
