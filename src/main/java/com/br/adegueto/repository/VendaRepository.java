package com.br.adegueto.repository;

import com.br.adegueto.model.Venda;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    /** Vendas no intervalo [inicio, fimExclusivo), da mais recente para a mais antiga, já com itens e vendedor. */
    @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
    List<Venda> findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(LocalDateTime inicio,
                                                                          LocalDateTime fimExclusivo);

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.produto"})
    @Query("select v from Venda v where v.id = :id")
    Optional<Venda> buscarCompletaPorId(@Param("id") Long id);
}
