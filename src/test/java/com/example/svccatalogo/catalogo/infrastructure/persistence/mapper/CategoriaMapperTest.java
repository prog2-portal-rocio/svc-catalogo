package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.CategoriaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaMapperTest {

    @Test
    void convierteDominioAEntidad() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L, "Peluquería", "Servicios de cabello",
                true, fecha, fecha
        );

        CategoriaEntity entity = CategoriaMapper.toEntity(categoria);

        assertEquals(1L, entity.getId());
        assertEquals("Peluquería", entity.getName());
        assertEquals("Servicios de cabello", entity.getDescription());
        assertTrue(entity.isEnabled());
        assertEquals(fecha, entity.getCreatedAt());
        assertEquals(fecha, entity.getUpdatedAt());
    }

    @Test
    void convierteEntidadADominio() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        CategoriaEntity entity = new CategoriaEntity(
                2L, "Masajes", null,
                false, fecha, fecha
        );

        Categoria categoria = CategoriaMapper.toDomain(entity);

        assertEquals(2L, categoria.getId());
        assertEquals("Masajes", categoria.getName());
        assertNull(categoria.getDescription());
        assertFalse(categoria.isEnabled());
        assertEquals(fecha, categoria.getCreatedAt());
        assertEquals(fecha, categoria.getUpdatedAt());
    }
}
