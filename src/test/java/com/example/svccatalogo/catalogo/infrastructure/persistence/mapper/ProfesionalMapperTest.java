package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.ProfesionalEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProfesionalMapperTest {

    @Test
    void convierteDominioAEntidad() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Profesional profesional = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
        );

        ProfesionalEntity entity = ProfesionalMapper.toEntity(profesional);

        assertEquals(10L, entity.getId());
        assertEquals(1L, entity.getCategoryId());
        assertEquals("Ana", entity.getFirstName());
        assertEquals("García", entity.getLastName());
        assertTrue(entity.isEnabled());
        assertEquals(fecha, entity.getCreatedAt());
        assertEquals(fecha, entity.getUpdatedAt());
    }

    @Test
    void convierteEntidadADominio() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        ProfesionalEntity entity = new ProfesionalEntity(
                11L, 2L, "Lucas", "Pérez",
                false, fecha, fecha
        );

        Profesional profesional = ProfesionalMapper.toDomain(entity);

        assertEquals(11L, profesional.getId());
        assertEquals(2L, profesional.getCategoryId());
        assertEquals("Lucas", profesional.getFirstName());
        assertEquals("Pérez", profesional.getLastName());
        assertFalse(profesional.isEnabled());
        assertEquals(fecha, profesional.getCreatedAt());
        assertEquals(fecha, profesional.getUpdatedAt());
    }
}
