package com.example.svccatalogo.catalogo.infrastructure.scheduling;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "catalogo.reconciliacion-periodica.enabled", havingValue = "true")
public class ReconciliacionCatalogoPeriodicaAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliacionCatalogoPeriodicaAdapter.class);
    private final ReconciliarCatalogoPort reconciliar;

    public ReconciliacionCatalogoPeriodicaAdapter(ReconciliarCatalogoPort reconciliar) {
        this.reconciliar = reconciliar;
    }

    @Scheduled(fixedDelayString = "${catalogo.reconciliacion-periodica.intervalo-ms:60000}",
            initialDelayString = "${catalogo.reconciliacion-periodica.demora-inicial-ms:60000}")
    public void reconciliar() {
        try {
            // El puerto decorado conserva diagnóstico y bloqueo compartido con arranque/Kafka.
            reconciliar.reconciliarCatalogo();
            LOG.info("Reconciliacion periodica del catalogo completada");
        } catch (RuntimeException error) {
            LOG.warn("Fallo la reconciliacion periodica ({}); se conserva el catalogo valido",
                    error.getClass().getSimpleName());
        }
    }
}
