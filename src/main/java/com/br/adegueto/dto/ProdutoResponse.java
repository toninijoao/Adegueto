package com.br.adegueto.dto;

import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Gin;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Vinho;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.model.Whisky;
import java.math.BigDecimal;

/** Produto para a API. Campos que não se aplicam à categoria vêm como null. */
public record ProdutoResponse(
        Long id,
        CategoriaProduto categoria,
        String nome,
        BigDecimal preco,
        Integer quantidade,
        Integer estoqueMin,
        Boolean estoqueBaixo,
        String tipo,
        String origem,
        String uva,
        Integer safra,
        Integer idade,
        String estilo) {

    public static ProdutoResponse de(Produto p) {
        Estoque e = p.getEstoque();
        Integer quantidade = e == null ? null : e.getQuantidade();
        Integer estoqueMin = e == null ? null : e.getEstoqueMin();
        Boolean baixo = e == null ? null : e.checaMin();

        String tipo = null;
        String origem = null;
        String uva = null;
        Integer safra = null;
        Integer idade = null;
        String estilo = null;

        if (p instanceof Vinho v) {
            tipo = v.getTipo();
            origem = v.getOrigem();
            uva = v.getUva();
            safra = v.getSafra();
        } else if (p instanceof Whisky w) {
            tipo = w.getTipo();
            origem = w.getOrigem();
            idade = w.getIdade();
        } else if (p instanceof Gin g) {
            estilo = g.getEstilo();
            origem = g.getOrigem();
        } else if (p instanceof Vodka d) {
            origem = d.getOrigem();
        }

        return new ProdutoResponse(p.getId(), p.getCategoria(), p.getNome(), p.getPreco(),
                quantidade, estoqueMin, baixo, tipo, origem, uva, safra, idade, estilo);
    }
}
