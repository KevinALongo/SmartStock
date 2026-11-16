package com.smartstock.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de uma venda: o que saiu de cada lote e o valor total já com o desconto de validade. */
public record BaixaResponse(Long produtoId, String produto, int quantidadeRetirada, long estoqueRestante,
                            BigDecimal valorTotal, List<ItemBaixa> itens) {}
