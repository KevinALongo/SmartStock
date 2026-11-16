package com.smartstock.config;

import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapeamentoNomesFisicosTest {

    private static MapeamentoNomesFisicos com(Map<String, String> config) {
        return new MapeamentoNomesFisicos(config::get);
    }

    @Test
    void semConfiguracaoMantemOsNomesPadrao() {
        var estrategia = com(Map.of());

        assertEquals("produtos", estrategia.toPhysicalTableName(Identifier.toIdentifier("produtos"), null).getText());
        assertEquals("codigo_barras", estrategia.toPhysicalColumnName(Identifier.toIdentifier("codigo_barras"), null).getText());
        assertEquals("id", estrategia.toPhysicalColumnName(Identifier.toIdentifier("pk_produto"), null).getText());
        assertEquals("id", estrategia.toPhysicalColumnName(Identifier.toIdentifier("pk_lote"), null).getText());
    }

    @Test
    void usaOsNomesConfiguradosParaTabelasEColunas() {
        var estrategia = com(Map.of(
                MapeamentoNomesFisicos.PREFIXO + "produtos", "tb_produto",
                MapeamentoNomesFisicos.PREFIXO + "pk_produto", "cod_produto",
                MapeamentoNomesFisicos.PREFIXO + "data_validade", "validade"));

        assertEquals("tb_produto", estrategia.toPhysicalTableName(Identifier.toIdentifier("produtos"), null).getText());
        assertEquals("cod_produto", estrategia.toPhysicalColumnName(Identifier.toIdentifier("pk_produto"), null).getText());
        assertEquals("validade", estrategia.toPhysicalColumnName(Identifier.toIdentifier("data_validade"), null).getText());
        // o que não foi configurado continua igual
        assertEquals("lotes", estrategia.toPhysicalTableName(Identifier.toIdentifier("lotes"), null).getText());
    }

    @Test
    void valorEntreAspasViraIdentificadorComAspas() {
        var estrategia = com(Map.of(MapeamentoNomesFisicos.PREFIXO + "produtos", "\"Produtos\""));

        Identifier id = estrategia.toPhysicalTableName(Identifier.toIdentifier("produtos"), null);

        assertEquals("Produtos", id.getText());
        assertTrue(id.isQuoted());
    }
}
