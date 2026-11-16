package com.smartstock.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prova que dá para apontar o sistema para tabelas/colunas com outros nomes só pela configuração.
 * Usa um H2 próprio para não misturar com o esquema dos outros testes.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:banco_existente;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "smartstock.banco.nomes.produtos=tb_mercadoria",
        "smartstock.banco.nomes.pk_produto=cod_mercadoria",
        "smartstock.banco.nomes.codigo_barras=ean",
        "smartstock.banco.nomes.preco_original=preco_venda",
        "smartstock.banco.nomes.estoque_minimo=minimo",
        "smartstock.banco.nomes.lotes=tb_lote_estoque",
        "smartstock.banco.nomes.pk_lote=cod_lote",
        "smartstock.banco.nomes.produto_id=cod_mercadoria_fk",
        "smartstock.banco.nomes.quantidade_atual=qtd",
        "smartstock.banco.nomes.data_validade=validade"
})
@Transactional
class BancoExistenteTest {

    @Autowired EntityManager em;

    @Test
    void tabelasEColunasUsamOsNomesConfigurados() {
        Number produtos = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM tb_mercadoria "
                        + "WHERE cod_mercadoria IS NULL AND ean IS NULL AND nome IS NULL "
                        + "AND categoria IS NULL AND preco_venda IS NULL AND minimo IS NULL")
                .getSingleResult();
        Number lotes = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM tb_lote_estoque "
                        + "WHERE cod_lote IS NULL AND cod_mercadoria_fk IS NULL AND qtd IS NULL AND validade IS NULL")
                .getSingleResult();

        assertEquals(0, produtos.intValue());
        assertEquals(0, lotes.intValue());
    }
}
