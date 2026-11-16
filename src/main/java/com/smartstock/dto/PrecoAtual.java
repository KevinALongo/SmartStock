package com.smartstock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Preço que deve ser cobrado AGORA por um produto: é o preço da próxima unidade a ser vendida,
 * ou seja, do lote disponível que vence primeiro (mesma regra FEFO usada na baixa).
 * Sem estoque disponível, devolve o preço de tabela. Produtos sem validade nunca têm desconto.
 */
public record PrecoAtual(Long produtoId, String codigoBarras, String produto,
                         BigDecimal precoOriginal, BigDecimal precoAtual, int percentualDesconto,
                         LocalDate dataValidadeProximoLote, Long diasParaVencer,
                         long estoqueDisponivel, String status) {}
