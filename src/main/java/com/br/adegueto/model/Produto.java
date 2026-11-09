package com.br.adegueto.model;

import com.br.adegueto.exception.DadoInvalidoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Produto comercializado. Mapeamento: JOINED. */
@Entity
@Table(name = "produtos")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Setter
    @Column(nullable = false)
    private boolean ativo = true;

    @OneToOne(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Estoque estoque;

    protected Produto(String nome, BigDecimal preco) {
        setNome(nome);
        setPreco(preco);
    }

    public abstract CategoriaProduto getCategoria();

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("o nome do produto é obrigatório");
        }
        this.nome = nome.trim();
    }

    public void setPreco(BigDecimal preco) {
        if (preco == null || preco.signum() <= 0) {
            throw new DadoInvalidoException("o preço deve ser positivo");
        }
        this.preco = preco.setScale(2, RoundingMode.HALF_UP);
    }

    /** Associação 1:1 com Estoque, mantendo os dois lados consistentes. */
    public void vincularEstoque(Estoque estoque) {
        this.estoque = estoque;
        if (estoque != null) {
            estoque.vincularProduto(this);
        }
    }
}
