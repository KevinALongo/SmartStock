package com.smartstock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Regra única de desconto por proximidade da validade, configurável em
 * {@code smartstock.desconto.faixas} (variável de ambiente {@code DESCONTO_FAIXAS}).
 *
 * <p>Formato: lista de {@code diasAte:percentual}, em ordem crescente de dias.
 * Padrão {@value #FAIXAS_PADRAO}:
 * <ul>
 *   <li>até 2 dias: 50% de desconto</li>
 *   <li>3 a 5 dias: 30%</li>
 *   <li>6 a 10 dias: 10%</li>
 *   <li>mais que a última faixa, sem validade, ou já vencido: sem desconto
 *       (vencido deve sair de venda)</li>
 * </ul>
 * Use {@code nenhuma} para desligar o desconto.
 */
@Component
public class PoliticaDesconto {

    public static final String FAIXAS_PADRAO = "2:50,5:30,10:10";
    private static final String SEM_DESCONTO = "nenhuma";

    private record Faixa(long ateDias, int percentual) {}

    private final List<Faixa> faixas;

    public PoliticaDesconto(@Value("${smartstock.desconto.faixas:" + FAIXAS_PADRAO + "}") String configuracao) {
        String texto = (configuracao == null || configuracao.isBlank()) ? FAIXAS_PADRAO : configuracao;
        this.faixas = interpretar(texto);
    }

    /** Política com as faixas padrão (útil em testes). */
    public static PoliticaDesconto padrao() {
        return new PoliticaDesconto(FAIXAS_PADRAO);
    }

    private static List<Faixa> interpretar(String texto) {
        String limpo = texto.trim();
        if (limpo.equalsIgnoreCase(SEM_DESCONTO)) {
            return List.of();
        }
        List<Faixa> lista = new ArrayList<>();
        long anterior = -1;
        for (String parte : limpo.split(",")) {
            String[] par = parte.trim().split(":");
            if (par.length != 2) {
                throw erro(texto, "use o formato dias:percentual, por exemplo 2:50");
            }
            long dias;
            int percentual;
            try {
                dias = Long.parseLong(par[0].trim());
                percentual = Integer.parseInt(par[1].trim());
            } catch (NumberFormatException e) {
                throw erro(texto, "dias e percentual devem ser números inteiros");
            }
            if (dias < 0) {
                throw erro(texto, "os dias não podem ser negativos");
            }
            if (percentual < 0 || percentual > 100) {
                throw erro(texto, "o percentual deve ficar entre 0 e 100");
            }
            if (dias <= anterior) {
                throw erro(texto, "os dias devem estar em ordem crescente, sem repetir");
            }
            lista.add(new Faixa(dias, percentual));
            anterior = dias;
        }
        return List.copyOf(lista);
    }

    private static IllegalStateException erro(String texto, String motivo) {
        return new IllegalStateException("smartstock.desconto.faixas inválido (\"" + texto + "\"): " + motivo
                + ". Exemplo válido: " + FAIXAS_PADRAO + " (ou \"" + SEM_DESCONTO + "\").");
    }

    /** Percentual de desconto (0 a 100) para um lote que vence em {@code diasParaVencer} dias. */
    public int percentualDesconto(long diasParaVencer) {
        if (diasParaVencer < 0) {
            return 0;
        }
        for (Faixa faixa : faixas) {
            if (diasParaVencer <= faixa.ateDias()) {
                return faixa.percentual();
            }
        }
        return 0;
    }

    /** Fator multiplicador do preço (ex.: 0.50 para 50% de desconto). */
    public BigDecimal fator(long diasParaVencer) {
        return BigDecimal.valueOf(100L - percentualDesconto(diasParaVencer)).movePointLeft(2);
    }

    public BigDecimal precoComDesconto(BigDecimal precoOriginal, long diasParaVencer) {
        return precoOriginal.multiply(fator(diasParaVencer)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Maior quantidade de dias que ainda recebe faixa de desconto (0 se não há faixas). */
    public long maiorFaixaDias() {
        return faixas.isEmpty() ? 0 : faixas.get(faixas.size() - 1).ateDias();
    }
}
