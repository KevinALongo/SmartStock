package com.smartstock.notificacao;

import com.smartstock.dto.AlertaReposicao;
import com.smartstock.dto.AlertaValidade;
import com.smartstock.service.EstoqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Junta os alertas de validade e de reposição e os envia pelo canal configurado. */
@Component
public class AvisoDiario {

    private static final Logger log = LoggerFactory.getLogger(AvisoDiario.class);
    private static final int LIMITE_LINHAS = 100;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EstoqueService estoque;
    private final Notificador notificador;

    public AvisoDiario(EstoqueService estoque, Notificador notificador) {
        this.estoque = estoque;
        this.notificador = notificador;
    }

    @Scheduled(cron = "${smartstock.notificacao.cron:0 0 8 * * *}", zone = "${smartstock.fuso:America/Sao_Paulo}")
    public void agendado() {
        enviar();
    }

    /** Envia o aviso agora. Retorna o total de alertas (0 = nada a avisar, nada foi enviado). */
    public int enviar() {
        List<AlertaValidade> validade = estoque.todosAlertasValidade();
        List<AlertaReposicao> reposicao = estoque.todosAlertasReposicao();
        int total = validade.size() + reposicao.size();
        if (total == 0) {
            log.info("Nenhum alerta de estoque hoje; nada a enviar.");
            return 0;
        }
        notificador.notificar("SmartStock: " + total + " alerta(s) de estoque", montarMensagem(validade, reposicao));
        return total;
    }

    String montarMensagem(List<AlertaValidade> validade, List<AlertaReposicao> reposicao) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        StringBuilder sb = new StringBuilder();

        if (!validade.isEmpty()) {
            sb.append("PRODUTOS PERTO DE VENCER (").append(validade.size()).append(")\n");
            validade.stream().limit(LIMITE_LINHAS).forEach(a -> sb
                    .append("- ").append(a.produto())
                    .append(" | vence em ").append(a.dataValidade().format(DATA))
                    .append(" | ").append(a.quantidade()).append(" un.")
                    .append(" | ").append(a.status())
                    .append(" | preço sugerido ").append(moeda.format(a.precoComDesconto()))
                    .append('\n'));
            maisLinhas(sb, validade.size());
            sb.append('\n');
        }

        if (!reposicao.isEmpty()) {
            sb.append("REPOSIÇÃO NECESSÁRIA (").append(reposicao.size()).append(")\n");
            reposicao.stream().limit(LIMITE_LINHAS).forEach(a -> sb
                    .append("- ").append(a.produto())
                    .append(" | estoque ").append(a.estoqueAtual())
                    .append(" | mínimo ").append(a.estoqueMinimo())
                    .append('\n'));
            maisLinhas(sb, reposicao.size());
        }
        return sb.toString();
    }

    private void maisLinhas(StringBuilder sb, int total) {
        if (total > LIMITE_LINHAS) {
            sb.append("... e mais ").append(total - LIMITE_LINHAS).append(" item(ns).\n");
        }
    }
}
