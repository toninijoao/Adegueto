package com.br.adegueto.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "whiskies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Whisky extends Produto {

    private String tipo;
    private String origem;
    private int idade;

    public Whisky(String nome, BigDecimal preco, String tipo, String origem, int idade) {
        super(nome, preco);
        this.tipo = tipo;
        this.origem = origem;
        this.idade = idade;
    }

    @Override
    public CategoriaProduto getCategoria() {
        return CategoriaProduto.WHISKY;
    }
}
