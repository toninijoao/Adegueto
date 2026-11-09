package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.adegueto.dto.ItemVendaRequest;
import com.br.adegueto.dto.VendaRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.NaoAutenticadoException;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import com.br.adegueto.exception.VendaInexistenteException;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Gin;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Venda;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.repository.EstoqueRepository;
import com.br.adegueto.repository.UsuarioRepository;
import com.br.adegueto.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VendaServiceTest {

    private VendaRepository vendaRepository;
    private EstoqueRepository estoqueRepository;
    private UsuarioRepository usuarioRepository;
    private VendaService service;
    private Usuario vendedor;

    @BeforeEach
    void preparar() {
        vendaRepository = mock(VendaRepository.class);
        estoqueRepository = mock(EstoqueRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        service = new VendaService(vendaRepository, estoqueRepository, usuarioRepository);

        vendedor = new Funcionario("Beto", "beto", "h");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(vendaRepository.saveAndFlush(any(Venda.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Estoque estoqueDe(Produto produto, int quantidade) {
        Estoque estoque = new Estoque(quantidade, 1);
        produto.vincularEstoque(estoque);
        return estoque;
    }

    private void cadastrar(long produtoId, Estoque estoque) {
        when(estoqueRepository.findByProdutoId(produtoId)).thenReturn(Optional.of(estoque));
    }

    private static VendaRequest venda(ItemVendaRequest... itens) {
        return new VendaRequest(List.of(itens));
    }

    // ---------- registro (RF07 / RF08) ----------

    @Test
    void registraVendaCalculaTotalEBaixaOEstoque() {
        Estoque estoque = estoqueDe(new Vodka("Vodka", new BigDecimal("10.00"), "Polônia"), 10);
        cadastrar(5L, estoque);

        Venda venda = service.registrar(1L, venda(new ItemVendaRequest(5L, 3)));

        assertThat(venda.getValorTotal()).isEqualByComparingTo("30.00");
        assertThat(venda.getItens()).hasSize(1);
        assertThat(venda.getUsuario()).isSameAs(vendedor);
        assertThat(venda.getData()).isNotNull();
        assertThat(estoque.getQuantidade()).isEqualTo(7);
        verify(vendaRepository).saveAndFlush(venda);
    }

    @Test
    void vendaComVariosItensSomaOsSubtotaisEBaixaCadaEstoque() {
        Estoque vodka = estoqueDe(new Vodka("Vodka", new BigDecimal("10.00"), "Polônia"), 10);
        Estoque gin = estoqueDe(new Gin("Gin", new BigDecimal("55.50"), "London Dry", "Inglaterra"), 4);
        cadastrar(1L, vodka);
        cadastrar(2L, gin);

        Venda venda = service.registrar(1L, venda(new ItemVendaRequest(1L, 3), new ItemVendaRequest(2L, 2)));

        assertThat(venda.getValorTotal()).isEqualByComparingTo("141.00");
        assertThat(vodka.getQuantidade()).isEqualTo(7);
        assertThat(gin.getQuantidade()).isEqualTo(2);
    }

    @Test
    void itemCongelaOPrecoDoMomentoDaVenda() {
        Produto produto = new Vodka("Vodka", new BigDecimal("20.00"), "Polônia");
        cadastrar(1L, estoqueDe(produto, 10));

        Venda venda = service.registrar(1L, venda(new ItemVendaRequest(1L, 2)));
        produto.setPreco(new BigDecimal("99.00"));

        assertThat(venda.getItens().get(0).getPrecoUnitario()).isEqualByComparingTo("20.00");
        assertThat(venda.getValorTotal()).isEqualByComparingTo("40.00");
    }

    @Test
    void vendeTodoOSaldoDisponivel() {
        Estoque estoque = estoqueDe(new Vodka("Vodka", BigDecimal.TEN, "Polônia"), 3);
        cadastrar(1L, estoque);

        service.registrar(1L, venda(new ItemVendaRequest(1L, 3)));

        assertThat(estoque.getQuantidade()).isZero();
    }

    @Test
    void mesmoProdutoInformadoDuasVezesEConsolidado() {
        Estoque estoque = estoqueDe(new Vodka("Vodka", new BigDecimal("10.00"), "Polônia"), 10);
        cadastrar(1L, estoque);

        Venda venda = service.registrar(1L, venda(new ItemVendaRequest(1L, 2), new ItemVendaRequest(1L, 3)));

        assertThat(venda.getItens()).hasSize(1);
        assertThat(venda.getItens().get(0).getQuantidade()).isEqualTo(5);
        assertThat(estoque.getQuantidade()).isEqualTo(5);
    }

    @Test
    void consolidacaoNaoPermiteBurlarOEstoque() {
        Estoque estoque = estoqueDe(new Vodka("Vodka", BigDecimal.TEN, "Polônia"), 4);
        cadastrar(1L, estoque);

        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, 3), new ItemVendaRequest(1L, 3))))
                .isInstanceOf(EstoqueInsuficienteException.class);
        verify(vendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void estoqueInsuficienteIdentificaOProdutoENaoGrava() {
        cadastrar(1L, estoqueDe(new Vodka("Vodka Pura", BigDecimal.TEN, "Polônia"), 2));

        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, 5))))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Vodka Pura")
                .hasMessageContaining("solicitado 5")
                .hasMessageContaining("disponível 2");
        verify(vendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void umItemSemEstoqueImpedeAVendaInteira() {
        Estoque ok = estoqueDe(new Vodka("Vodka", BigDecimal.TEN, "Polônia"), 10);
        Estoque curto = estoqueDe(new Gin("Gin", BigDecimal.TEN, "Seco", "Inglaterra"), 1);
        cadastrar(1L, ok);
        cadastrar(2L, curto);

        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, 2), new ItemVendaRequest(2L, 5))))
                .isInstanceOf(EstoqueInsuficienteException.class);
        verify(vendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void vendaSemItensEhRejeitada() {
        assertThatThrownBy(() -> service.registrar(1L, null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.registrar(1L, new VendaRequest(null))).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.registrar(1L, new VendaRequest(List.of())))
                .isInstanceOf(DadoInvalidoException.class);
        verify(vendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void quantidadesInvalidasSaoRejeitadas() {
        cadastrar(1L, estoqueDe(new Vodka("Vodka", BigDecimal.TEN, "Polônia"), 10));

        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, 0))))
                .isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, null))))
                .isInstanceOf(QuantidadeInvalidaException.class);
        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, -3))))
                .isInstanceOf(QuantidadeInvalidaException.class);
        // quantidades que se anulariam depois de somadas também são recusadas
        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(1L, 5), new ItemVendaRequest(1L, -5))))
                .isInstanceOf(QuantidadeInvalidaException.class);
    }

    @Test
    void itemSemProdutoEhRejeitado() {
        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(null, 1))))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void produtoInexistenteEhRejeitado() {
        when(estoqueRepository.findByProdutoId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, venda(new ItemVendaRequest(9L, 1))))
                .isInstanceOf(ProdutoInexistenteException.class);
        verify(vendaRepository, never()).saveAndFlush(any());
    }

    @Test
    void usuarioInexistenteNaSessaoEhRejeitado() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(2L, venda(new ItemVendaRequest(1L, 1))))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    // ---------- histórico (RF09) ----------

    @Test
    void historicoUsaOPeriodoInclusivoDeAmbasAsDatas() {
        when(vendaRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(any(), any()))
                .thenReturn(List.of());

        service.historico(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        ArgumentCaptor<LocalDateTime> de = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(vendaRepository).findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(de.capture(), ate.capture());
        assertThat(de.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(ate.getValue()).isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0)); // 30/11 entra por inteiro
    }

    @Test
    void historicoSemDatasAbreOPeriodo() {
        Venda existente = new Venda(vendedor);
        when(vendaRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(any(), any()))
                .thenReturn(List.of(existente));

        assertThat(service.historico(null, null)).containsExactly(existente);
    }

    @Test
    void historicoSemResultadosRetornaListaVazia() {
        when(vendaRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(any(), any()))
                .thenReturn(List.of());

        assertThat(service.historico(LocalDate.of(2026, 1, 1), null)).isEmpty();
    }

    @Test
    void historicoComDataInicialPosteriorAFinalEhRejeitado() {
        assertThatThrownBy(() -> service.historico(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void obterVendaInexistenteLancaExcecao() {
        when(vendaRepository.buscarCompletaPorId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(9L))
                .isInstanceOf(VendaInexistenteException.class)
                .hasMessage("venda não encontrada");
    }
}
