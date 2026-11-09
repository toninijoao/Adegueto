package com.br.adegueto.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import org.junit.jupiter.api.Test;

class EstoqueTest {

    @Test
    void adicionarSomaAoSaldo() {
        Estoque estoque = new Estoque(10, 2);
        estoque.adicionar(5);
        assertThat(estoque.getQuantidade()).isEqualTo(15);
    }

    @Test
    void adicionarQuantidadeNaoPositivaLancaExcecao() {
        Estoque estoque = new Estoque(10, 2);
        assertThatThrownBy(() -> estoque.adicionar(0)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> estoque.adicionar(-3)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThat(estoque.getQuantidade()).isEqualTo(10);
    }

    @Test
    void removerSubtraiDoSaldo() {
        Estoque estoque = new Estoque(10, 2);
        estoque.remover(4);
        assertThat(estoque.getQuantidade()).isEqualTo(6);
    }

    @Test
    void removerTodoOSaldoEPermitido() {
        Estoque estoque = new Estoque(3, 0);
        estoque.remover(3);
        assertThat(estoque.getQuantidade()).isZero();
    }

    @Test
    void removerMaisQueODisponivelLancaExcecaoEMantemSaldo() {
        Estoque estoque = new Estoque(5, 1);
        assertThatThrownBy(() -> estoque.remover(6)).isInstanceOf(EstoqueInsuficienteException.class);
        assertThat(estoque.getQuantidade()).isEqualTo(5);
    }

    @Test
    void removerQuantidadeNaoPositivaLancaExcecao() {
        Estoque estoque = new Estoque(5, 1);
        assertThatThrownBy(() -> estoque.remover(0)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> estoque.remover(-1)).isInstanceOf(QuantidadeInvalidaException.class);
    }

    @Test
    void checaMinRetornaTrueQuandoAbaixoDoMinimo() {
        assertThat(new Estoque(1, 5).checaMin()).isTrue();
    }

    @Test
    void checaMinRetornaTrueQuandoIgualAoMinimo() {
        assertThat(new Estoque(5, 5).checaMin()).isTrue();
    }

    @Test
    void checaMinRetornaFalseQuandoAcimaDoMinimo() {
        assertThat(new Estoque(6, 5).checaMin()).isFalse();
    }

    @Test
    void checaMinReageAAtualizacaoDoSaldo() {
        Estoque estoque = new Estoque(6, 5);
        estoque.remover(1);
        assertThat(estoque.checaMin()).isTrue();
        estoque.adicionar(10);
        assertThat(estoque.checaMin()).isFalse();
    }

    @Test
    void valoresIniciaisNegativosSaoRejeitados() {
        assertThatThrownBy(() -> new Estoque(-1, 0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new Estoque(0, -1)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void estoqueMinimoNegativoNaoEPermitidoNaAtualizacao() {
        Estoque estoque = new Estoque(5, 1);
        assertThatThrownBy(() -> estoque.setEstoqueMin(-2)).isInstanceOf(DadoInvalidoException.class);
    }
}
