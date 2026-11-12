package com.smartstock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Saída de estoque (venda). */
public record BaixaRequest(
        @NotNull(message = "é obrigatório") Long produtoId,
        @NotNull(message = "é obrigatório") @Positive(message = "deve ser maior que zero") Integer quantidade) {}
