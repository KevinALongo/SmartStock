package com.smartstock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlertaValidade(String produto, String categoria, String codigoBarras,
                             Integer quantidade, LocalDate dataValidade, long diasParaVencer,
                             BigDecimal precoOriginal, BigDecimal precoComDesconto,
                             String status) {}

