package com.smartstock.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras", unique = true, nullable = false)
    private String codigoBarras;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String categoria;

    @Column(name = "preco_original", nullable = false)
    private BigDecimal precoOriginal;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;
}
