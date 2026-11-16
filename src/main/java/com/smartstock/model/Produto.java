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

    // Os nomes em @Table/@Column são LÓGICOS: o nome real no banco vem de smartstock.banco.nomes.*
    // (veja MapeamentoNomesFisicos e a seção "Usando um banco existente" do README).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_produto")
    private Long id;

    @Column(name = "codigo_barras", unique = true, nullable = false)
    private String codigoBarras;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "preco_original", nullable = false)
    private BigDecimal precoOriginal;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;
}
