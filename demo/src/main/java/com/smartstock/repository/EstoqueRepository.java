package com.smartstock.repository;


import com.smartstock.dto.AlertaReposicao;
import com.smartstock.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EstoqueRepository extends JpaRepository<Lote, Long> {

    @Query("""
        SELECT l FROM Lote l JOIN FETCH l.produto p
        WHERE l.quantidadeAtual > 0
          AND l.dataValidade IS NOT NULL
          AND l.dataValidade <= :limite
        ORDER BY l.dataValidade ASC
        """)
    List<Lote> buscarLotesAteData(@Param("limite") LocalDate limite);

    @Query("""
        SELECT new com.smartstock.dto.AlertaReposicao(
            p.nome, COALESCE(SUM(l.quantidadeAtual), 0L), p.estoqueMinimo, p.categoria)
        FROM Produto p LEFT JOIN Lote l ON l.produto = p
        GROUP BY p.id, p.nome, p.estoqueMinimo, p.categoria
        HAVING COALESCE(SUM(l.quantidadeAtual), 0L) < p.estoqueMinimo
        """)
    List<AlertaReposicao> buscarProdutosAbaixoDoMinimo();
}
