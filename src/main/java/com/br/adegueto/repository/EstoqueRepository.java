package com.br.adegueto.repository;

import com.br.adegueto.model.Estoque;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByProdutoId(Long produtoId);

    List<Estoque> findAllByOrderByProdutoNomeAsc();
}
