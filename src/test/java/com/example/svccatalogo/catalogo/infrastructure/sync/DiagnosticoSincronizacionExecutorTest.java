package com.example.svccatalogo.catalogo.infrastructure.sync;

import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class DiagnosticoSincronizacionExecutorTest {

    private final EstadoSincronizacionRepositoryPort estados = mock(EstadoSincronizacionRepositoryPort.class);
    private final DiagnosticoSincronizacionExecutor executor = new DiagnosticoSincronizacionExecutor(estados);

    @Test
    void registraUnSoloExitoParaLaOperacionConFallbackAnidado() {
        executor.ejecutar(() -> executor.ejecutar(() -> {}, true), true);
        verify(estados).registrarResultadoIntento(any(), eq(true));
        verifyNoMoreInteractions(estados);
    }

    @Test
    void conservaLaExcepcionOriginalSiTambienFallaElDiagnostico() {
        var original = new IllegalStateException("mensaje sensible");
        doThrow(new IllegalStateException("detalle privado")).when(estados).registrarResultadoIntento(any(), eq(false));
        assertSame(original, assertThrows(IllegalStateException.class,
                () -> executor.ejecutar(() -> { throw original; }, true)));
    }

    @Test
    void unaNotificacionDescartadaNoBorraElErrorAnterior() {
        executor.ejecutar(() -> {}, false);
        verifyNoInteractions(estados);
    }

    @Test
    void registraElFalloDeUnaNotificacionSinRegistrarExitosDeSusLlamadasAnidadas() {
        var fallo = new IllegalStateException();
        assertThrows(IllegalStateException.class, () -> executor.ejecutar(() -> {
            executor.ejecutar(() -> {}, true);
            throw fallo;
        }, false));
        verify(estados).registrarResultadoIntento(any(), eq(false));
        verifyNoMoreInteractions(estados);
    }

    @Test
    void liberaElContextoDelHiloDespuesDeUnFallo() {
        assertThrows(IllegalStateException.class,
                () -> executor.ejecutar(() -> { throw new IllegalStateException(); }, true));
        executor.ejecutar(() -> {}, true);
        verify(estados).registrarResultadoIntento(any(), eq(false));
        verify(estados).registrarResultadoIntento(any(), eq(true));
    }

    @Test
    void unFalloAlGuardarElExitoNoSeConfundeConUnFalloDeLaSincronizacion(CapturedOutput output) {
        doThrow(new IllegalStateException("detalle privado de la conexion"))
                .when(estados).registrarResultadoIntento(any(), eq(true));
        var operacion = mock(Runnable.class);
        assertDoesNotThrow(() -> executor.ejecutar(operacion, true));
        verify(operacion).run();
        assertTrue(output.getOut().contains("No se pudo persistir el diagnostico de sincronizacion"));
        assertTrue(output.getOut().contains("IllegalStateException"));
        assertFalse(output.getAll().contains("detalle privado de la conexion"));
        verify(estados, never()).registrarResultadoIntento(any(), eq(false));
    }

    @Test
    void noEscribeHastaQueLaTransaccionExteriorConfirma() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            executor.ejecutar(() -> {}, true);
            verifyNoInteractions(estados);
            TransactionSynchronizationManager.getSynchronizations().getFirst()
                    .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            verify(estados).registrarResultadoIntento(any(), eq(true));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackPosteriorAlRetornoNoSePresentaComoExito() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            executor.ejecutar(() -> {}, true);
            TransactionSynchronizationManager.getSynchronizations().getFirst()
                    .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(estados).registrarResultadoIntento(any(), eq(false));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
