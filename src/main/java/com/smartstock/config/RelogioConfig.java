package com.smartstock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio único do sistema. Todas as regras de "hoje" (validade, desconto, aviso) usam este fuso,
 * e não o fuso do servidor: num servidor em UTC, depois das 21h em Brasília o "hoje" já seria amanhã.
 * Configure em {@code smartstock.fuso} (variável APP_FUSO). Padrão: America/Sao_Paulo.
 */
@Configuration
public class RelogioConfig {

    @Bean
    Clock relogio(@Value("${smartstock.fuso:America/Sao_Paulo}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
