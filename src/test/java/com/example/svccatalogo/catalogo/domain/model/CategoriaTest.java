package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaTest {

    @Test
    void deberiaCrearCategoriaConDatosValidos() {
        Instant creacion = Instant.parse("2026-07-01T12:00:00Z");
        Instant actualizacion = Instant.parse("2026-09-10T13:40:00Z");

        Categoria categoria = new Categoria(
                1L,
                "Clínica médica",
                "Atención clínica general",
                true,
                creacion,
                actualizacion
        );

        assertEquals(1L, categoria.getId());
        assertEquals("Clínica médica", categoria.getName());
        assertEquals("Atención clínica general", categoria.getDescription());
        assertTrue(categoria.isEnabled());
        assertEquals(creacion, categoria.getCreatedAt());
        assertEquals(actualizacion, categoria.getUpdatedAt());
    }

    @Test
    void deberiaRechazarIdInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new Categoria(0L, "Clínica médica", "Descripción",
                        true, Instant.now(), Instant.now())
        );
    }

    @Test
    void deberiaRechazarNombreVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new Categoria(1L, " ", "Descripción",
                        true, Instant.now(), Instant.now())
        );
    }
}
