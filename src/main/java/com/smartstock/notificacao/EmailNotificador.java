package com.smartstock.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Envia os alertas por e-mail (SMTP). Só é criado com smartstock.notificacao.email.ativo=true.
 * Exige SPRING_MAIL_HOST (e, normalmente, usuário e senha SMTP) e ao menos um destinatário.
 */
@Component
@ConditionalOnProperty(name = "smartstock.notificacao.email.ativo", havingValue = "true")
public class EmailNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificador.class);

    private final JavaMailSender mailSender;
    private final String remetente;
    private final String[] destinatarios;

    public EmailNotificador(JavaMailSender mailSender,
                            @Value("${smartstock.notificacao.email.de:}") String remetente,
                            @Value("${smartstock.notificacao.email.para:}") String[] destinatarios) {
        this.mailSender = mailSender;
        this.remetente = remetente == null ? "" : remetente.trim();
        this.destinatarios = Arrays.stream(destinatarios)
                .map(String::trim)
                .filter(d -> !d.isEmpty())
                .toArray(String[]::new);
        if (this.destinatarios.length == 0) {
            throw new IllegalStateException(
                    "E-mail ativado, mas nenhum destinatário foi definido. Defina EMAIL_DESTINATARIO "
                            + "(vários separados por vírgula).");
        }
    }

    @Override
    public void notificar(String assunto, String mensagem) {
        SimpleMailMessage email = new SimpleMailMessage();
        if (!remetente.isEmpty()) {
            email.setFrom(remetente);
        }
        email.setTo(destinatarios);
        email.setSubject(assunto);
        email.setText(mensagem);
        try {
            mailSender.send(email);
            log.info("Alerta enviado por e-mail para {} destinatário(s).", destinatarios.length);
        } catch (MailException e) {
            // Falha de envio não pode derrubar o agendamento; fica registrada no log.
            log.error("Falha ao enviar o e-mail de alerta: {}", e.getMessage());
        }
    }
}
