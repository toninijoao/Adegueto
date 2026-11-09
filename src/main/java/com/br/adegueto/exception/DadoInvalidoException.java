package com.br.adegueto.exception;

/** Dado de entrada ou estado inválido (campo vazio, preço não positivo, venda sem itens...). */
public class DadoInvalidoException extends AdeguetoException {

    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
