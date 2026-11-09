package com.br.adegueto.model;

import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Produto e quantidade dentro de uma venda, com o preço congelado no momento da venda. */
@Entity
@Table(name = "itens_venda")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    public ItemVenda(Produto produto, int quantidade) {
        if (produto == null) {
            throw new DadoInvalidoException("o produto do item é obrigatório");
        }
        if (quantidade <= 0) {
            throw new QuantidadeInvalidaException("a quantidade do item deve ser maior que zero");
        }
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco(); // preço congelado: preserva o histórico
    }

    /** RN004: quantidade x precoUnitario. */
    public BigDecimal calcularSubtotal() {
        return precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    void vincularVenda(Venda venda) {
        this.venda = venda;
    }
}
