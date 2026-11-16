package com.smartstock.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Usado quando o e-mail está desligado (padrão): escreve os alertas no log. */
@Component
@ConditionalOnProperty(name = "smartstock.notificacao.email.ativo", havingValue = "false", matchIfMissing = true)
public class LogNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

    @Override
    public void notificar(String assunto, String mensagem) {
        log.warn("{}\n{}", assunto, mensagem);
    }
}
