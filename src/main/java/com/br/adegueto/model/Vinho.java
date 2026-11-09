package com.br.adegueto.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vinhos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vinho extends Produto {

    private String tipo;
    private String origem;
    private String uva;
    private int safra;

    public Vinho(String nome, BigDecimal preco, String tipo, String origem, String uva, int safra) {
        super(nome, preco);
        this.tipo = tipo;
        this.origem = origem;
        this.uva = uva;
        this.safra = safra;
    }

    @Override
    public CategoriaProduto getCategoria() {
        return CategoriaProduto.VINHO;
    }
}
