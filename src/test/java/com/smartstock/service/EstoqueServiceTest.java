package com.smartstock.service;

import com.smartstock.dto.*;
import com.smartstock.exception.RegraDeNegocioException;
import com.smartstock.model.Lote;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EstoqueServiceTest {

    @Autowired EstoqueService estoque;
    @Autowired ProdutoService produtos;
    @Autowired EstoqueRepository lotes;

    private Produto novoProduto(String codigo, int minimo) {
        return produtos.criar(new ProdutoRequest(codigo, "Leite", "Laticínios", new BigDecimal("10.00"), minimo));
    }

    @Test
    void baixaConsomePrimeiroOLoteQueVenceAntes() {
        Produto p = novoProduto("111", 0);
        LocalDate hoje = LocalDate.now();
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(30)));
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(3)));

        BaixaResponse r = estoque.registrarBaixa(new BaixaRequest(p.getId(), 7));

        assertEquals(3, r.estoqueRestante());
        // o lote de 3 dias foi zerado, então não gera mais alerta de validade
        assertTrue(estoque.alertasValidade().isEmpty());
    }

    @Test
    void baixaMaiorQueOEstoqueEhRecusada() {
        Produto p = novoProduto("222", 0);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 2, LocalDate.now().plusDays(20)));

        assertThrows(RegraDeNegocioException.class,
                () -> estoque.registrarBaixa(new BaixaRequest(p.getId(), 3)));
    }

    @Test
    void entradaDeLoteVencidoEhRecusada() {
        Produto p = novoProduto("333", 0);
        assertThrows(RegraDeNegocioException.class, () ->
                estoque.registrarEntrada(new LoteRequest(p.getId(), 1, LocalDate.now().minusDays(1))));
    }

    @Test
    void alertaDeValidadeAplicaDesconto() {
        Produto p = novoProduto("444", 0);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 4, LocalDate.now().plusDays(1)));

        List<AlertaValidade> alertas = estoque.alertasValidade();

        assertEquals(1, alertas.size());
        assertEquals(0, new BigDecimal("5.00").compareTo(alertas.get(0).precoComDesconto()));
        assertTrue(alertas.get(0).status().contains("50%"));
    }

    @Test
    void reposicaoNaoContaLoteVencidoComoEstoque() {
        Produto p = novoProduto("555", 5);
        Lote vencido = new Lote();
        vencido.setProduto(p);
        vencido.setQuantidadeAtual(10);
        vencido.setDataValidade(LocalDate.now().minusDays(2));
        lotes.save(vencido);

        List<AlertaReposicao> alertas = estoque.alertasReposicao();

        assertTrue(alertas.stream().anyMatch(a -> a.produto().equals("Leite") && a.estoqueAtual() == 0));
    }
}
