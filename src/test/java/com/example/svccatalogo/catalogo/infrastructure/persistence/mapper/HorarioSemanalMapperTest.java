package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.HorarioSemanalEntity;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class HorarioSemanalMapperTest {

    @Test
    void convierteDominioAEntidad() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        HorarioSemanal horario = new HorarioSemanal(
                100L,
                10L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30,
                true,
                fecha,
                fecha
        );

        HorarioSemanalEntity entity = HorarioSemanalMapper.toEntity(horario);

        assertEquals(100L, entity.getId());
        assertEquals(10L, entity.getProfessionalId());
        assertEquals(DayOfWeek.MONDAY, entity.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), entity.getStartTime());
        assertEquals(LocalTime.of(13, 0), entity.getEndTime());
        assertEquals(30, entity.getSlotDurationMinutes());
        assertTrue(entity.isEnabled());
        assertEquals(fecha, entity.getCreatedAt());
        assertEquals(fecha, entity.getUpdatedAt());
    }

    @Test
    void convierteEntidadADominio() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        HorarioSemanalEntity entity = new HorarioSemanalEntity(
                101L,
                11L,
                DayOfWeek.FRIDAY,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                60,
                false,
                fecha,
                fecha
        );

        HorarioSemanal horario = HorarioSemanalMapper.toDomain(entity);

        assertEquals(101L, horario.getId());
        assertEquals(11L, horario.getProfessionalId());
        assertEquals(DayOfWeek.FRIDAY, horario.getDayOfWeek());
        assertEquals(LocalTime.of(14, 0), horario.getStartTime());
        assertEquals(LocalTime.of(18, 0), horario.getEndTime());
        assertEquals(60, horario.getSlotDurationMinutes());
        assertFalse(horario.isEnabled());
        assertEquals(fecha, horario.getCreatedAt());
        assertEquals(fecha, horario.getUpdatedAt());
    }
}
