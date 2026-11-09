package com.br.adegueto.service;

import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.repository.EstoqueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;

    /** RF06: posição de estoque de todos os produtos, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<Estoque> listar() {
        return estoqueRepository.findAllByOrderByProdutoNomeAsc();
    }

    /** RF10/RN002: produtos com quantidade menor ou igual ao estoque mínimo. */
    @Transactional(readOnly = true)
    public List<Estoque> listarBaixo() {
        return estoqueRepository.findAllByOrderByProdutoNomeAsc().stream()
                .filter(Estoque::checaMin)
                .toList();
    }

    /** RF06/RN003: entrada de produtos. */
    @Transactional
    public Estoque entrada(Long produtoId, Integer quantidade) {
        Estoque estoque = localizar(produtoId);
        estoque.adicionar(exigirQuantidade(quantidade));
        return estoqueRepository.saveAndFlush(estoque);
    }

    /** RF06/RN003: saída manual de produtos. Não permite saldo negativo. */
    @Transactional
    public Estoque saida(Long produtoId, Integer quantidade) {
        Estoque estoque = localizar(produtoId);
        estoque.remover(exigirQuantidade(quantidade));
        return estoqueRepository.saveAndFlush(estoque);
    }

    private Estoque localizar(Long produtoId) {
        return estoqueRepository.findByProdutoId(produtoId).orElseThrow(ProdutoInexistenteException::new);
    }

    private static int exigirQuantidade(Integer quantidade) {
        if (quantidade == null) {
            throw new QuantidadeInvalidaException("a quantidade é obrigatória");
        }
        return quantidade; // a regra "maior que zero" é do próprio Estoque
    }
}
