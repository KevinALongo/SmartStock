package com.smartstock.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaDescontoTest {

    private final PoliticaDesconto politica = PoliticaDesconto.padrao();

    @Test
    void faixasDeDesconto() {
        assertEquals(50, politica.percentualDesconto(0));
        assertEquals(50, politica.percentualDesconto(2));
        assertEquals(30, politica.percentualDesconto(3));
        assertEquals(30, politica.percentualDesconto(5));
        assertEquals(10, politica.percentualDesconto(6));
        assertEquals(10, politica.percentualDesconto(10));
        assertEquals(0, politica.percentualDesconto(11));
    }

    @Test
    void vencidoNaoRecebeDesconto() {
        assertEquals(0, politica.percentualDesconto(-1));
        assertEquals(0, new BigDecimal("10.00")
                .compareTo(politica.precoComDesconto(new BigDecimal("10.00"), -3)));
    }

    @Test
    void produtoSemValidadeNaoRecebeDesconto() {
        assertEquals(0, politica.percentualDesconto(Long.MAX_VALUE));
    }

    @Test
    void precoComDescontoArredondaParaDuasCasas() {
        assertEquals(0, new BigDecimal("5.00")
                .compareTo(politica.precoComDesconto(new BigDecimal("10.00"), 1)));
        assertEquals(0, new BigDecimal("2.33")
                .compareTo(politica.precoComDesconto(new BigDecimal("3.33"), 4)));
        assertEquals(0, new BigDecimal("9.00")
                .compareTo(politica.precoComDesconto(new BigDecimal("10.00"), 8)));
    }

    @Test
    void faixasPersonalizadas() {
        PoliticaDesconto custom = new PoliticaDesconto("1:70, 7:20, 15:5");

        assertEquals(70, custom.percentualDesconto(1));
        assertEquals(20, custom.percentualDesconto(7));
        assertEquals(5, custom.percentualDesconto(15));
        assertEquals(0, custom.percentualDesconto(16));
        assertEquals(15, custom.maiorFaixaDias());
    }

    @Test
    void padraoTemMaiorFaixaDeDezDias() {
        assertEquals(10, politica.maiorFaixaDias());
    }

    @Test
    void nenhumaDesligaODesconto() {
        PoliticaDesconto semDesconto = new PoliticaDesconto("nenhuma");

        assertEquals(0, semDesconto.percentualDesconto(0));
        assertEquals(0, semDesconto.maiorFaixaDias());
    }

    @Test
    void configuracaoEmBrancoUsaOPadrao() {
        assertEquals(50, new PoliticaDesconto("  ").percentualDesconto(1));
    }

    @Test
    void configuracaoInvalidaEhRecusadaComMensagemClara() {
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("abc"));
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("2-50"));
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("2:150"));
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("-1:10"));
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("5:30,2:50"));
        assertThrows(IllegalStateException.class, () -> new PoliticaDesconto("2:50,2:30"));
    }
}
