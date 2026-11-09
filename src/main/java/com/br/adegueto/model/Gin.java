package com.br.adegueto.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gins")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gin extends Produto {

    private String estilo;
    private String origem;

    public Gin(String nome, BigDecimal preco, String estilo, String origem) {
        super(nome, preco);
        this.estilo = estilo;
        this.origem = origem;
    }

    @Override
    public CategoriaProduto getCategoria() {
        return CategoriaProduto.GIN;
    }
}
