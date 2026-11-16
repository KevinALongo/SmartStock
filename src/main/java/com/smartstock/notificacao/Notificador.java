package com.smartstock.notificacao;

/** Canal de envio dos alertas. Para um novo canal (ex.: WhatsApp), basta criar outra implementação. */
public interface Notificador {

    void notificar(String assunto, String mensagem);
}
