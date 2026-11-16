package com.smartstock.service;

import com.smartstock.dto.*;
import com.smartstock.exception.RecursoNaoEncontradoException;
import com.smartstock.exception.RegraDeNegocioException;
import com.smartstock.model.Lote;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import com.smartstock.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstoqueService {

    private static final int TAMANHO_LOTE_INTERNO = 500;

    private final EstoqueRepository repository;
    private final ProdutoRepository produtos;
    private final PoliticaDesconto politica;
    private final Clock relogio;

    @Value("${smartstock.dias-alerta:10}")
    private int diasAlerta;

    public EstoqueService(EstoqueRepository repository, ProdutoRepository produtos,
                          PoliticaDesconto politica, Clock relogio) {
        this.repository = repository;
        this.produtos = produtos;
        this.politica = politica;
        this.relogio = relogio;
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    /**
     * Janela dos alertas de validade: nunca menor que a maior faixa de desconto, para que todo lote
     * que já recebe desconto apareça no alerta (mesmo que dias-alerta tenha sido configurado menor).
     */
    private long janelaAlerta() {
        return Math.max(diasAlerta, politica.maiorFaixaDias());
    }

    // ---------- Alertas ----------

    @Transactional(readOnly = true)
    public Page<AlertaValidade> alertasValidade(Pageable pageable) {
        LocalDate hoje = hoje();
        return repository.buscarLotesAteData(hoje.plusDays(janelaAlerta()), pageable)
                .map(lote -> paraAlerta(lote, hoje));
    }

    @Transactional(readOnly = true)
    public Page<AlertaReposicao> alertasReposicao(Pageable pageable) {
        return repository.buscarProdutosAbaixoDoMinimo(hoje(), pageable);
    }

    /** Todos os alertas de validade, sem paginação (uso interno: aviso diário). */
    @Transactional(readOnly = true)
    public List<AlertaValidade> todosAlertasValidade() {
        List<AlertaValidade> todos = new ArrayList<>();
        Pageable pagina = PageRequest.of(0, TAMANHO_LOTE_INTERNO);
        Page<AlertaValidade> atual;
        do {
            atual = alertasValidade(pagina);
            todos.addAll(atual.getContent());
            pagina = pagina.next();
        } while (atual.hasNext());
        return todos;
    }

    /** Todos os alertas de reposição, sem paginação (uso interno: aviso diário). */
    @Transactional(readOnly = true)
    public List<AlertaReposicao> todosAlertasReposicao() {
        List<AlertaReposicao> todos = new ArrayList<>();
        Pageable pagina = PageRequest.of(0, TAMANHO_LOTE_INTERNO);
        Page<AlertaReposicao> atual;
        do {
            atual = alertasReposicao(pagina);
            todos.addAll(atual.getContent());
            pagina = pagina.next();
        } while (atual.hasNext());
        return todos;
    }

    private AlertaValidade paraAlerta(Lote lote, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, lote.getDataValidade());
        BigDecimal original = lote.getProduto().getPrecoOriginal();
        BigDecimal comDesconto = politica.precoComDesconto(original, dias);
        int percentual = politica.percentualDesconto(dias);

        String status;
        if (dias < 0) {
            status = "VENCIDO: retirar da venda";
        } else if (percentual > 0) {
            status = "ATENÇÃO: vence em " + dias + " dia(s). Desconto de " + percentual + "% aplicado.";
        } else {
            status = "ATENÇÃO: vence em " + dias + " dia(s). Sem desconto.";
        }

        return new AlertaValidade(lote.getProduto().getNome(), lote.getProduto().getCategoria(),
                lote.getProduto().getCodigoBarras(), lote.getQuantidadeAtual(), lote.getDataValidade(),
                dias, original, comDesconto, status);
    }

    // ---------- Movimentação ----------

    @Transactional(readOnly = true)
    public Page<Lote> listarLotes(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public Lote registrarEntrada(LoteRequest req) {
        Produto produto = produtos.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + req.produtoId()));
        if (req.dataValidade() != null && req.dataValidade().isBefore(hoje())) {
            throw new RegraDeNegocioException("Não é possível dar entrada em um lote já vencido.");
        }
        Lote lote = new Lote();
        lote.setProduto(produto);
        lote.setQuantidadeAtual(req.quantidade());
        lote.setDataValidade(req.dataValidade());
        return repository.save(lote);
    }

    /**
     * Saída (venda): retira primeiro do lote que vence antes (FEFO) e ignora lotes vencidos.
     * O desconto de validade é aplicado a cada parte retirada, e o valor total já vem calculado.
     */
    @Transactional
    public BaixaResponse registrarBaixa(BaixaRequest req) {
        Produto produto = produtos.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + req.produtoId()));

        LocalDate hoje = hoje();
        List<Lote> lotes = repository.buscarLotesDisponiveis(produto.getId(), hoje);
        long disponivel = lotes.stream().mapToLong(Lote::getQuantidadeAtual).sum();
        if (disponivel < req.quantidade()) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente: disponível " + disponivel + ", solicitado " + req.quantidade() + ".");
        }

        List<ItemBaixa> itens = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;
        int restante = req.quantidade();
        for (Lote lote : lotes) {
            if (restante == 0) break;
            int retirar = Math.min(restante, lote.getQuantidadeAtual());
            lote.setQuantidadeAtual(lote.getQuantidadeAtual() - retirar);
            restante -= retirar;

            // lote sem validade nunca recebe desconto
            long dias = lote.getDataValidade() == null
                    ? Long.MAX_VALUE
                    : ChronoUnit.DAYS.between(hoje, lote.getDataValidade());
            BigDecimal unitario = politica.precoComDesconto(produto.getPrecoOriginal(), dias);
            itens.add(new ItemBaixa(lote.getId(), retirar, lote.getDataValidade(),
                    politica.percentualDesconto(dias), unitario));
            valorTotal = valorTotal.add(unitario.multiply(BigDecimal.valueOf(retirar)));
        }
        repository.saveAll(lotes);

        return new BaixaResponse(produto.getId(), produto.getNome(), req.quantidade(),
                disponivel - req.quantidade(), valorTotal.setScale(2, java.math.RoundingMode.HALF_UP), itens);
    }
}
