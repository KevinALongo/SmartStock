package com.smartstock.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Instala o {@link MapeamentoNomesFisicos} no Hibernate antes de a fábrica de entidades ser criada.
 * É isso que permite apontar o sistema para tabelas e colunas com outros nomes só pela configuração.
 */
@Component
public class MapeamentoBancoPostProcessor implements BeanPostProcessor, EnvironmentAware {

    private Environment ambiente;

    @Override
    public void setEnvironment(Environment environment) {
        this.ambiente = environment;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LocalContainerEntityManagerFactoryBean fabrica) {
            fabrica.getJpaPropertyMap().put("hibernate.physical_naming_strategy",
                    new MapeamentoNomesFisicos(chave -> ambiente.getProperty(chave)));
        }
        return bean;
    }
}
