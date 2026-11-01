package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcesarNotificacionCatalogoServiceTest {

    private EventoCatalogoProcesadoPort eventosPort;
    private ConsultarEstadoSincronizacionPort estadoPort;
    private SincronizarCatalogoPort sincronizarPort;
    private ProcesarNotificacionCatalogoService service;

    @BeforeEach
    void configurar() {
        eventosPort = mock(EventoCatalogoProcesadoPort.class);
        estadoPort = mock(ConsultarEstadoSincronizacionPort.class);
        sincronizarPort = mock(SincronizarCatalogoPort.class);

        service = new ProcesarNotificacionCatalogoService(
                eventosPort,
                estadoPort,
                sincronizarPort
        );
    }

    @Test
    void debeIgnorarEventoDuplicado() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(false);

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verifyNoInteractions(estadoPort, sincronizarPort);
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

        verify(sincronizarPort).sincronizarCatalogo();
    }

    @Test
    void debeSincronizarSiNoExisteVersionLocal() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(null), estado(7L));

        service.procesar(new NotificacionCatalogo("evento-123", 7L));

        verify(sincronizarPort).sincronizarCatalogo();
    }

    @Test
    void debePropagarErrorCuandoSincronizacionFalla() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(5L));

        doThrow(new IllegalStateException("Error de sincronizacion"))
                .when(sincronizarPort).sincronizarCatalogo();

        assertThrows(
                IllegalStateException.class,
                () -> service.procesar(
                        new NotificacionCatalogo("evento-123", 7L)
                )
        );
    }

    @Test
    void debeRechazarSiNoAlcanzaVersionNotificada() {
        when(eventosPort.registrarSiNoExiste("evento-123", 7L))
                .thenReturn(true);

        when(estadoPort.consultarEstado())
                .thenReturn(estado(5L), estado(6L));

        assertThrows(
                IllegalStateException.class,
                () -> service.procesar(
                        new NotificacionCatalogo("evento-123", 7L)
                )
        );

        verify(sincronizarPort).sincronizarCatalogo();
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
