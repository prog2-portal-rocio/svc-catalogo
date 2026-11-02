package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificacionCatalogoTest {

    @Test
    void debeCrearNotificacionValida() {
        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-123", 7L);

        assertEquals("evento-123", notificacion.eventId());
        assertEquals(7L, notificacion.nuevaVersion());
    }

    @Test
    void debeRechazarIdentificadorNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NotificacionCatalogo(null, 7L)
        );
    }

    @Test
    void debeRechazarIdentificadorVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NotificacionCatalogo("   ", 7L)
        );
    }

    @Test
    void debeRechazarVersionNegativa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NotificacionCatalogo("evento-123", -1L)
        );
    }
}
