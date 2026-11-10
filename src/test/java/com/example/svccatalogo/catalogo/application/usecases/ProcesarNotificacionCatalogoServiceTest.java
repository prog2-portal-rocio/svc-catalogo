package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcesarNotificacionCatalogoServiceTest {

    private EventoCatalogoProcesadoPort eventosPort;
    private ConsultarEstadoSincronizacionPort estadoPort;
    private SincronizarCatalogoIncrementalPort sincronizarPort;
    private ProcesarNotificacionCatalogoService service;
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @BeforeEach
    void configurar() {
        eventosPort = mock(EventoCatalogoProcesadoPort.class);
        estadoPort = mock(ConsultarEstadoSincronizacionPort.class);
        sincronizarPort = mock(SincronizarCatalogoIncrementalPort.class);
        estadoRepository = mock(EstadoSincronizacionRepositoryPort.class);

        service = new ProcesarNotificacionCatalogoService(
                eventosPort,
                estadoPort,
                sincronizarPort,
                estadoRepository
        );
    }

    @Test
    void debeIgnorarEventoDuplicado() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(false);

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verifyNoInteractions(estadoPort, sincronizarPort);
        var orden = inOrder(estadoRepository, eventosPort);
        orden.verify(estadoRepository).obtenerEstadoConBloqueo();
        orden.verify(eventosPort).registrarSiNoExiste("evento-123", 7L);
    }

    @Test
    void debeProcesarEventoSiVersionYaEstaActualizada() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(7L));

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verifyNoInteractions(sincronizarPort);
        verify(eventosPort).registrarSiNoExiste("evento-123", 7L);
    }

    @Test
    void debeSincronizarSiVersionLocalEsAnterior() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(5L), estado(7L));

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verify(sincronizarPort).sincronizarCatalogoIncremental();
    }

    @Test
    void debeSincronizarSiNoExisteVersionLocal() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(null), estado(7L));

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verify(sincronizarPort).sincronizarCatalogoIncremental();
    }

    @Test
    void debePropagarErrorCuandoSincronizacionFalla() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(5L));

        doThrow(new IllegalStateException("Error de sincronizacion"))
                .when(sincronizarPort).sincronizarCatalogoIncremental();

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-123", 7L);

        assertThrows(
                IllegalStateException.class,
                () -> service.procesar(notificacion)
        );
    }

    @Test
    void debeRechazarSiNoAlcanzaVersionNotificada() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(5L), estado(6L));

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-123", 7L);

        assertThrows(
                IllegalStateException.class,
                () -> service.procesar(notificacion)
        );

        verify(sincronizarPort).sincronizarCatalogoIncremental();
    }

    @Test
    void debeDescartarNotificacionAtrasadaSinSincronizar() {
        when(eventosPort.registrarSiNoExiste("evento-atrasado", 6L)).thenReturn(true);
        when(estadoPort.consultarEstado()).thenReturn(estado(8L));

        service.procesar(new NotificacionCatalogo("evento-atrasado", 6L));

        verifyNoInteractions(sincronizarPort);
    }

    @Test
    void debeAceptarVersionVigenteSuperiorALaNotificada() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L)).thenReturn(true);
        when(estadoPort.consultarEstado()).thenReturn(estado(5L), estado(9L));

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verify(sincronizarPort).sincronizarCatalogoIncremental();
    }

    private EstadoSincronizacion estado(Long version) {
        return new EstadoSincronizacion(
                version,
                null,
                null,
                "SUCCESS",
                null
        );
    }
}
