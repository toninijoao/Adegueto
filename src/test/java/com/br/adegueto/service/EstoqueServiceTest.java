package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.repository.EstoqueRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstoqueServiceTest {

    private EstoqueRepository repository;
    private EstoqueService service;

    @BeforeEach
    void preparar() {
        repository = mock(EstoqueRepository.class);
        service = new EstoqueService(repository);
        when(repository.saveAndFlush(any(Estoque.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Estoque estoqueDe(String nome, int quantidade, int minimo) {
        Vodka produto = new Vodka(nome, BigDecimal.TEN, "Polônia");
        Estoque estoque = new Estoque(quantidade, minimo);
        produto.vincularEstoque(estoque);
        return estoque;
    }

    @Test
    void entradaSomaAoSaldo() {
        Estoque estoque = estoqueDe("Vodka", 10, 2);
        when(repository.findByProdutoId(1L)).thenReturn(Optional.of(estoque));

        Estoque resultado = service.entrada(1L, 5);

        assertThat(resultado.getQuantidade()).isEqualTo(15);
        verify(repository).saveAndFlush(estoque);
    }

    @Test
    void saidaSubtraiDoSaldo() {
        Estoque estoque = estoqueDe("Vodka", 10, 2);
        when(repository.findByProdutoId(1L)).thenReturn(Optional.of(estoque));

        assertThat(service.saida(1L, 4).getQuantidade()).isEqualTo(6);
    }

    @Test
    void saidaMaiorQueOSaldoLancaExcecaoENaoGrava() {
        Estoque estoque = estoqueDe("Vodka", 3, 1);
        when(repository.findByProdutoId(1L)).thenReturn(Optional.of(estoque));

        assertThatThrownBy(() -> service.saida(1L, 4)).isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(estoque.getQuantidade()).isEqualTo(3);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void quantidadeNulaZeroOuNegativaEhRejeitada() {
        Estoque estoque = estoqueDe("Vodka", 10, 2);
        when(repository.findByProdutoId(1L)).thenReturn(Optional.of(estoque));

        assertThatThrownBy(() -> service.entrada(1L, null)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> service.entrada(1L, 0)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> service.saida(1L, -2)).isInstanceOf(QuantidadeInvalidaException.class);
        assertThat(estoque.getQuantidade()).isEqualTo(10);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void produtoInexistenteLancaExcecao() {
        when(repository.findByProdutoId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.entrada(9L, 1)).isInstanceOf(ProdutoInexistenteException.class);
        assertThatThrownBy(() -> service.saida(9L, 1)).isInstanceOf(ProdutoInexistenteException.class);
    }

    @Test
    void listarBaixoRetornaSomenteQuemAtingiuOuFicouAbaixoDoMinimo() {
        Estoque abaixo = estoqueDe("A", 1, 5);
        Estoque igual = estoqueDe("B", 5, 5);
        Estoque acima = estoqueDe("C", 6, 5);
        when(repository.findAllByOrderByProdutoNomeAsc()).thenReturn(List.of(abaixo, igual, acima));

        assertThat(service.listarBaixo()).containsExactly(abaixo, igual);
    }

    @Test
    void listarRetornaTodos() {
        Estoque a = estoqueDe("A", 1, 5);
        Estoque c = estoqueDe("C", 6, 5);
        when(repository.findAllByOrderByProdutoNomeAsc()).thenReturn(List.of(a, c));

        assertThat(service.listar()).containsExactly(a, c);
    }

    @Test
    void vendaOuSaidaQueLevaAoMinimoPassaAAparecerNoEstoqueBaixo() {
        Estoque estoque = estoqueDe("A", 6, 5);
        when(repository.findByProdutoId(1L)).thenReturn(Optional.of(estoque));
        when(repository.findAllByOrderByProdutoNomeAsc()).thenReturn(List.of(estoque));

        assertThat(service.listarBaixo()).isEmpty();
        service.saida(1L, 1);
        assertThat(service.listarBaixo()).containsExactly(estoque);
    }
}
