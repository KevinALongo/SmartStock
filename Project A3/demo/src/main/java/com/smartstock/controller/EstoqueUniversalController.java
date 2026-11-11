package com.smartstock.controller;

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
@RequestMapping("/api/universal/estoque")
public class EstoqueUniversalController {

    @Autowired
    private EstoqueUniversalRepository repository;

    @GetMapping("/alertas-validade")
    public ResponseEntity<List>> obterAlertasValidadeUniversal() {
        List<Lote> lotesCriticos = repository.buscarLotesProximosDoVencimentoUniversal();

        List> resposta = lotesCriticos.stream().map(lote -> {
            Map<String, Object> alerta = new HashMap<>();
            alerta.put("produto", lote.getProduto().getNome());
            alerta.put("categoria", lote.getProduto().getCategoria());
            alerta.put("codigoBarras", lote.getProduto().getCodigoBarras());
            alerta.put("quantidadeRestante", lote.getQuantidadeAtual());
            alerta.put("dataValidade", lote.getDataValida().toString());
            alerta.put("precoOriginal", lote.getProduto().getPrecoOriginal());
            alerta.put("precoComDescontoAutomatico", lote.calcularPrecoDinamico());
            alerta.put("status", "ALERTA: Item perecível próximo ao vencimento. Desconto aplicado automaticamente.");
            return alerta;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/alertas-reposicao")
    public ResponseEntity<List>> obterAlertasReposicaoUniversal() {
        List<Object[]> resultados = repository.verificarReposicaoUniversal();

        List> resposta = resultados.stream().map(linha -> {
            Map<String, Object> alerta = new HashMap<>();
            alerta.put("produto", linha[0]);
            alerta.put("estoqueAtual", linha[1]);
            alerta.put("estoqueMinimoPermitido", linha[2]);
            alerta.put("categoria", linha[3]);
            alerta.put("status", "URGENTE: Estoque crítico abaixo do mínimo. Necessária reposição imediata.");
            return alerta;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resposta);
    }
}