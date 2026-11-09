package com.br.adegueto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ItemVendaTest {

    private Vodka vodka(String preco) {
        return new Vodka("Vodka Premium", new BigDecimal(preco), "Rússia");
    }

    @Test
    void subtotalEQuantidadeVezesPrecoUnitario() {
        ItemVenda item = new ItemVenda(vodka("10.50"), 3);
        assertThat(item.calcularSubtotal()).isEqualByComparingTo("31.50");
    }

    @Test
    void precoUnitarioFicaCongeladoAposAlteracaoDoPrecoDoProduto() {
        Vodka produto = vodka("20.00");
        ItemVenda item = new ItemVenda(produto, 2);

        produto.setPreco(new BigDecimal("99.90"));

        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("20.00");
        assertThat(item.calcularSubtotal()).isEqualByComparingTo("40.00");
    }

    @Test
    void quantidadeNaoPositivaELancaExcecao() {
        assertThatThrownBy(() -> new ItemVenda(vodka("10"), 0)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> new ItemVenda(vodka("10"), -1)).isInstanceOf(QuantidadeInvalidaException.class);
    }

    @Test
    void produtoNuloELancaExcecao() {
        assertThatThrownBy(() -> new ItemVenda(null, 1)).isInstanceOf(DadoInvalidoException.class);
    }
}
