package com.example.svccatalogo.catalogo.infrastructure.startup;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconciliacionCatalogoInicioAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliacionCatalogoInicioAdapter.class);

    private final ReconciliarCatalogoPort reconciliarPort;
    private final Executor executor;
    private final AtomicBoolean iniciada = new AtomicBoolean();

    public ReconciliacionCatalogoInicioAdapter(ReconciliarCatalogoPort reconciliarPort, Executor executor) {
        this.reconciliarPort = reconciliarPort;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (iniciada.compareAndSet(false, true)) {
            executor.execute(this::reconciliar);
        }
    }

    private void reconciliar() {
        try {
            reconciliarPort.reconciliarCatalogo();
            LOGGER.info("Reconciliacion inicial del catalogo completada");
        } catch (RuntimeException exception) {
            LOGGER.error("Fallo la reconciliacion inicial del catalogo ({}). "
                            + "Se conserva la copia local; su vigencia no pudo confirmarse",
                    exception.getClass().getSimpleName());
        }
    }
}
