package com.smartstock.service;

import com.smartstock.dto.PrecoAtual;
import com.smartstock.exception.RecursoNaoEncontradoException;
import com.smartstock.model.Lote;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import com.smartstock.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aplica o desconto de validade e responde "quanto cobrar agora por este produto?".
 * O preço é o do lote que será vendido primeiro (FEFO), exatamente como na baixa de estoque.
 */
@Service
public class PrecoService {

    private final ProdutoRepository produtos;
    private final EstoqueRepository lotes;
    private final PoliticaDesconto politica;
    private final Clock relogio;

    public PrecoService(ProdutoRepository produtos, EstoqueRepository lotes,
                        PoliticaDesconto politica, Clock relogio) {
        this.produtos = produtos;
        this.lotes = lotes;
        this.politica = politica;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public PrecoAtual porProduto(Long id) {
        Produto produto = produtos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
        LocalDate hoje = LocalDate.now(relogio);
        return montar(produto, lotes.consultarLotesDisponiveis(id, hoje), hoje);
    }

    @Transactional(readOnly = true)
    public PrecoAtual porCodigoBarras(String codigoBarras) {
        Produto produto = produtos.findByCodigoBarras(codigoBarras)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado com o código de barras: " + codigoBarras));
        LocalDate hoje = LocalDate.now(relogio);
        return montar(produto, lotes.consultarLotesDisponiveis(produto.getId(), hoje), hoje);
    }

    @Transactional(readOnly = true)
    public Page<PrecoAtual> listar(Pageable pageable) {
        Page<Produto> pagina = produtos.findAll(pageable);
        LocalDate hoje = LocalDate.now(relogio);
        List<Long> ids = pagina.getContent().stream().map(Produto::getId).toList();
        Map<Long, List<Lote>> lotesPorProduto = ids.isEmpty()
                ? Map.of()
                : lotes.consultarLotesDisponiveisDosProdutos(ids, hoje).stream()
                        .collect(Collectors.groupingBy(l -> l.getProduto().getId()));
        return pagina.map(p -> montar(p, lotesPorProduto.getOrDefault(p.getId(), List.of()), hoje));
    }

    /** @param disponiveis lotes não vencidos e com saldo, já em ordem FEFO */
    private PrecoAtual montar(Produto produto, List<Lote> disponiveis, LocalDate hoje) {
        BigDecimal original = produto.getPrecoOriginal();
        if (disponiveis.isEmpty()) {
            return new PrecoAtual(produto.getId(), produto.getCodigoBarras(), produto.getNome(),
                    original, original, 0, null, null, 0L, "SEM ESTOQUE DISPONÍVEL: preço de tabela");
        }

        long estoque = disponiveis.stream().mapToLong(Lote::getQuantidadeAtual).sum();
        LocalDate validade = disponiveis.get(0).getDataValidade();
        Long dias = validade == null ? null : ChronoUnit.DAYS.between(hoje, validade);
        long diasParaCalculo = dias == null ? Long.MAX_VALUE : dias;

        int percentual = politica.percentualDesconto(diasParaCalculo);
        BigDecimal atual = politica.precoComDesconto(original, diasParaCalculo);

        String status;
        if (dias == null) {
            status = "PREÇO NORMAL: produto sem validade";
        } else if (percentual > 0) {
            status = "DESCONTO DE " + percentual + "% APLICADO: vence em " + dias + " dia(s)";
        } else {
            status = "PREÇO NORMAL: vence em " + dias + " dia(s)";
        }

        return new PrecoAtual(produto.getId(), produto.getCodigoBarras(), produto.getNome(),
                original, atual, percentual, validade, dias, estoque, status);
    }
}
