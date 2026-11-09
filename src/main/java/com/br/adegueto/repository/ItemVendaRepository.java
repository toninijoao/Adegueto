package com.br.adegueto.repository;

import com.br.adegueto.model.ItemVenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    boolean existsByProdutoId(Long produtoId);
}
