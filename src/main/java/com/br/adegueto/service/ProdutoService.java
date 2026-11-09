package com.br.adegueto.service;

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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    /** RN005. */
    public static final String MENSAGEM_NAO_ENCONTRADO = "produto não encontrado";

    private final ProdutoRepository produtoRepository;
    private final ItemVendaRepository itemVendaRepository;

    /** RF03: cadastra o produto da categoria informada e cria o estoque associado. */
    @Transactional
    public Produto cadastrar(ProdutoRequest dados) {
        if (dados == null) {
            throw new DadoInvalidoException("os dados do produto são obrigatórios");
        }
        if (dados.categoria() == null) {
            throw new DadoInvalidoException("a categoria é obrigatória");
        }
        if (dados.quantidadeInicial() == null) {
            throw new DadoInvalidoException("a quantidade inicial é obrigatória");
        }
        Produto produto = criar(dados);
        int estoqueMin = dados.estoqueMin() == null ? 0 : dados.estoqueMin();
        produto.vincularEstoque(new Estoque(dados.quantidadeInicial(), estoqueMin));
        return produtoRepository.save(produto);
    }

    /** RF04: busca por nome (parcial, sem distinguir maiúsculas) e/ou categoria. Lista vazia é válida (RN005). */
    @Transactional(readOnly = true)
    public List<Produto> buscar(String nome, CategoriaProduto categoria) {
        String termo = nome == null ? "" : nome.trim();
        return produtoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(termo).stream()
                .filter(p -> categoria == null || p.getCategoria() == categoria)
                .toList();
    }

    @Transactional(readOnly = true)
    public Produto obter(Long id) {
        return produtoRepository.findById(id).orElseThrow(ProdutoInexistenteException::new);
    }

    /**
     * RF05: atualiza nome, preço, atributos específicos e estoque mínimo.
     * A categoria não pode mudar e a quantidade em estoque só muda por movimentações (Etapa 4).
     */
    @Transactional
    public Produto atualizar(Long id, ProdutoRequest dados) {
        if (dados == null) {
            throw new DadoInvalidoException("os dados do produto são obrigatórios");
        }
        Produto produto = obter(id);
        if (dados.categoria() != null && dados.categoria() != produto.getCategoria()) {
            throw new DadoInvalidoException("não é possível alterar a categoria de um produto");
        }

        produto.setNome(dados.nome());
        produto.setPreco(dados.preco());
        if (produto instanceof Vinho v) {
            v.setTipo(texto(dados.tipo(), "tipo"));
            v.setOrigem(texto(dados.origem(), "origem"));
            v.setUva(texto(dados.uva(), "uva"));
            v.setSafra(positivo(dados.safra(), "safra"));
        } else if (produto instanceof Whisky w) {
            w.setTipo(texto(dados.tipo(), "tipo"));
            w.setOrigem(texto(dados.origem(), "origem"));
            w.setIdade(positivo(dados.idade(), "idade"));
        } else if (produto instanceof Gin g) {
            g.setEstilo(texto(dados.estilo(), "estilo"));
            g.setOrigem(texto(dados.origem(), "origem"));
        } else if (produto instanceof Vodka d) {
            d.setOrigem(texto(dados.origem(), "origem"));
        }
        if (dados.estoqueMin() != null) {
            produto.getEstoque().setEstoqueMin(dados.estoqueMin());
        }
        return produto; // gravado ao fim da transação
    }

    /** RF05/D3: só exclui produtos que nunca foram vendidos. O estoque associado é removido junto. */
    @Transactional
    public void excluir(Long id) {
        Produto produto = obter(id);
        if (itemVendaRepository.existsByProdutoId(id)) {
            throw new ProdutoComVendasException();
        }
        produtoRepository.delete(produto);
    }

    private Produto criar(ProdutoRequest d) {
        return switch (d.categoria()) {
            case VINHO -> new Vinho(d.nome(), d.preco(), texto(d.tipo(), "tipo"), texto(d.origem(), "origem"),
                    texto(d.uva(), "uva"), positivo(d.safra(), "safra"));
            case WHISKY -> new Whisky(d.nome(), d.preco(), texto(d.tipo(), "tipo"), texto(d.origem(), "origem"),
                    positivo(d.idade(), "idade"));
            case GIN -> new Gin(d.nome(), d.preco(), texto(d.estilo(), "estilo"), texto(d.origem(), "origem"));
            case VODKA -> new Vodka(d.nome(), d.preco(), texto(d.origem(), "origem"));
        };
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("o campo " + campo + " é obrigatório para esta categoria");
        }
        return valor.trim();
    }

    private static int positivo(Integer valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new DadoInvalidoException("o campo " + campo + " é obrigatório e deve ser positivo");
        }
        return valor;
    }
}
