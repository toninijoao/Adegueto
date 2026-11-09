package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.adegueto.dto.ProdutoRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.ProdutoComVendasException;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Gin;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Vinho;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.model.Whisky;
import com.br.adegueto.repository.ItemVendaRepository;
import com.br.adegueto.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProdutoServiceTest {

    private ProdutoRepository produtoRepository;
    private ItemVendaRepository itemVendaRepository;
    private ProdutoService service;

    @BeforeEach
    void preparar() {
        produtoRepository = mock(ProdutoRepository.class);
        itemVendaRepository = mock(ItemVendaRepository.class);
        service = new ProdutoService(produtoRepository, itemVendaRepository);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static ProdutoRequest vinho() {
        return new ProdutoRequest(CategoriaProduto.VINHO, "Malbec Reserva", new BigDecimal("89.90"),
                12, 3, "Tinto", "Argentina", "Malbec", 2020, null, null);
    }

    private Produto produtoExistente(Produto produto) {
        produto.vincularEstoque(new Estoque(10, 2));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        return produto;
    }

    // ---------- cadastro (RF03) ----------

    @Test
    void cadastraVinhoComEstoqueAssociado() {
        Produto criado = service.cadastrar(vinho());

        assertThat(criado).isInstanceOf(Vinho.class);
        assertThat(criado.getCategoria()).isEqualTo(CategoriaProduto.VINHO);
        assertThat(criado.getEstoque().getQuantidade()).isEqualTo(12);
        assertThat(criado.getEstoque().getEstoqueMin()).isEqualTo(3);
        assertThat(criado.getEstoque().getProduto()).isSameAs(criado);
    }

    @Test
    void cadastraCadaCategoriaComSeusAtributos() {
        Produto whisky = service.cadastrar(new ProdutoRequest(CategoriaProduto.WHISKY, "Single Malt",
                new BigDecimal("300"), 5, 1, "Single Malt", "Escócia", null, null, 12, null));
        Produto gin = service.cadastrar(new ProdutoRequest(CategoriaProduto.GIN, "London Dry",
                new BigDecimal("120"), 5, 1, null, "Inglaterra", null, null, null, "London Dry"));
        Produto vodka = service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "Vodka Pura",
                new BigDecimal("60"), 5, 1, null, "Polônia", null, null, null, null));

        assertThat(whisky).isInstanceOf(Whisky.class);
        assertThat(gin).isInstanceOf(Gin.class);
        assertThat(vodka).isInstanceOf(Vodka.class);
    }

    @Test
    void estoqueMinimoAusenteAssumeZero() {
        var dados = new ProdutoRequest(CategoriaProduto.VODKA, "Vodka", BigDecimal.TEN, 4, null,
                null, "Polônia", null, null, null, null);
        assertThat(service.cadastrar(dados).getEstoque().getEstoqueMin()).isZero();
    }

    @Test
    void camposComunsObrigatorios() {
        assertThatThrownBy(() -> service.cadastrar(null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(null, "N", BigDecimal.TEN, 1, 0,
                null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "N", BigDecimal.TEN,
                null, 0, null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, " ", BigDecimal.TEN,
                1, 0, null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "N", BigDecimal.ZERO,
                1, 0, null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void quantidadeInicialENegativaSaoRejeitadas() {
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "N", BigDecimal.TEN,
                -1, 0, null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "N", BigDecimal.TEN,
                1, -1, null, "x", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void atributosEspecificosAusentesSaoRejeitados() {
        // vinho sem uva
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VINHO, "V", BigDecimal.TEN,
                1, 0, "Tinto", "Chile", null, 2020, null, null))).isInstanceOf(DadoInvalidoException.class);
        // vinho com safra inválida
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VINHO, "V", BigDecimal.TEN,
                1, 0, "Tinto", "Chile", "Merlot", 0, null, null))).isInstanceOf(DadoInvalidoException.class);
        // whisky sem idade
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.WHISKY, "W", BigDecimal.TEN,
                1, 0, "Blend", "Escócia", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        // gin sem estilo
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.GIN, "G", BigDecimal.TEN,
                1, 0, null, "Inglaterra", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        // vodka sem origem
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest(CategoriaProduto.VODKA, "D", BigDecimal.TEN,
                1, 0, null, " ", null, null, null, null))).isInstanceOf(DadoInvalidoException.class);
        verify(produtoRepository, never()).save(any());
    }

    // ---------- consulta (RF04 / RN005) ----------

    @Test
    void buscaFiltraPorCategoria() {
        Produto v = new Vinho("Tinto", BigDecimal.TEN, "Tinto", "Chile", "Merlot", 2019);
        Produto d = new Vodka("Tinta Vodka", BigDecimal.TEN, "Polônia");
        when(produtoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("tint")).thenReturn(List.of(v, d));

        assertThat(service.buscar("tint", CategoriaProduto.VODKA)).containsExactly(d);
        assertThat(service.buscar("tint", null)).containsExactly(v, d);
    }

    @Test
    void buscaSemNomeUsaTermoVazioETrimaEspacos() {
        when(produtoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("")).thenReturn(List.of());
        assertThat(service.buscar(null, null)).isEmpty();
        assertThat(service.buscar("   ", null)).isEmpty();
        verify(produtoRepository, org.mockito.Mockito.times(2)).findByNomeContainingIgnoreCaseOrderByNomeAsc("");
    }

    @Test
    void buscaSemResultadosRetornaListaVazia() {
        when(produtoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("xyz")).thenReturn(List.of());
        assertThat(service.buscar("xyz", null)).isEmpty();
    }

    @Test
    void obterInexistenteLancaExcecao() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obter(99L))
                .isInstanceOf(ProdutoInexistenteException.class)
                .hasMessage("produto não encontrado");
    }

    // ---------- alteração (RF05) ----------

    @Test
    void atualizaDadosGeraisEEspecificos() {
        Vinho v = (Vinho) produtoExistente(new Vinho("Velho", new BigDecimal("50"), "Tinto", "Chile", "Merlot", 2018));

        var dados = new ProdutoRequest(CategoriaProduto.VINHO, "Novo Nome", new BigDecimal("75.50"), null, 6,
                "Branco", "França", "Chardonnay", 2021, null, null);
        service.atualizar(1L, dados);

        assertThat(v.getNome()).isEqualTo("Novo Nome");
        assertThat(v.getPreco()).isEqualByComparingTo("75.50");
        assertThat(v.getTipo()).isEqualTo("Branco");
        assertThat(v.getUva()).isEqualTo("Chardonnay");
        assertThat(v.getSafra()).isEqualTo(2021);
        assertThat(v.getEstoque().getEstoqueMin()).isEqualTo(6);
    }

    @Test
    void atualizarNaoAlteraAQuantidadeEmEstoque() {
        Vodka d = (Vodka) produtoExistente(new Vodka("Vodka", BigDecimal.TEN, "Polônia"));
        var dados = new ProdutoRequest(null, "Vodka", BigDecimal.TEN, 999, null, null, "Rússia",
                null, null, null, null);

        service.atualizar(1L, dados);

        assertThat(d.getEstoque().getQuantidade()).isEqualTo(10);
        assertThat(d.getOrigem()).isEqualTo("Rússia");
    }

    @Test
    void naoPermiteMudarACategoria() {
        produtoExistente(new Vodka("Vodka", BigDecimal.TEN, "Polônia"));
        var dados = new ProdutoRequest(CategoriaProduto.GIN, "Vodka", BigDecimal.TEN, null, null,
                null, "Polônia", null, null, null, "Seco");

        assertThatThrownBy(() -> service.atualizar(1L, dados)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void atualizarProdutoInexistenteLancaExcecao() {
        when(produtoRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.atualizar(5L, vinho())).isInstanceOf(ProdutoInexistenteException.class);
    }

    // ---------- exclusão (RF05 / D3) ----------

    @Test
    void excluiProdutoNuncaVendido() {
        Produto p = produtoExistente(new Vodka("Vodka", BigDecimal.TEN, "Polônia"));
        when(itemVendaRepository.existsByProdutoId(1L)).thenReturn(false);

        service.excluir(1L);

        verify(produtoRepository).delete(p);
    }

    @Test
    void naoExcluiProdutoComVendas() {
        produtoExistente(new Vodka("Vodka", BigDecimal.TEN, "Polônia"));
        when(itemVendaRepository.existsByProdutoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(ProdutoComVendasException.class);
        verify(produtoRepository, never()).delete(any());
    }

    @Test
    void excluirInexistenteLancaExcecao() {
        when(produtoRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(ProdutoInexistenteException.class);
    }
}
