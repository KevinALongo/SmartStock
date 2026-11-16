package com.smartstock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Toda a API exige autenticação HTTP Basic. Sem sessão e sem CSRF (API stateless).
 * Use HTTPS em produção: no Basic, usuário e senha trafegam em Base64, não criptografados.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private static final int TAMANHO_MINIMO_SENHA = 8;

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(@Value("${smartstock.seguranca.usuario}") String usuario,
                                @Value("${smartstock.seguranca.senha}") String senha,
                                PasswordEncoder encoder) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                    "Defina a variável APP_SENHA com pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        UserDetails admin = User.withUsername(usuario)
                .password(encoder.encode(senha))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
