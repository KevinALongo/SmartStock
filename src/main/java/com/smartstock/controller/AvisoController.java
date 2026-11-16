package com.smartstock.controller;

import com.smartstock.notificacao.AvisoDiario;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final AvisoDiario aviso;

    public AvisoController(AvisoDiario aviso) {
        this.aviso = aviso;
    }

    /** Dispara o aviso agora (útil para testar o envio de e-mail sem esperar as 8h). */
    @PostMapping("/enviar")
    public Map<String, Object> enviar() {
        int total = aviso.enviar();
        return Map.of("alertas", total, "enviado", total > 0);
    }
}
