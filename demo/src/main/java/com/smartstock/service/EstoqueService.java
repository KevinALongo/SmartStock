package com.smartstock.service;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.dto.AlertaValidade;
import com.smartstock.repository.EstoqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EstoqueService {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);
    private final EstoqueRepository repository;

    @Value("${smartstock.dias-alerta:10}")
    private int diasAlerta;

    public EstoqueService(EstoqueRepository repository) {
        this.repository = repository;
    }

    public List<AlertaValidade> alertasValidade() {
        LocalDate hoje = LocalDate.now();
        return repository.buscarLotesAteData(hoje.plusDays(diasAlerta)).stream().map(l -> {
            long dias = ChronoUnit.DAYS.between(hoje, l.getDataValidade());
            BigDecimal original = l.getProduto().getPrecoOriginal();
            String status = dias < 0 ? "VENCIDO: retirar da venda"
                    : "ATENÇÃO: vence em " + dias + " dia(s). Desconto aplicado.";
            return new AlertaValidade(l.getProduto().getNome(), l.getProduto().getCategoria(),
                    l.getProduto().getCodigoBarras(), l.getQuantidadeAtual(), l.getDataValidade(),
                    dias, original, dias < 0 ? original : precoComDesconto(original, dias), status);
        }).toList();
    }

    public List<AlertaReposicao> alertasReposicao() {
        return repository.buscarProdutosAbaixoDoMinimo();
    }

    private BigDecimal precoComDesconto(BigDecimal preco, long dias) {
        BigDecimal fator = dias <= 2 ? new BigDecimal("0.50")
                : dias <= 5 ? new BigDecimal("0.70")
                : dias <= 10 ? new BigDecimal("0.90")
                : BigDecimal.ONE;
        return preco.multiply(fator).setScale(2, RoundingMode.HALF_UP);
    }

    // Aviso automático: roda todo dia às 8h (precisa de @EnableScheduling na classe principal)
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarEAvisar() {
        alertasValidade().forEach(a -> log.warn("VALIDADE: {} - {}", a.produto(), a.status()));
        alertasReposicao().forEach(a -> log.warn("REPOSIÇÃO: {} (atual {}, mínimo {})",
                a.produto(), a.estoqueAtual(), a.estoqueMinimo()));
        // aqui entra o envio de e-mail/WhatsApp/webhook para o gestor
    }
}
