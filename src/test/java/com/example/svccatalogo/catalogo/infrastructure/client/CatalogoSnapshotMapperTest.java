package com.example.svccatalogo.catalogo.infrastructure.client;

import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.CategoriaResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.ProfesionalResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.HorarioSemanalResponseDto;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoSnapshotMapperTest {

    private final CatalogoSnapshotMapper mapper =
            new CatalogoSnapshotMapper();

    @Test
    void debeConvertirSnapshotCompleto() {
        Instant fecha = Instant.parse("2026-09-10T13:40:00Z");

        SnapshotResponseDto respuesta = new SnapshotResponseDto(
                7L,
                fecha,
                List.of(new CategoriaResponseDto(
                        1L, "Clinica medica", "Atencion general",
                        true, fecha, fecha
                )),
                List.of(new ProfesionalResponseDto(
                        15L, 1L, "Ana", "Gomez",
                        true, fecha, fecha
                )),
                List.of(new HorarioSemanalResponseDto(
                        21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        30, true, fecha, fecha
                ))
        );

        SnapshotCatalogo snapshot = mapper.toDomain(respuesta);

        assertEquals(7L, snapshot.version());

        assertEquals(1, snapshot.categorias().size());
        assertEquals("Clinica medica",
                snapshot.categorias().get(0).getName());

        assertEquals(1, snapshot.profesionales().size());
        assertEquals("Ana",
                snapshot.profesionales().get(0).getFirstName());
        assertEquals(1L,
                snapshot.profesionales().get(0).getCategoryId());

        assertEquals(1, snapshot.horarios().size());
        assertEquals(DayOfWeek.MONDAY,
                snapshot.horarios().get(0).getDayOfWeek());
        assertEquals(LocalTime.of(9, 0),
                snapshot.horarios().get(0).getStartTime());
        assertEquals(30,
                snapshot.horarios().get(0).getSlotDurationMinutes());

        assertEquals(fecha,
                snapshot.categorias().get(0).getCreatedAt());
    }

    @Test
    void debeRechazarSnapshotNulo() {
        assertThrows(NullPointerException.class, () ->
                mapper.toDomain(null)
        );
    }

    @Test
    void debeRechazarVersionAusente() {
        SnapshotResponseDto respuesta = new SnapshotResponseDto(
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );

        assertThrows(NullPointerException.class, () ->
                mapper.toDomain(respuesta)
        );
    }

    @Test
    void debeRechazarListaDeProfesionalesAusente() {
        SnapshotResponseDto respuesta = new SnapshotResponseDto(
                1L,
                null,
                List.of(),
                null,
                List.of()
        );

        assertThrows(NullPointerException.class, () ->
                mapper.toDomain(respuesta)
        );
    }
}
