package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarEstadoSincronizacionUseCaseTest {

    @Test
    void debeConsultarEstadoConSnapshotAplicado() {
        EstadoSincronizacionRepositoryPort repositorio =
                mock(EstadoSincronizacionRepositoryPort.class);

        Instant fecha = Instant.parse("2026-10-26T14:00:00Z");

        EstadoSincronizacion estado = new EstadoSincronizacion(
                1L,
                fecha,
                fecha,
                "SUCCESS",
                "Snapshot aplicado correctamente"
        );

        when(repositorio.obtenerEstado()).thenReturn(estado);

        ConsultarEstadoSincronizacionUseCase casoDeUso =
                new ConsultarEstadoSincronizacionUseCase(repositorio);

        EstadoSincronizacion resultado = casoDeUso.consultarEstado();

        assertSame(estado, resultado);
        assertEquals(1L, resultado.getAppliedVersion());
        assertEquals("SUCCESS", resultado.getPhase());

        verify(repositorio).obtenerEstado();
    }

    @Test
    void debeConsultarEstadoInicialSinSnapshot() {
        EstadoSincronizacionRepositoryPort repositorio =
                mock(EstadoSincronizacionRepositoryPort.class);

        EstadoSincronizacion estadoInicial = new EstadoSincronizacion(
                null,
                null,
                null,
                "NOT_STARTED",
                "Sin sincronizaciones realizadas"
        );

        when(repositorio.obtenerEstado()).thenReturn(estadoInicial);

        ConsultarEstadoSincronizacionUseCase casoDeUso =
                new ConsultarEstadoSincronizacionUseCase(repositorio);

        EstadoSincronizacion resultado = casoDeUso.consultarEstado();

        assertNull(resultado.getAppliedVersion());
        assertNull(resultado.getAppliedAt());
        assertEquals("NOT_STARTED", resultado.getPhase());

        verify(repositorio).obtenerEstado();
    }
}
