package com.smartstock.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliticaDescontoTest {

    @Test
    void faixasDeDesconto() {
        assertEquals(50, PoliticaDesconto.percentualDesconto(0));
        assertEquals(50, PoliticaDesconto.percentualDesconto(2));
        assertEquals(30, PoliticaDesconto.percentualDesconto(3));
        assertEquals(30, PoliticaDesconto.percentualDesconto(5));
        assertEquals(10, PoliticaDesconto.percentualDesconto(6));
        assertEquals(10, PoliticaDesconto.percentualDesconto(10));
        assertEquals(0, PoliticaDesconto.percentualDesconto(11));
    }

    @Test
    void vencidoNaoRecebeDesconto() {
        assertEquals(0, PoliticaDesconto.percentualDesconto(-1));
        assertEquals(0, new BigDecimal("10.00")
                .compareTo(PoliticaDesconto.precoComDesconto(new BigDecimal("10.00"), -3)));
    }

    @Test
    void precoComDescontoArredondaParaDuasCasas() {
        assertEquals(0, new BigDecimal("5.00")
                .compareTo(PoliticaDesconto.precoComDesconto(new BigDecimal("10.00"), 1)));
        assertEquals(0, new BigDecimal("2.33")
                .compareTo(PoliticaDesconto.precoComDesconto(new BigDecimal("3.33"), 4)));
        assertEquals(0, new BigDecimal("9.00")
                .compareTo(PoliticaDesconto.precoComDesconto(new BigDecimal("10.00"), 8)));
    }
}
