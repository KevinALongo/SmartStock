package com.smartstock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra única de desconto por proximidade da validade.
 * <ul>
 *   <li>até 2 dias: 50% de desconto</li>
 *   <li>3 a 5 dias: 30%</li>
 *   <li>6 a 10 dias: 10%</li>
 *   <li>mais de 10 dias, ou já vencido: sem desconto (vencido deve sair de venda)</li>
 * </ul>
 */
public final class PoliticaDesconto {

    private PoliticaDesconto() {}

    public static BigDecimal fator(long diasParaVencer) {
        if (diasParaVencer < 0) return BigDecimal.ONE;
        if (diasParaVencer <= 2) return new BigDecimal("0.50");
        if (diasParaVencer <= 5) return new BigDecimal("0.70");
        if (diasParaVencer <= 10) return new BigDecimal("0.90");
        return BigDecimal.ONE;
    }

    public static BigDecimal precoComDesconto(BigDecimal precoOriginal, long diasParaVencer) {
        return precoOriginal.multiply(fator(diasParaVencer)).setScale(2, RoundingMode.HALF_UP);
    }

    public static int percentualDesconto(long diasParaVencer) {
        return BigDecimal.ONE.subtract(fator(diasParaVencer)).movePointRight(2).intValue();
    }
}
