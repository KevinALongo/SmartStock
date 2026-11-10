package com.smartstock.repository;

import com.smartstock.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EstoqueUniversalRepository extends JpaRepository<Lote, Long> {

    @Query("SELECT l FROM Lote l JOIN FETCH l.produto p WHERE l.dataValida >= CURRENT_DATE AND DATEDIFF(l.dataValida, CURRENT_DATE) <= 10 ORDER BY l.dataValida ASC")
    List<Lote> buscarLotesProximosDoVencimentoUniversal();

    @Query("SELECT p.nome, SUM(l.quantidadeAtual) as totalEstoque, p.estoqueMinimo, p.categoria " +
            "FROM Lote l JOIN l.produto p " +
            "GROUP BY p.id, p.nome, p.estoqueMinimo, p.categoria " +
            "HAVING SUM(l.quantidadeAtual) < p.estoqueMinimo")
    List<Object[]> verificarReposicaoUniversal();
}