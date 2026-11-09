package com.br.adegueto.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vodkas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vodka extends Produto {

    private String origem;

    public Vodka(String nome, BigDecimal preco, String origem) {
        super(nome, preco);
        this.origem = origem;
    }

    @Override
    public CategoriaProduto getCategoria() {
        return CategoriaProduto.VODKA;
    }
}
