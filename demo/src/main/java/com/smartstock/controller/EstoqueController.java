package com.smartstock.controller;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.dto.AlertaValidade;
import com.smartstock.model.Lote;
import com.smartstock.repository.EstoqueUniversalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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