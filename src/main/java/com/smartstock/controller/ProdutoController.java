package com.smartstock.controller;

import com.smartstock.dto.PrecoAtual;
import com.smartstock.dto.ProdutoRequest;
import com.smartstock.model.Produto;
import com.smartstock.service.PrecoService;
import com.smartstock.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final PrecoService precos;

    public ProdutoController(ProdutoService service, PrecoService precos) {
        this.service = service;
        this.precos = precos;
    }

    /** Preço atual (com desconto de validade já aplicado) de todos os produtos, paginado e ordenado por nome. */
    @GetMapping("/precos-atuais")
    public PagedModel<PrecoAtual> precosAtuais(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(precos.listar(Paginacao.de(page, size, Sort.by("nome"))));
    }

    /** Quanto cobrar agora por este produto (para o caixa/PDV). */
    @GetMapping("/{id}/preco-atual")
    public PrecoAtual precoAtual(@PathVariable Long id) {
        return precos.porProduto(id);
    }

    /** O mesmo, buscando pelo código de barras lido no caixa. */
    @GetMapping("/codigo/{codigoBarras}/preco-atual")
    public PrecoAtual precoAtualPorCodigo(@PathVariable String codigoBarras) {
        return precos.porCodigoBarras(codigoBarras);
    }

    /** Lista paginada, ordenada por nome. Parâmetros: page (começa em 0) e size (máx. 100). */
    @GetMapping
    public PagedModel<Produto> listar(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return new PagedModel<>(service.listar(Paginacao.de(page, size, Sort.by("nome"))));
    }

    @GetMapping("/{id}")
    public Produto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@Valid @RequestBody ProdutoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }

    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
