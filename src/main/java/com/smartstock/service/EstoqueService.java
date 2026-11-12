package com.smartstock.service;

import com.smartstock.dto.*;
import com.smartstock.exception.RecursoNaoEncontradoException;
import com.smartstock.exception.RegraDeNegocioException;
import com.smartstock.model.Lote;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import com.smartstock.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EstoqueService {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);

    private final EstoqueRepository repository;
    private final ProdutoRepository produtos;

    @Value("${smartstock.dias-alerta:10}")
    private int diasAlerta;

    public EstoqueService(EstoqueRepository repository, ProdutoRepository produtos) {
        this.repository = repository;
        this.produtos = produtos;
    }

    // ---------- Alertas ----------

    @Transactional(readOnly = true)
    public List<AlertaValidade> alertasValidade() {
        LocalDate hoje = LocalDate.now();
        return repository.buscarLotesAteData(hoje.plusDays(diasAlerta)).stream().map(l -> {
            long dias = ChronoUnit.DAYS.between(hoje, l.getDataValidade());
            BigDecimal original = l.getProduto().getPrecoOriginal();
            BigDecimal comDesconto = PoliticaDesconto.precoComDesconto(original, dias);
            int percentual = PoliticaDesconto.percentualDesconto(dias);

            String status;
            if (dias < 0) {
                status = "VENCIDO: retirar da venda";
            } else if (percentual > 0) {
                status = "ATENÇÃO: vence em " + dias + " dia(s). Desconto de " + percentual + "% aplicado.";
            } else {
                status = "ATENÇÃO: vence em " + dias + " dia(s). Sem desconto.";
            }

            return new AlertaValidade(l.getProduto().getNome(), l.getProduto().getCategoria(),
                    l.getProduto().getCodigoBarras(), l.getQuantidadeAtual(), l.getDataValidade(),
                    dias, original, comDesconto, status);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaReposicao> alertasReposicao() {
        return repository.buscarProdutosAbaixoDoMinimo(LocalDate.now());
    }

    // Aviso automático: todo dia às 8h (horário de Brasília)
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    public void verificarEAvisar() {
        alertasValidade().forEach(a -> log.warn("VALIDADE: {} - {}", a.produto(), a.status()));
        alertasReposicao().forEach(a -> log.warn("REPOSIÇÃO: {} (atual {}, mínimo {})",
                a.produto(), a.estoqueAtual(), a.estoqueMinimo()));
        // aqui entra o envio de e-mail/WhatsApp/webhook para o gestor
    }

    // ---------- Movimentação ----------

    @Transactional(readOnly = true)
    public List<Lote> listarLotes() {
        return repository.findAll();
    }

    @Transactional
    public Lote registrarEntrada(LoteRequest req) {
        Produto produto = produtos.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + req.produtoId()));
        if (req.dataValidade() != null && req.dataValidade().isBefore(LocalDate.now())) {
            throw new RegraDeNegocioException("Não é possível dar entrada em um lote já vencido.");
        }
        Lote lote = new Lote();
        lote.setProduto(produto);
        lote.setQuantidadeAtual(req.quantidade());
        lote.setDataValidade(req.dataValidade());
        return repository.save(lote);
    }

    /** Saída (venda): retira primeiro do lote que vence antes (FEFO) e ignora lotes vencidos. */
    @Transactional
    public BaixaResponse registrarBaixa(BaixaRequest req) {
        Produto produto = produtos.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + req.produtoId()));

        List<Lote> lotes = repository.buscarLotesDisponiveis(produto.getId(), LocalDate.now());
        long disponivel = lotes.stream().mapToLong(Lote::getQuantidadeAtual).sum();
        if (disponivel < req.quantidade()) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente: disponível " + disponivel + ", solicitado " + req.quantidade() + ".");
        }

        int restante = req.quantidade();
        for (Lote lote : lotes) {
            if (restante == 0) break;
            int retirar = Math.min(restante, lote.getQuantidadeAtual());
            lote.setQuantidadeAtual(lote.getQuantidadeAtual() - retirar);
            restante -= retirar;
        }
        repository.saveAll(lotes);

        return new BaixaResponse(produto.getId(), produto.getNome(), req.quantidade(),
                disponivel - req.quantidade());
    }
}
