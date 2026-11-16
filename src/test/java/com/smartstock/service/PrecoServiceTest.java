package com.smartstock.service;

import com.smartstock.dto.LoteRequest;
import com.smartstock.dto.PrecoAtual;
import com.smartstock.dto.ProdutoRequest;
import com.smartstock.exception.RecursoNaoEncontradoException;
import com.smartstock.model.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PrecoServiceTest {

    @Autowired PrecoService precos;
    @Autowired ProdutoService produtos;
    @Autowired EstoqueService estoque;
    @Autowired Clock clock;

    private Produto novoProduto(String codigo) {
        return produtos.criar(new ProdutoRequest(codigo, "Leite", "Laticínios", new BigDecimal("10.00"), 0));
    }

    private static void assertPreco(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "preço esperado " + esperado + " mas veio " + real);
    }

    @Test
    void precoAtualJaVemComODescontoDeValidade() {
        Produto p = novoProduto("P1");
        estoque.registrarEntrada(new LoteRequest(p.getId(), 4, LocalDate.now(clock).plusDays(1)));

        PrecoAtual preco = precos.porProduto(p.getId());

        assertPreco("10.00", preco.precoOriginal());
        assertPreco("5.00", preco.precoAtual());
        assertEquals(50, preco.percentualDesconto());
        assertEquals(1L, preco.diasParaVencer());
        assertEquals(4L, preco.estoqueDisponivel());
        assertTrue(preco.status().contains("50%"));
    }

    @Test
    void precoSegueOLoteQueSeraVendidoPrimeiro() {
        Produto p = novoProduto("P2");
        LocalDate hoje = LocalDate.now(clock);
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(20)));
        estoque.registrarEntrada(new LoteRequest(p.getId(), 5, hoje.plusDays(3)));

        PrecoAtual preco = precos.porProduto(p.getId());

        assertPreco("7.00", preco.precoAtual());
        assertEquals(30, preco.percentualDesconto());
        assertEquals(hoje.plusDays(3), preco.dataValidadeProximoLote());
        assertEquals(10L, preco.estoqueDisponivel());
    }

    @Test
    void semEstoqueDisponivelUsaPrecoDeTabela() {
        Produto p = novoProduto("P3");

        PrecoAtual preco = precos.porProduto(p.getId());

        assertPreco("10.00", preco.precoAtual());
        assertEquals(0, preco.percentualDesconto());
        assertEquals(0L, preco.estoqueDisponivel());
        assertNull(preco.dataValidadeProximoLote());
        assertTrue(preco.status().startsWith("SEM ESTOQUE"));
    }

    @Test
    void produtoSemValidadeNuncaTemDesconto() {
        Produto p = novoProduto("P4");
        estoque.registrarEntrada(new LoteRequest(p.getId(), 3, null));

        PrecoAtual preco = precos.porProduto(p.getId());

        assertPreco("10.00", preco.precoAtual());
        assertEquals(0, preco.percentualDesconto());
        assertNull(preco.diasParaVencer());
        assertEquals(3L, preco.estoqueDisponivel());
    }

    @Test
    void buscaPeloCodigoDeBarras() {
        Produto p = novoProduto("789000111");
        estoque.registrarEntrada(new LoteRequest(p.getId(), 1, LocalDate.now(clock).plusDays(8)));

        PrecoAtual preco = precos.porCodigoBarras("789000111");

        assertEquals(p.getId(), preco.produtoId());
        assertPreco("9.00", preco.precoAtual());
    }

    @Test
    void produtoInexistenteRetornaNaoEncontrado() {
        assertThrows(RecursoNaoEncontradoException.class, () -> precos.porProduto(987654321L));
        assertThrows(RecursoNaoEncontradoException.class, () -> precos.porCodigoBarras("nao-existe"));
    }

    @Test
    void listagemDePrecosEhPaginadaECalculaCadaProduto() {
        Produto a = produtos.criar(new ProdutoRequest("L1", "Arroz", "Grãos", new BigDecimal("20.00"), 0));
        Produto b = produtos.criar(new ProdutoRequest("L2", "Leite", "Laticínios", new BigDecimal("10.00"), 0));
        produtos.criar(new ProdutoRequest("L3", "Pão", "Padaria", new BigDecimal("8.00"), 0));
        estoque.registrarEntrada(new LoteRequest(b.getId(), 2, LocalDate.now(clock).plusDays(2)));

        var pagina = precos.listar(PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("nome")));
        List<PrecoAtual> itens = pagina.getContent();

        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, itens.size());
        assertEquals("Arroz", itens.get(0).produto());
        assertPreco("20.00", itens.get(0).precoAtual());
        assertEquals("Leite", itens.get(1).produto());
        assertPreco("5.00", itens.get(1).precoAtual());
        assertNotNull(a.getId());
    }
}
