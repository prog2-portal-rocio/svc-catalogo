package com.example.svccatalogo.catalogo.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Evitar que la metadata automatica de OAuth exponga rutas fuera de la API.
        return configurarStateless(http)
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/profesionales",
                                "/api/catalogo/profesionales/{id}", "/api/catalogo/profesionales/{id}/horarios")
                        .hasAuthority("SCOPE_catalogo:read")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/sincronizacion/estado")
                        .hasAuthority("SCOPE_catalogo:sync:read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain denegarOtrasRutas(HttpSecurity http) {
        return configurarStateless(http)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .build();
    }

    private HttpSecurity configurarStateless(HttpSecurity http) {
        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
