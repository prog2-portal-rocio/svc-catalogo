package com.example.svccatalogo.catalogo.infrastructure.sync;

import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/** Registra un solo resultado para la operacion exterior, incluidas sus llamadas anidadas. */
@Component
public class DiagnosticoSincronizacionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticoSincronizacionExecutor.class);
    private final EstadoSincronizacionRepositoryPort estados;
    private final ThreadLocal<Boolean> ejecutando = new ThreadLocal<>();

    public DiagnosticoSincronizacionExecutor(EstadoSincronizacionRepositoryPort estados) {
        this.estados = estados;
    }

    public void ejecutar(Runnable operacion, boolean registrarExito) {
        if (Boolean.TRUE.equals(ejecutando.get())) {
            operacion.run();
            return;
        }
        ejecutando.set(true);
        try {
            ejecutarExterior(operacion, registrarExito);
        } finally {
            ejecutando.remove();
        }
    }

    private void ejecutarExterior(Runnable operacion, boolean registrarExito) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            ejecutarEnTransaccionExistente(operacion, registrarExito);
            return;
        }
        try {
            operacion.run(); // El proxy transaccional del delegado termina antes de retornar.
        } catch (RuntimeException fallo) {
            registrar(false, registrarExito);
            throw fallo;
        }
        registrar(true, registrarExito);
    }

    private void ejecutarEnTransaccionExistente(Runnable operacion, boolean registrarExito) {
        var fallo = new AtomicBoolean();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                registrar(status == STATUS_COMMITTED && !fallo.get(), registrarExito);
            }
        });
        try {
            operacion.run();
        } catch (RuntimeException exception) {
            fallo.set(true);
            throw exception;
        }
    }

    private void registrar(boolean exitoso, boolean registrarExito) {
        if (exitoso && !registrarExito) {
            return;
        }
        try {
            estados.registrarResultadoIntento(Instant.now(), exitoso);
        } catch (RuntimeException falloDiagnostico) {
            // No ocultar la excepcion original ni volcar mensajes potencialmente sensibles.
            LOGGER.error("No se pudo persistir el diagnostico de sincronizacion ({})",
                    falloDiagnostico.getClass().getSimpleName());
        }
    }
}
