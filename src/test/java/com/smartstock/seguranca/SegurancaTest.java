package com.smartstock.seguranca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
class SegurancaTest {

    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtrosDeSeguranca;

    private MockMvc mvc;

    @BeforeEach
    void montar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguranca).build();
    }

    private static String basic(String usuario, String senha) {
        String credenciais = usuario + ":" + senha;
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void semCredenciaisRetorna401() throws Exception {
        int status = mvc.perform(get("/api/produtos")).andReturn().getResponse().getStatus();
        assertEquals(401, status);
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        int status = mvc.perform(get("/api/produtos")
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "senha-errada")))
                .andReturn().getResponse().getStatus();
        assertEquals(401, status);
    }

    @Test
    void credenciaisCorretasRetornam200ComListaPaginada() throws Exception {
        var resposta = mvc.perform(get("/api/produtos?page=0&size=5")
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "senha-de-teste")))
                .andReturn().getResponse();

        assertEquals(200, resposta.getStatus());
        String corpo = resposta.getContentAsString();
        assertTrue(corpo.contains("\"content\""));
        assertTrue(corpo.contains("\"totalElements\""));
    }

    @Test
    void alertasTambemExigemAutenticacao() throws Exception {
        int status = mvc.perform(get("/api/estoque/alertas-validade")).andReturn().getResponse().getStatus();
        assertEquals(401, status);
    }

    @Test
    void precosAtuaisRetornamListaPaginada() throws Exception {
        var resposta = mvc.perform(get("/api/produtos/precos-atuais?page=0&size=5")
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "senha-de-teste")))
                .andReturn().getResponse();

        assertEquals(200, resposta.getStatus());
        assertTrue(resposta.getContentAsString().contains("\"content\""));
    }

    @Test
    void precoAtualDeProdutoInexistenteRetorna404() throws Exception {
        int status = mvc.perform(get("/api/produtos/987654321/preco-atual")
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "senha-de-teste")))
                .andReturn().getResponse().getStatus();

        assertEquals(404, status);
    }
}
