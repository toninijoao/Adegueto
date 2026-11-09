package com.br.adegueto.exception;

/** Exclusão recusada: o produto já aparece em vendas (decisão D3). */
public class ProdutoComVendasException extends AdeguetoException {

    public ProdutoComVendasException() {
        super("o produto possui vendas registradas e não pode ser excluído");
    }
}
