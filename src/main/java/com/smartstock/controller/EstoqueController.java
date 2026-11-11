package com.smartstock.controller;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.dto.AlertaValidade;
import com.smartstock.service.EstoqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping("/alertas-validade")
    public ResponseEntity<List<AlertaValidade>> validade() {
        return ResponseEntity.ok(service.alertasValidade());
    }

    @GetMapping("/alertas-reposicao")
    public ResponseEntity<List<AlertaReposicao>> reposicao() {
        return ResponseEntity.ok(service.alertasReposicao());
    }
}