package com.example.svccatalogo.catalogo.infrastructure.startup;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "catalogo.reconciliacion-inicio.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliacionCatalogoInicioConfiguration {

    @Bean
    public ThreadPoolTaskExecutor reconciliacionCatalogoExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("catalogo-reconciliacion-");
        executor.setDaemon(true);
        return executor;
    }

    @Bean
    public ReconciliacionCatalogoInicioAdapter reconciliacionCatalogoInicioAdapter(
            ReconciliarCatalogoPort reconciliarPort,
            @Qualifier("reconciliacionCatalogoExecutor") ThreadPoolTaskExecutor executor) {
        return new ReconciliacionCatalogoInicioAdapter(reconciliarPort, executor);
    }
}
