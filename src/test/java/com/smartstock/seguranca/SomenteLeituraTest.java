package com.smartstock.seguranca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:somente_leitura;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "smartstock.somente-leitura=true"
})
class SomenteLeituraTest {

    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtrosDeSeguranca;

    private MockMvc mvc;

    @BeforeEach
    void montar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguranca).build();
    }

    private static String basic() {
        String credenciais = "admin:senha-de-teste";
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void consultasContinuamLiberadas() throws Exception {
        int status = mvc.perform(get("/api/estoque/alertas-validade").header(HttpHeaders.AUTHORIZATION, basic()))
                .andReturn().getResponse().getStatus();
        assertEquals(200, status);
    }

    @Test
    void cadastroEBaixaSaoBloqueados() throws Exception {
        int cadastro = mvc.perform(post("/api/produtos").header(HttpHeaders.AUTHORIZATION, basic())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        int baixa = mvc.perform(post("/api/estoque/baixa").header(HttpHeaders.AUTHORIZATION, basic())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertEquals(403, cadastro);
        assertEquals(403, baixa);
    }

    @Test
    void semCredenciaisContinuaSendo401() throws Exception {
        int status = mvc.perform(get("/api/produtos")).andReturn().getResponse().getStatus();
        assertEquals(401, status);
    }
}
