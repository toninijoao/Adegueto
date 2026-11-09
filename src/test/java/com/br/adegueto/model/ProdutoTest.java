package com.br.adegueto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.adegueto.exception.DadoInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoTest {

    @Test
    void cadaEspecializacaoInformaSuaCategoria() {
        BigDecimal preco = new BigDecimal("50");
        assertThat(new Vinho("V", preco, "Tinto", "Chile", "Malbec", 2020).getCategoria())
                .isEqualTo(CategoriaProduto.VINHO);
        assertThat(new Whisky("W", preco, "Single Malt", "Escócia", 12).getCategoria())
                .isEqualTo(CategoriaProduto.WHISKY);
        assertThat(new Gin("G", preco, "London Dry", "Inglaterra").getCategoria())
                .isEqualTo(CategoriaProduto.GIN);
        assertThat(new Vodka("D", preco, "Polônia").getCategoria())
                .isEqualTo(CategoriaProduto.VODKA);
    }

    @Test
    void precoNaoPositivoELancaExcecao() {
        assertThatThrownBy(() -> new Vodka("D", BigDecimal.ZERO, "Polônia")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Vodka("D", new BigDecimal("-1"), "Polônia")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void nomeEmBrancoELancaExcecao() {
        assertThatThrownBy(() -> new Vodka("  ", BigDecimal.TEN, "Polônia")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void precoEArredondadoParaDuasCasas() {
        Vodka vodka = new Vodka("D", new BigDecimal("10.555"), "Polônia");
        assertThat(vodka.getPreco()).isEqualByComparingTo("10.56");
    }

    @Test
    void vincularEstoqueMantemOsDoisLadosConsistentes() {
        Vodka vodka = new Vodka("D", BigDecimal.TEN, "Polônia");
        Estoque estoque = new Estoque(10, 2);

        vodka.vincularEstoque(estoque);

        assertThat(vodka.getEstoque()).isSameAs(estoque);
        assertThat(estoque.getProduto()).isSameAs(vodka);
    }

    @Test
    void produtoNasceAtivo() {
        assertThat(new Vodka("D", BigDecimal.TEN, "Polônia").isAtivo()).isTrue();
    }
}
