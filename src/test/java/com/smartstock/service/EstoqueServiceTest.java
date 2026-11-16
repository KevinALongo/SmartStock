package com.smartstock.service;

import com.smartstock.dto.*;
import com.smartstock.exception.RegraDeNegocioException;
import com.smartstock.model.Lote;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EstoqueServiceTest {

    private static final Pageable PAGINA = PageRequest.of(0, 50);

    @Autowired EstoqueService estoque;
    @Autowired ProdutoService produtos;
    @Autowired EstoqueRepository lotes;
    @Autowired Clock clock;

    private Produto novoProduto(String codigo, int minimo) {
        return produtos.criar(new ProdutoRequest(codigo, "Leite", "Laticínios", new BigDecimal("10.00"), minimo));
    }

    @Test
    void baixaConsomePrimeiroOLoteQueVenceAntes() {
        Produto p = novoProduto("111", 0);
        LocalDate hoje = LocalDate.now(clock);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(30)));
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(3)));

        BaixaResponse r = estoque.registrarBaixa(new BaixaRequest(p.getId(), 7));

        assertEquals(3, r.estoqueRestante());
        // o lote de 3 dias foi zerado, então não gera mais alerta de validade
        assertTrue(estoque.alertasValidade(PAGINA).isEmpty());
    }

    @Test
    void baixaMaiorQueOEstoqueEhRecusada() {
        Produto p = novoProduto("222", 0);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 2, LocalDate.now(clock).plusDays(20)));

        assertThrows(RegraDeNegocioException.class,
                () -> estoque.registrarBaixa(new BaixaRequest(p.getId(), 3)));
    }

    @Test
    void entradaDeLoteVencidoEhRecusada() {
        Produto p = novoProduto("333", 0);
        assertThrows(RegraDeNegocioException.class, () ->
                estoque.registrarEntrada(new LoteRequest(p.getId(), 1, LocalDate.now(clock).minusDays(1))));
    }

    @Test
    void alertaDeValidadeAplicaDesconto() {
        Produto p = novoProduto("444", 0);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 4, LocalDate.now(clock).plusDays(1)));

        List<AlertaValidade> alertas = estoque.alertasValidade(PAGINA).getContent();

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
        vencido.setDataValidade(LocalDate.now(clock).minusDays(2));
        lotes.save(vencido);

        List<AlertaReposicao> alertas = estoque.alertasReposicao(PAGINA).getContent();

        assertTrue(alertas.stream().anyMatch(a -> a.produto().equals("Leite") && a.estoqueAtual() == 0));
    }

    @Test
    void listagemDeProdutosEhPaginada() {
        novoProduto("A1", 0);
        novoProduto("A2", 0);
        novoProduto("A3", 0);

        Page<Produto> primeira = produtos.listar(PageRequest.of(0, 2));
        Page<Produto> segunda = produtos.listar(PageRequest.of(1, 2));

        assertEquals(3, primeira.getTotalElements());
        assertEquals(2, primeira.getTotalPages());
        assertEquals(2, primeira.getContent().size());
        assertEquals(1, segunda.getContent().size());
    }

    @Test
    void alertasDeReposicaoSaoPaginadosEContados() {
        novoProduto("B1", 5);
        novoProduto("B2", 5);
        novoProduto("B3", 5);

        Page<AlertaReposicao> pagina = estoque.alertasReposicao(PageRequest.of(0, 2));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, pagina.getContent().size());
        assertEquals(3, estoque.todosAlertasReposicao().size());
    }

    @Test
    void baixaAplicaODescontoDeCadaLoteERetornaOValorTotal() {
        Produto p = novoProduto("666", 0);
        LocalDate hoje = LocalDate.now(clock);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 2, hoje.plusDays(1)));   // 50% -> 5.00
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(20)));  // sem desconto -> 10.00

        BaixaResponse r = estoque.registrarBaixa(new BaixaRequest(p.getId(), 3));

        // 2 un. a 5.00 + 1 un. a 10.00
        assertEquals(0, new BigDecimal("20.00").compareTo(r.valorTotal()));
        assertEquals(2, r.itens().size());
        assertEquals(50, r.itens().get(0).percentualDesconto());
        assertEquals(0, r.itens().get(1).percentualDesconto());
        assertEquals(4, r.estoqueRestante());
    }
}
