package com.br.adegueto.service;

import com.br.adegueto.dto.EstoqueResponse;
import com.br.adegueto.dto.ProdutoMaisVendidoResponse;
import com.br.adegueto.dto.RelatorioEstoqueResponse;
import com.br.adegueto.dto.RelatorioVendasResponse;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.ItemVenda;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Venda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF11: relatórios consolidados. Reaproveita as consultas de vendas e estoque e agrega em memória
 * (volume compatível com o escopo do projeto). Cada método roda em uma única transação de leitura.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    public static final int LIMITE_PADRAO = 10;

    private final VendaService vendaService;
    private final EstoqueService estoqueService;

    /** Quantidade de vendas, faturamento e ticket médio do período (datas inclusivas e opcionais). */
    @Transactional(readOnly = true)
    public RelatorioVendasResponse resumoVendas(LocalDate inicio, LocalDate fim) {
        List<Venda> vendas = vendaService.historico(inicio, fim);

        BigDecimal faturamento = vendas.stream()
                .map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticketMedio = vendas.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(vendas.size()), 2, RoundingMode.HALF_UP);
        String mensagem = vendas.isEmpty() ? VendaService.MENSAGEM_NAO_ENCONTRADA : null;

        return new RelatorioVendasResponse(inicio, fim, vendas.size(), faturamento, ticketMedio, mensagem);
    }

    /**
     * Ranking por quantidade vendida (empate: ordem alfabética). O limite padrão é 10.
     * A agregação usa a própria instância de Produto como chave: dentro de uma transação o JPA
     * devolve a mesma instância para o mesmo registro.
     */
    @Transactional(readOnly = true)
    public List<ProdutoMaisVendidoResponse> produtosMaisVendidos(LocalDate inicio, LocalDate fim, Integer limite) {
        int max = limite == null ? LIMITE_PADRAO : limite;
        if (max <= 0) {
            throw new DadoInvalidoException("o limite deve ser maior que zero");
        }

        Map<Produto, Acumulado> porProduto = new LinkedHashMap<>();
        for (Venda venda : vendaService.historico(inicio, fim)) {
            for (ItemVenda item : venda.getItens()) {
                porProduto.computeIfAbsent(item.getProduto(), Acumulado::new).somar(item);
            }
        }

        return porProduto.values().stream()
                .sorted(Comparator.comparingLong(Acumulado::quantidade).reversed()
                        .thenComparing(a -> a.produto.getNome()))
                .limit(max)
                .map(a -> new ProdutoMaisVendidoResponse(a.produto.getId(), a.produto.getNome(),
                        a.produto.getCategoria(), a.quantidade, a.receita))
                .toList();
    }

    /** Posição de todos os produtos, com a contagem de itens em estoque baixo (RN002). */
    @Transactional(readOnly = true)
    public RelatorioEstoqueResponse posicaoEstoque() {
        List<Estoque> estoques = estoqueService.listar();
        List<EstoqueResponse> itens = estoques.stream().map(EstoqueResponse::de).toList();
        int baixos = (int) estoques.stream().filter(Estoque::checaMin).count();
        return new RelatorioEstoqueResponse(itens.size(), baixos, itens);
    }

    private static final class Acumulado {
        private final Produto produto;
        private long quantidade;
        private BigDecimal receita = BigDecimal.ZERO;

        private Acumulado(Produto produto) {
            this.produto = produto;
        }

        private void somar(ItemVenda item) {
            quantidade += item.getQuantidade();
            receita = receita.add(item.calcularSubtotal());
        }

        private long quantidade() {
            return quantidade;
        }
    }
}
