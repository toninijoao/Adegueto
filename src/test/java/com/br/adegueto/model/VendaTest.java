package com.br.adegueto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.adegueto.exception.DadoInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VendaTest {

    private final Usuario vendedor = new Funcionario("Maria", "maria", "hash");

    @Test
    void totalESomaDosSubtotaisDeTodosOsItens() {
        Venda venda = new Venda(vendedor);
        venda.adicionarItem(new ItemVenda(new Vodka("Vodka", new BigDecimal("10.00"), "Polônia"), 3)); // 30.00
        venda.adicionarItem(new ItemVenda(new Gin("Gin", new BigDecimal("55.50"), "London Dry", "Inglaterra"), 2)); // 111.00

        venda.calcularTotal();

        assertThat(venda.getValorTotal()).isEqualByComparingTo("141.00");
    }

    @Test
    void adicionarItemAssociaOItemAVenda() {
        Venda venda = new Venda(vendedor);
        ItemVenda item = new ItemVenda(new Vodka("Vodka", new BigDecimal("10.00"), "Polônia"), 1);

        venda.adicionarItem(item);

        assertThat(venda.getItens()).containsExactly(item);
        assertThat(item.getVenda()).isSameAs(venda);
    }

    @Test
    void finalizarCalculaTotalERegistraData() {
        Venda venda = new Venda(vendedor);
        venda.adicionarItem(new ItemVenda(new Vodka("Vodka", new BigDecimal("12.34"), "Suécia"), 2));

        venda.finalizar();

        assertThat(venda.getValorTotal()).isEqualByComparingTo("24.68");
        assertThat(venda.getData()).isNotNull();
    }

    @Test
    void finalizarSemItensLancaExcecao() {
        Venda venda = new Venda(vendedor);
        assertThatThrownBy(venda::finalizar).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void vendaSemUsuarioELancaExcecao() {
        assertThatThrownBy(() -> new Venda(null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void listaDeItensNaoPodeSerModificadaDeFora() {
        Venda venda = new Venda(vendedor);
        assertThatThrownBy(() -> venda.getItens().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
