package com.smartstock.notificacao;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.dto.AlertaValidade;
import com.smartstock.dto.LoteRequest;
import com.smartstock.dto.ProdutoRequest;
import com.smartstock.model.Produto;
import com.smartstock.service.EstoqueService;
import com.smartstock.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AvisoDiarioTest {

    @Autowired AvisoDiario aviso;
    @Autowired ProdutoService produtos;
    @Autowired EstoqueService estoque;

    @Test
    void semAlertasNaoEnviaNada() {
        assertEquals(0, aviso.enviar());
    }

    @Test
    void contaAlertasDeValidadeEReposicao() {
        Produto p = produtos.criar(new ProdutoRequest("999", "Iogurte", "Laticínios", new BigDecimal("4.00"), 10));
        estoque.registrarEntrada(new LoteRequest(p.getId(), 2, LocalDate.now().plusDays(1)));

        // 1 alerta de validade (vence amanhã) + 1 de reposição (2 un. contra mínimo de 10)
        assertEquals(2, aviso.enviar());
    }

    @Test
    void mensagemListaOsItensDosAlertas() {
        AvisoDiario semDependencias = new AvisoDiario(null, null);
        var validade = List.of(new AlertaValidade("Leite", "Laticínios", "789", 3,
                LocalDate.of(2026, 11, 20), 7L, new BigDecimal("10.00"), new BigDecimal("9.00"),
                "ATENÇÃO: vence em 7 dia(s). Desconto de 10% aplicado."));
        var reposicao = List.of(new AlertaReposicao("Arroz", 2L, 10, "Grãos"));

        String mensagem = semDependencias.montarMensagem(validade, reposicao);

        assertTrue(mensagem.contains("Leite"));
        assertTrue(mensagem.contains("20/11/2026"));
        assertTrue(mensagem.contains("Arroz"));
        assertTrue(mensagem.contains("mínimo 10"));
    }
}
