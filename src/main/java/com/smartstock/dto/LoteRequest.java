package com.smartstock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Entrada de estoque. dataValidade pode ser nula para produtos não perecíveis. */
public record LoteRequest(
        @NotNull(message = "é obrigatório") Long produtoId,
        @NotNull(message = "é obrigatório") @Positive(message = "deve ser maior que zero") Integer quantidade,
        LocalDate dataValidade) {}
