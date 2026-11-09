package com.br.adegueto.repository;

import com.br.adegueto.model.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
