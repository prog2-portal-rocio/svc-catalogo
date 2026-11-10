package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReconciliarCatalogoUseCaseTest {

    private final EstadoSincronizacionRepositoryPort estados = mock(EstadoSincronizacionRepositoryPort.class);
    private final SincronizarCatalogoIncrementalPort sincronizacion = mock(SincronizarCatalogoIncrementalPort.class);
    private final CatalogoRedisPort redis = mock(CatalogoRedisPort.class);
    private final ReconciliarCatalogoUseCase useCase = new ReconciliarCatalogoUseCase(estados, sincronizacion, redis);

    @Test
    void bloqueaAntesDeSincronizarYCompruebaLaVersionAntesDeTerminar() {
        when(estados.obtenerEstado()).thenReturn(estado(7L));
        when(redis.obtenerVersionActual()).thenReturn(7L);

        useCase.reconciliarCatalogo();

        var orden = inOrder(estados, sincronizacion, redis);
        orden.verify(estados).obtenerEstadoConBloqueo();
        orden.verify(sincronizacion).sincronizarCatalogoIncremental();
        orden.verify(estados).obtenerEstado();
        orden.verify(redis).obtenerVersionActual();
        verifyNoMoreInteractions(estados, sincronizacion, redis);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {6, 8})
    void noConfirmaVersionesInconsistentes(Long version) {
        when(estados.obtenerEstado()).thenReturn(estado(version));
        when(redis.obtenerVersionActual()).thenReturn(7L);

        assertThrows(IllegalStateException.class, useCase::reconciliarCatalogo);
    }

    @Test
    void propagaElFalloSinReintentarNiGuardarUnEstadoExitoso() {
        doThrow(new IllegalStateException("Fallo de sincronizacion"))
                .when(sincronizacion).sincronizarCatalogoIncremental();

        assertThrows(IllegalStateException.class, useCase::reconciliarCatalogo);

        verify(sincronizacion).sincronizarCatalogoIncremental();
        verify(estados).obtenerEstadoConBloqueo();
        verifyNoMoreInteractions(estados, sincronizacion);
        verifyNoInteractions(redis);
    }

    private EstadoSincronizacion estado(Long version) {
        return new EstadoSincronizacion(version, null, null, "SUCCESS", null);
    }
}
