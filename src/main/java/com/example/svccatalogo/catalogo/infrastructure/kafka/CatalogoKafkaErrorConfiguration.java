package com.example.svccatalogo.catalogo.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class CatalogoKafkaErrorConfiguration {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            @Value("${catalogo.kafka.reintentos.intervalo-ms:2000}") long intervalo,
            @Value("${catalogo.kafka.reintentos.maximos:3}") long reintentos) {
        return new RecuperacionCatalogoKafkaErrorHandler(intervalo, reintentos);
    }
}
