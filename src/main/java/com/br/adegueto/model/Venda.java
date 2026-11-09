package com.br.adegueto.model;

import com.br.adegueto.exception.DadoInvalidoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Transação comercial registrada por um usuário, com um ou mais itens. */
@Entity
@Table(name = "vendas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime data;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda(Usuario usuario) {
        if (usuario == null) {
            throw new DadoInvalidoException("a venda deve estar associada a um usuário");
        }
        this.usuario = usuario;
    }

    public void adicionarItem(ItemVenda item) {
        if (item == null) {
            throw new DadoInvalidoException("o item da venda é obrigatório");
        }
        item.vincularVenda(this);
        itens.add(item);
    }

    /** RN004: soma dos subtotais de todos os itens. */
    public void calcularTotal() {
        this.valorTotal = itens.stream()
                .map(ItemVenda::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Efetiva a venda: exige ao menos um item, recalcula o total e registra a data. */
    public void finalizar() {
        if (itens.isEmpty()) {
            throw new DadoInvalidoException("a venda deve possuir ao menos um item");
        }
        calcularTotal();
        this.data = LocalDateTime.now();
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
