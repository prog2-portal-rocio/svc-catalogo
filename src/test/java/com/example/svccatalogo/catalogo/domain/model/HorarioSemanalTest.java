
package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HorarioSemanalTest {

    @ParameterizedTest
    @CsvSource({
            "2026-11-09,,true,30,true",
            "2026-11-10,,true,30,false",
            "2026-11-09,,false,30,false",
            "2026-11-09,,true,200,false",
            "2026-11-09,,true,2147483647,false",
            "2026-11-09,09:00,true,30,true",
            "2026-11-09,11:30,true,30,true",
            "2026-11-09,12:00,true,30,false",
            "2026-11-09,08:30,true,30,false",
            "2026-11-09,12:30,true,30,false",
            "2026-11-09,09:15,true,30,false",
            "2026-11-09,09:00:01,true,30,false",
            "2026-11-09,09:00:00.000000001,true,30,false",
            "2026-11-09,11:40,true,40,false",
            "2026-11-09,11:00,true,40,true"
    })
    void compruebaBloquesCompletosAlineados(String fecha, String hora, boolean habilitado,
                                          int duracion, boolean esperado) {
        var horario = new HorarioSemanal(1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), duracion, habilitado, null, null);

        assertEquals(esperado, horario.tieneBloqueDisponible(LocalDate.parse(fecha),
                hora == null ? null : LocalTime.parse(hora)));
    }

    @Test
    void noEnvuelveElBloqueHaciaElDiaSiguiente() {
        var horario = new HorarioSemanal(1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(23, 0), LocalTime.of(23, 50), 30, true, null, null);

        assertTrue(horario.tieneBloqueDisponible(LocalDate.of(2026, 11, 9), LocalTime.of(23, 0)));
        assertFalse(horario.tieneBloqueDisponible(LocalDate.of(2026, 11, 9), LocalTime.of(23, 30)));
    }

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
