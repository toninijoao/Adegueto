package com.br.adegueto.model;

import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Controle da quantidade física de um produto.
 * O saldo só muda por adicionar()/remover(), nunca fica negativo.
 */
@Entity
@Table(name = "estoques")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "estoque_min", nullable = false)
    private int estoqueMin;

    /** Bloqueio otimista contra vendas simultâneas (decisão D5). */
    @Version
    private Long versao;

    public Estoque(int quantidade, int estoqueMin) {
        if (quantidade < 0) {
            throw new DadoInvalidoException("a quantidade inicial não pode ser negativa");
        }
        validarEstoqueMin(estoqueMin);
        this.quantidade = quantidade;
        this.estoqueMin = estoqueMin;
    }

    /** Entrada de produtos (RN003). */
    public void adicionar(int quantidade) {
        validarPositiva(quantidade);
        this.quantidade += quantidade;
    }

    /** Saída de produtos ou venda (RN003). Nunca deixa o saldo negativo. */
    public void remover(int quantidade) {
        validarPositiva(quantidade);
        if (quantidade > this.quantidade) {
            throw new EstoqueInsuficienteException(quantidade, this.quantidade);
        }
        this.quantidade -= quantidade;
    }

    /** RN002: true quando quantidade <= estoqueMin. */
    public boolean checaMin() {
        return quantidade <= estoqueMin;
    }

    public void setEstoqueMin(int estoqueMin) {
        validarEstoqueMin(estoqueMin);
        this.estoqueMin = estoqueMin;
    }

    void vincularProduto(Produto produto) {
        this.produto = produto;
    }

    private static void validarPositiva(int quantidade) {
        if (quantidade <= 0) {
            throw new QuantidadeInvalidaException("a quantidade deve ser maior que zero");
        }
    }

    private static void validarEstoqueMin(int estoqueMin) {
        if (estoqueMin < 0) {
            throw new DadoInvalidoException("o estoque mínimo não pode ser negativo");
        }
    }
}
