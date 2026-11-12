package com.smartstock.controller;

import com.smartstock.dto.*;
import com.smartstock.model.Lote;
import com.smartstock.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/lotes")
    public List<Lote> lotes() {
        return service.listarLotes();
    }

    @PostMapping("/lotes")
    public ResponseEntity<Lote> entrada(@Valid @RequestBody LoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrada(req));
    }

    @PostMapping("/baixa")
    public BaixaResponse baixa(@Valid @RequestBody BaixaRequest req) {
        return service.registrarBaixa(req);
    }
}
