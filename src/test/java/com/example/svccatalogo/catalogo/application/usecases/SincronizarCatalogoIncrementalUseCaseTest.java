package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.*;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SincronizarCatalogoIncrementalUseCaseTest {

    private EstadoSincronizacionRepositoryPort estadoRepository;
    private CatalogoRedisPort redisPort;
    private CatalogoIncrementalPersistencePort persistencePort;
    private SincronizarCatalogoPort sincronizacionCompleta;

    private SincronizarCatalogoIncrementalUseCase useCase;

    @BeforeEach
    void configurar() {
        estadoRepository = mock(EstadoSincronizacionRepositoryPort.class);
        redisPort = mock(CatalogoRedisPort.class);
        persistencePort = mock(CatalogoIncrementalPersistencePort.class);
        sincronizacionCompleta = mock(SincronizarCatalogoPort.class);

        useCase = new SincronizarCatalogoIncrementalUseCase(
                estadoRepository,
                redisPort,
                persistencePort,
                sincronizacionCompleta
        );
    }

    @Test
    void debeSincronizarSnapshotCuandoNoExisteVersionLocal() {
        configurarVersionLocal(null);

        useCase.sincronizarCatalogoIncremental();

        verify(sincronizacionCompleta).sincronizarCatalogo();
        verifyNoInteractions(redisPort, persistencePort);
    }

    @Test
    void noDebeModificarDatosCuandoEstaActualizado() {
        configurarVersionLocal(5L);
        when(redisPort.obtenerVersionActual()).thenReturn(5L);

        useCase.sincronizarCatalogoIncremental();

        verifyNoInteractions(persistencePort, sincronizacionCompleta);
        verify(redisPort, never()).obtenerCambios(anyLong());
    }

    @Test
    void debeAplicarUnaVersionIncremental() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual()).thenReturn(5L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        Categoria categoria = mock(Categoria.class);

        CambiosCatalogo cambios = crearCambios(
                5L,
                List.of(10L),
                List.of(),
                List.of()
        );

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.of(cambios));

        when(redisPort.obtenerCategoria(10L))
                .thenReturn(Optional.of(categoria));

        useCase.sincronizarCatalogoIncremental();

        verify(persistencePort).aplicarVersion(
                4L,
                5L,
                List.of(categoria),
                List.of(),
                List.of()
        );

        verifyNoInteractions(sincronizacionCompleta);
    }

    @Test
    void debeAplicarVariasVersionesEnOrden() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual()).thenReturn(6L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        CambiosCatalogo cambios5 = crearCambios(
                5L, List.of(), List.of(), List.of()
        );

        CambiosCatalogo cambios6 = crearCambios(
                6L, List.of(), List.of(), List.of()
        );

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.of(cambios5));

        when(redisPort.obtenerCambios(6L))
                .thenReturn(Optional.of(cambios6));

        useCase.sincronizarCatalogoIncremental();

        verify(redisPort).obtenerCambios(5L);
        verify(redisPort).obtenerCambios(6L);

        verify(persistencePort).aplicarVersion(
                4L, 6L,
                List.of(),
                List.of(),
                List.of()
        );

        verifyNoMoreInteractions(persistencePort);
        verifyNoInteractions(sincronizacionCompleta);
    }

    @Test
    void debeRecuperarSnapshotCuandoSePerdioElHistorial() {
        configurarVersionLocal(2L);

        when(redisPort.obtenerVersionActual()).thenReturn(7L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        useCase.sincronizarCatalogoIncremental();

        verify(sincronizacionCompleta).sincronizarCatalogo();
        verifyNoInteractions(persistencePort);
    }

    @Test
    void debeRecuperarSnapshotCuandoFaltanCambios() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual()).thenReturn(5L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.empty());

        useCase.sincronizarCatalogoIncremental();

        verify(sincronizacionCompleta).sincronizarCatalogo();
        verifyNoInteractions(persistencePort);
    }

    @Test
    void debeRecuperarSnapshotCuandoFaltaUnaEntidad() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual()).thenReturn(5L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        CambiosCatalogo cambios = crearCambios(
                5L,
                List.of(10L),
                List.of(),
                List.of()
        );

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.of(cambios));

        when(redisPort.obtenerCategoria(10L))
                .thenReturn(Optional.empty());

        useCase.sincronizarCatalogoIncremental();

        verify(sincronizacionCompleta).sincronizarCatalogo();
        verifyNoInteractions(persistencePort);
    }

    @Test
    void noDebeContinuarCuandoFallaLaPersistencia() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual()).thenReturn(5L);
        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        CambiosCatalogo cambios = crearCambios(
                5L,
                List.of(),
                List.of(),
                List.of()
        );

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.of(cambios));

        doThrow(new IllegalStateException("Error de persistencia"))
                .when(persistencePort)
                .aplicarVersion(
                        4L,
                        5L,
                        List.of(),
                        List.of(),
                        List.of()
                );

        assertThrows(
                IllegalStateException.class,
                () -> useCase.sincronizarCatalogoIncremental()
        );

        verifyNoInteractions(sincronizacionCompleta);
        verify(redisPort, never()).obtenerCambios(6L);
    }

    @Test
    void debeRechazarVersionLocalSuperiorALaRemota() {
        configurarVersionLocal(8L);

        when(redisPort.obtenerVersionActual()).thenReturn(7L);

        assertThrows(
                IllegalStateException.class,
                () -> useCase.sincronizarCatalogoIncremental()
        );

        verifyNoInteractions(persistencePort, sincronizacionCompleta);
    }

    private void configurarVersionLocal(Long version) {
        when(estadoRepository.obtenerEstado()).thenReturn(
                new EstadoSincronizacion(
                        version,
                        null,
                        null,
                        "SUCCESS",
                        null
                )
        );
    }

    private CambiosCatalogo crearCambios(
            long version,
            List<Long> categorias,
            List<Long> profesionales,
            List<Long> horarios) {

        CambiosCatalogo cambios = mock(CambiosCatalogo.class);

        when(cambios.version()).thenReturn(version);
        when(cambios.categorias()).thenReturn(categorias);
        when(cambios.profesionales()).thenReturn(profesionales);
        when(cambios.horariosSemanales()).thenReturn(horarios);

        return cambios;
    }
    @Test
    void debeReintentarCuandoRedisCambiaDuranteLaLectura() {
        configurarVersionLocal(4L);

        when(redisPort.obtenerVersionActual())
                .thenReturn(5L, 6L, 6L, 6L);

        when(redisPort.obtenerVersionMasAntiguaDisponible())
                .thenReturn(4L);

        CambiosCatalogo cambios5 = crearCambios(
                5L, List.of(), List.of(), List.of()
        );

        CambiosCatalogo cambios6 = crearCambios(
                6L, List.of(), List.of(), List.of()
        );

        when(redisPort.obtenerCambios(5L))
                .thenReturn(Optional.of(cambios5));

        when(redisPort.obtenerCambios(6L))
                .thenReturn(Optional.of(cambios6));

        useCase.sincronizarCatalogoIncremental();

        verify(redisPort, times(2)).obtenerCambios(5L);
        verify(redisPort).obtenerCambios(6L);

        verify(persistencePort).aplicarVersion(
                4L, 6L,
                List.of(),
                List.of(),
                List.of()
        );

        verifyNoMoreInteractions(persistencePort);
        verifyNoInteractions(sincronizacionCompleta);
    }
}