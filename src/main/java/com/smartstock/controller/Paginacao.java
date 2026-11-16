package com.smartstock.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Monta o Pageable com limites seguros: página mínima 0 e tamanho entre 1 e 100. */
final class Paginacao {

    private static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {}

    static Pageable de(int pagina, int tamanho) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO));
    }

    static Pageable de(int pagina, int tamanho, Sort ordem) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO), ordem);
    }
}
