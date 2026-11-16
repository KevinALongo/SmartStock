package com.smartstock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Parte de uma baixa retirada de um lote, com o desconto de validade já aplicado ao preço unitário. */
public record ItemBaixa(Long loteId, int quantidade, LocalDate dataValidade,
                        int percentualDesconto, BigDecimal precoUnitario) {}
