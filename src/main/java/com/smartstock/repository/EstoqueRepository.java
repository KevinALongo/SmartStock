package com.smartstock.repository;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.model.Lote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EstoqueRepository extends JpaRepository<Lote, Long> {

    boolean existsByProdutoId(Long produtoId);

    @Query("""
        SELECT l FROM Lote l JOIN FETCH l.produto p
        WHERE l.quantidadeAtual > 0
          AND l.dataValidade IS NOT NULL
          AND l.dataValidade <= :limite
        ORDER BY l.dataValidade ASC
        """)
    List<Lote> buscarLotesAteData(@Param("limite") LocalDate limite);

    /** Produtos cujo estoque válido (não vencido) está abaixo do mínimo. */
    @Query("""
        SELECT new com.smartstock.dto.AlertaReposicao(
            p.nome, COALESCE(SUM(l.quantidadeAtual), 0L), p.estoqueMinimo, p.categoria)
        FROM Produto p
        LEFT JOIN Lote l ON l.produto = p
                        AND (l.dataValidade IS NULL OR l.dataValidade >= :hoje)
        GROUP BY p.id, p.nome, p.estoqueMinimo, p.categoria
        HAVING COALESCE(SUM(l.quantidadeAtual), 0L) < p.estoqueMinimo
        """)
    List<AlertaReposicao> buscarProdutosAbaixoDoMinimo(@Param("hoje") LocalDate hoje);

    /** Lotes disponíveis para venda, do que vence primeiro ao que vence por último (FEFO). Sem validade vai por último. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM Lote l
        WHERE l.produto.id = :produtoId
          AND l.quantidadeAtual > 0
          AND (l.dataValidade IS NULL OR l.dataValidade >= :hoje)
        ORDER BY CASE WHEN l.dataValidade IS NULL THEN 1 ELSE 0 END, l.dataValidade ASC
        """)
    List<Lote> buscarLotesDisponiveis(@Param("produtoId") Long produtoId, @Param("hoje") LocalDate hoje);
}
