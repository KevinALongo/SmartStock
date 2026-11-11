package com.smartstock.dto;


public record AlertaReposicao(String produto, Long estoqueAtual,
                              Integer estoqueMinimo, String categoria) {}