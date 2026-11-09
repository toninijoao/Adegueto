package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.adegueto.dto.ProdutoMaisVendidoResponse;
import com.br.adegueto.dto.RelatorioEstoqueResponse;
import com.br.adegueto.dto.RelatorioVendasResponse;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.ItemVenda;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Venda;
import com.br.adegueto.model.Vodka;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelatorioServiceTest {

    private final Usuario vendedor = new Funcionario("Beto", "beto", "h");
    private VendaService vendaService;
    private EstoqueService estoqueService;
    private RelatorioService service;

    @BeforeEach
    void preparar() {
        vendaService = mock(VendaService.class);
        estoqueService = mock(EstoqueService.class);
        service = new RelatorioService(vendaService, estoqueService);
    }

    private static Produto produto(String nome, String preco) {
        return new Vodka(nome, new BigDecimal(preco), "Polônia");
    }

    private Venda venda(ItemVenda... itens) {
        Venda venda = new Venda(vendedor);
        for (ItemVenda item : itens) {
            venda.adicionarItem(item);
        }
        venda.finalizar();
        return venda;
    }

    private void historicoRetorna(Venda... vendas) {
        when(vendaService.historico(any(), any())).thenReturn(List.of(vendas));
    }

    // ---------- resumo de vendas ----------

    @Test
    void resumoSomaFaturamentoECalculaTicketMedio() {
        Produto a = produto("A", "10.00");
        historicoRetorna(venda(new ItemVenda(a, 3)), venda(new ItemVenda(a, 7))); // 30 + 70

        RelatorioVendasResponse r = service.resumoVendas(null, null);

        assertThat(r.quantidadeVendas()).isEqualTo(2);
        assertThat(r.faturamento()).isEqualByComparingTo("100.00");
        assertThat(r.ticketMedio()).isEqualByComparingTo("50.00");
        assertThat(r.mensagem()).isNull();
    }

    @Test
    void ticketMedioEArredondadoParaDuasCasas() {
        Produto a = produto("A", "10.00");
        historicoRetorna(venda(new ItemVenda(a, 3)), venda(new ItemVenda(a, 3)), venda(new ItemVenda(a, 4))); // 100/3

        assertThat(service.resumoVendas(null, null).ticketMedio()).isEqualByComparingTo("33.33");
    }

    @Test
    void semVendasRetornaZerosEMensagemRN005() {
        historicoRetorna();

        RelatorioVendasResponse r = service.resumoVendas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(r.quantidadeVendas()).isZero();
        assertThat(r.faturamento()).isEqualByComparingTo("0.00");
        assertThat(r.ticketMedio()).isEqualByComparingTo("0.00");
        assertThat(r.mensagem()).isEqualTo("nenhuma venda encontrada");
        assertThat(r.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    // ---------- produtos mais vendidos ----------

    @Test
    void rankingAgrupaPorProdutoEOrdenaPorQuantidade() {
        Produto a = produto("A", "10.00");
        Produto b = produto("B", "20.00");
        Produto c = produto("C", "5.00");
        historicoRetorna(
                venda(new ItemVenda(a, 3), new ItemVenda(b, 4)),
                venda(new ItemVenda(a, 2), new ItemVenda(c, 1)));

        List<ProdutoMaisVendidoResponse> ranking = service.produtosMaisVendidos(null, null, null);

        assertThat(ranking).extracting(ProdutoMaisVendidoResponse::produtoNome).containsExactly("A", "B", "C");
        assertThat(ranking.get(0).quantidadeVendida()).isEqualTo(5);
        assertThat(ranking.get(0).receita()).isEqualByComparingTo("50.00");
        assertThat(ranking.get(1).receita()).isEqualByComparingTo("80.00");
        assertThat(ranking.get(0).categoria()).isEqualTo(CategoriaProduto.VODKA);
    }

    @Test
    void empateNaQuantidadeDesempataPorNome() {
        Produto zeta = produto("Zeta", "10.00");
        Produto alfa = produto("Alfa", "10.00");
        historicoRetorna(venda(new ItemVenda(zeta, 2), new ItemVenda(alfa, 2)));

        assertThat(service.produtosMaisVendidos(null, null, null))
                .extracting(ProdutoMaisVendidoResponse::produtoNome).containsExactly("Alfa", "Zeta");
    }

    @Test
    void limiteReduzOTamanhoDoRanking() {
        Produto a = produto("A", "10.00");
        Produto b = produto("B", "10.00");
        Produto c = produto("C", "10.00");
        historicoRetorna(venda(new ItemVenda(a, 3), new ItemVenda(b, 2), new ItemVenda(c, 1)));

        assertThat(service.produtosMaisVendidos(null, null, 2))
                .extracting(ProdutoMaisVendidoResponse::produtoNome).containsExactly("A", "B");
    }

    @Test
    void limitePadraoEDez() {
        Venda venda = new Venda(vendedor);
        for (int i = 1; i <= 12; i++) {
            venda.adicionarItem(new ItemVenda(produto("P" + i, "10.00"), i));
        }
        venda.finalizar();
        historicoRetorna(venda);

        assertThat(service.produtosMaisVendidos(null, null, null)).hasSize(RelatorioService.LIMITE_PADRAO);
    }

    @Test
    void limiteInvalidoEhRejeitado() {
        assertThatThrownBy(() -> service.produtosMaisVendidos(null, null, 0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.produtosMaisVendidos(null, null, -1)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void semVendasORankingFicaVazio() {
        historicoRetorna();
        assertThat(service.produtosMaisVendidos(null, null, null)).isEmpty();
    }

    // ---------- posição de estoque ----------

    @Test
    void relatorioDeEstoqueContaProdutosEItensBaixos() {
        Estoque abaixo = estoqueDe(produto("A", "10"), 1, 5);
        Estoque igual = estoqueDe(produto("B", "10"), 5, 5);
        Estoque acima = estoqueDe(produto("C", "10"), 9, 5);
        when(estoqueService.listar()).thenReturn(List.of(abaixo, igual, acima));

        RelatorioEstoqueResponse r = service.posicaoEstoque();

        assertThat(r.totalProdutos()).isEqualTo(3);
        assertThat(r.produtosComEstoqueBaixo()).isEqualTo(2);
        assertThat(r.itens()).extracting("estoqueBaixo").containsExactly(true, true, false);
    }

    private static Estoque estoqueDe(Produto produto, int quantidade, int minimo) {
        Estoque estoque = new Estoque(quantidade, minimo);
        produto.vincularEstoque(estoque);
        return estoque;
    }
}
