package com.smartstock.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Traduz os nomes "lógicos" usados nas entidades para os nomes reais do banco do cliente,
 * lendo {@code smartstock.banco.nomes.<nome-logico>} da configuração.
 *
 * <p>Nomes lógicos: tabelas {@code produtos} e {@code lotes}; colunas {@code pk_produto},
 * {@code codigo_barras}, {@code nome}, {@code categoria}, {@code preco_original},
 * {@code estoque_minimo}, {@code pk_lote}, {@code produto_id}, {@code quantidade_atual},
 * {@code data_validade}. Sem configuração, o nome físico é igual ao lógico
 * (exceto {@code pk_produto} e {@code pk_lote}, que viram {@code id}).
 *
 * <p>Para forçar aspas no identificador, escreva o valor entre aspas duplas ou crases.
 */
public class MapeamentoNomesFisicos implements PhysicalNamingStrategy {

    public static final String PREFIXO = "smartstock.banco.nomes.";

    private static final Map<String, String> PADROES = Map.of(
            "pk_produto", "id",
            "pk_lote", "id");

    private final UnaryOperator<String> propriedades;

    /** @param propriedades devolve o valor configurado para uma chave, ou null se não existir */
    public MapeamentoNomesFisicos(UnaryOperator<String> propriedades) {
        this.propriedades = propriedades;
    }

    private Identifier mapear(Identifier logico) {
        if (logico == null) {
            return null;
        }
        String nome = logico.getText();
        String configurado = propriedades.apply(PREFIXO + nome);
        String fisico = (configurado != null && !configurado.isBlank()) ? configurado.trim() : PADROES.get(nome);
        if (fisico == null) {
            return logico;
        }
        boolean comAspas = logico.isQuoted();
        if (fisico.length() >= 2
                && ((fisico.startsWith("\"") && fisico.endsWith("\""))
                || (fisico.startsWith("`") && fisico.endsWith("`")))) {
            comAspas = true;
            fisico = fisico.substring(1, fisico.length() - 1);
        }
        return Identifier.toIdentifier(fisico, comAspas);
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return mapear(logicalName);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return mapear(logicalName);
    }
}
