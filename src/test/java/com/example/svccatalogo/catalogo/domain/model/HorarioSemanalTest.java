
package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class HorarioSemanalTest {

    private final Instant creacion =
            Instant.parse("2026-07-01T12:00:00Z");

    private final Instant actualizacion =
            Instant.parse("2026-09-10T13:40:00Z");

    @Test
    void deberiaCrearHorarioConDatosValidos() {
        HorarioSemanal horario = new HorarioSemanal(
                21L, 15L, DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                30, true, creacion, actualizacion
        );

        assertEquals(21L, horario.getId());
        assertEquals(15L, horario.getProfessionalId());
        assertEquals(DayOfWeek.MONDAY, horario.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), horario.getStartTime());
        assertEquals(LocalTime.of(12, 0), horario.getEndTime());
        assertEquals(30, horario.getSlotDurationMinutes());
        assertTrue(horario.isEnabled());
        assertEquals(creacion, horario.getCreatedAt());
        assertEquals(actualizacion, horario.getUpdatedAt());
    }

    @Test
    void deberiaRechazarProfesionalInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new HorarioSemanal(
                        21L, 0L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0),
                        30, true, creacion, actualizacion
                )
        );
    }

    @Test
    void deberiaRechazarHorarioInvertido() {
        assertThrows(IllegalArgumentException.class, () ->
                new HorarioSemanal(
                        21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(12, 0), LocalTime.of(9, 0),
                        30, true, creacion, actualizacion
                )
        );
    }

    @Test
    void deberiaRechazarDuracionInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new HorarioSemanal(
                        21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0),
                        0, true, creacion, actualizacion
                )
        );
    }

    @Test
    void deberiaRechazarDiaNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new HorarioSemanal(
                        21L, 15L, null,
                        LocalTime.of(9, 0), LocalTime.of(12, 0),
                        30, true, creacion, actualizacion
                )
        );
    }
}
