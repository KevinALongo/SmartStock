package com.smartstock.dto;

public record BaixaResponse(Long produtoId, String produto, int quantidadeRetirada, long estoqueRestante) {}
