package com.smartstock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank(message = "é obrigatório") String codigoBarras,
        @NotBlank(message = "é obrigatório") String nome,
        @NotBlank(message = "é obrigatório") String categoria,
        @NotNull(message = "é obrigatório") @Positive(message = "deve ser maior que zero") BigDecimal precoOriginal,
        @NotNull(message = "é obrigatório") @PositiveOrZero(message = "não pode ser negativo") Integer estoqueMinimo) {}
