package com.br.adegueto.service;

import com.br.adegueto.dto.ItemVendaRequest;
import com.br.adegueto.dto.VendaRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.NaoAutenticadoException;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import com.br.adegueto.exception.VendaInexistenteException;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.ItemVenda;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Venda;
import com.br.adegueto.repository.EstoqueRepository;
import com.br.adegueto.repository.UsuarioRepository;
import com.br.adegueto.repository.VendaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendaService {

    /** RN005. */
    public static final String MENSAGEM_NAO_ENCONTRADA = "nenhuma venda encontrada";

    /** Limites usados quando o período é aberto (sem data inicial e/ou final). */
    private static final LocalDate INICIO_PADRAO = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIM_PADRAO = LocalDate.of(2999, 12, 31);

    private final VendaRepository vendaRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * RF07/RF08: registra a venda e dá baixa no estoque em uma única transação.
     * Se qualquer item falhar (produto inexistente, estoque insuficiente, conflito de concorrência),
     * nada é gravado.
     */
    @Transactional
    public Venda registrar(Long usuarioId, VendaRequest dados) {
        if (dados == null || dados.itens() == null || dados.itens().isEmpty()) {
            throw new DadoInvalidoException("a venda deve possuir ao menos um item");
        }
        Usuario vendedor = usuarioRepository.findById(usuarioId).orElseThrow(NaoAutenticadoException::new);
        Map<Long, Integer> quantidades = consolidar(dados.itens());

        Venda venda = new Venda(vendedor);
        quantidades.forEach((produtoId, solicitado) -> {
            Estoque estoque = estoqueRepository.findByProdutoId(produtoId)
                    .orElseThrow(ProdutoInexistenteException::new);
            Produto produto = estoque.getProduto();
            if (solicitado > estoque.getQuantidade()) {
                throw new EstoqueInsuficienteException(produto.getNome(), solicitado, estoque.getQuantidade());
            }
            venda.adicionarItem(new ItemVenda(produto, solicitado)); // preço congelado neste momento
            estoque.remover(solicitado);
        });

        venda.finalizar();
        // O flush grava a venda e os estoques juntos e dispara a verificação de versão (D5).
        return vendaRepository.saveAndFlush(venda);
    }

    /** RF09: histórico por período (datas inclusivas, ambas opcionais), da venda mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<Venda> historico(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new DadoInvalidoException("a data inicial não pode ser posterior à data final");
        }
        LocalDateTime de = (inicio == null ? INICIO_PADRAO : inicio).atStartOfDay();
        LocalDateTime ate = (fim == null ? FIM_PADRAO : fim).plusDays(1).atStartOfDay();
        return vendaRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(de, ate);
    }

    @Transactional(readOnly = true)
    public Venda obter(Long id) {
        return vendaRepository.buscarCompletaPorId(id).orElseThrow(VendaInexistenteException::new);
    }

    /** Valida cada linha e soma as quantidades de um mesmo produto informado mais de uma vez. */
    private static Map<Long, Integer> consolidar(List<ItemVendaRequest> itens) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemVendaRequest item : itens) {
            if (item == null || item.produtoId() == null) {
                throw new DadoInvalidoException("o produto do item é obrigatório");
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new QuantidadeInvalidaException("a quantidade de cada item deve ser maior que zero");
            }
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }
        return quantidades;
    }
}
