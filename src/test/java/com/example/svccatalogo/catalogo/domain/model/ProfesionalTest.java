
package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProfesionalTest {

    @Test
    void deberiaCrearProfesionalConDatosValidos() {
        Instant creacion = Instant.parse("2026-07-01T12:00:00Z");
        Instant actualizacion = Instant.parse("2026-09-10T13:40:00Z");

        Profesional profesional = new Profesional(
                15L, 1L, "Ana", "Gomez",
                true, creacion, actualizacion
        );

        assertEquals(15L, profesional.getId());
        assertEquals(1L, profesional.getCategoryId());
        assertEquals("Ana", profesional.getFirstName());
        assertEquals("Gomez", profesional.getLastName());
        assertTrue(profesional.isEnabled());
        assertEquals(creacion, profesional.getCreatedAt());
        assertEquals(actualizacion, profesional.getUpdatedAt());
    }

    @Test
    void deberiaRechazarCategoriaInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new Profesional(
                        15L, 0L, "Ana", "Gomez",
                        true, Instant.now(), Instant.now()
                )
        );
    }

    @Test
    void deberiaRechazarNombreVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new Profesional(
                        15L, 1L, " ", "Gomez",
                        true, Instant.now(), Instant.now()
                )
        );
    }

    @Test
    void deberiaRechazarApellidoVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new Profesional(
                        15L, 1L, "Ana", " ",
                        true, Instant.now(), Instant.now()
                )
        );
    }
}
