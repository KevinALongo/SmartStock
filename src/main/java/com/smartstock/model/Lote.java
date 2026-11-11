package com.smartstock.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@Entity
@Table(name = "lotes")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    public Double calcularPrecoDinamico() {
        if (this.dataValidade == null || this.produto == null || this.produto.getPrecoOriginal() == null) {
            return 0.0;
        }

        long diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), this.dataValidade);
        double precoOriginal = this.produto.getPrecoOriginal().doubleValue();

        if (diasParaVencer <= 2) {
            return precoOriginal * 0.50; // 50% de desconto (crítico)
        } else if (diasParaVencer <= 5) {
            return precoOriginal * 0.70; // 30% de desconto
        } else if (diasParaVencer <= 10) {
            return precoOriginal * 0.90; // 10% de desconto antecipado
        }
        return precoOriginal;
    }
}