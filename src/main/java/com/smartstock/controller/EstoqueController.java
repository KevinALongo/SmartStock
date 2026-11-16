package com.smartstock.controller;

import com.smartstock.dto.*;
import com.smartstock.model.Lote;
import com.smartstock.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping("/alertas-validade")
    public PagedModel<AlertaValidade> validade(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.alertasValidade(Paginacao.de(page, size)));
    }

    @GetMapping("/alertas-reposicao")
    public PagedModel<AlertaReposicao> reposicao(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.alertasReposicao(Paginacao.de(page, size)));
    }

    @GetMapping("/lotes")
    public PagedModel<Lote> lotes(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.listarLotes(Paginacao.de(page, size, Sort.by("id"))));
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
