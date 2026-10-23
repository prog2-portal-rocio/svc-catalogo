package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoSnapshotPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
class AplicarSnapshotCatalogoUseCaseTest {

    private CatalogoSnapshotPersistencePort persistencePort;
    private AplicarSnapshotCatalogoUseCase useCase;

    @BeforeEach
    void setUp() {
        persistencePort = mock(CatalogoSnapshotPersistencePort.class);
        useCase = new AplicarSnapshotCatalogoUseCase(persistencePort);
    }

    @Test
    void delegaSnapshotValidoAlPuertoDePersistencia() {
        useCase.aplicarSnapshot(
                List.of(),
                List.of(),
                List.of(),
                1L
        );

        verify(persistencePort).reemplazarCatalogo(
                List.of(),
                List.of(),
                List.of(),
                1L
        );
    }

    @Test
    void rechazaVersionNegativaSinPersistir() {
        assertThrows(IllegalArgumentException.class, () ->
                useCase.aplicarSnapshot(
                        List.of(),
                        List.of(),
                        List.of(),
                        -1L
                )
        );

        verifyNoInteractions(persistencePort);
    }

    @Test
    void rechazaCategoriasNulasSinPersistir() {
        assertThrows(NullPointerException.class, () ->
                useCase.aplicarSnapshot(
                        null,
                        List.of(),
                        List.of(),
                        1L
                )
        );

        verifyNoInteractions(persistencePort);
    }

    @Test
    void rechazaProfesionalesNulosSinPersistir() {
        assertThrows(NullPointerException.class, () ->
                useCase.aplicarSnapshot(
                        List.of(),
                        null,
                        List.of(),
                        1L
                )
        );

        verifyNoInteractions(persistencePort);
    }

    @Test
    void rechazaHorariosNulosSinPersistir() {
        assertThrows(NullPointerException.class, () ->
                useCase.aplicarSnapshot(
                        List.of(),
                        List.of(),
                        null,
                        1L
                )
        );

        verifyNoInteractions(persistencePort);
    }

    @Test
    void delegaSnapshotConDatosAlPuertoDePersistencia() {
        Instant fecha = Instant.parse("2026-10-22T12:00:00Z");

        Categoria categoria = new Categoria(
                1L,
                "Medicina general",
                "Consultas generales",
                true,
                fecha,
                fecha
        );

        Profesional profesional = new Profesional(
                10L,
                1L,
                "Ana",
                "Perez",
                true,
                fecha,
                fecha
        );

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

        List<Categoria> categorias = List.of(categoria);
        List<Profesional> profesionales = List.of(profesional);
        List<HorarioSemanal> horarios = List.of(horario);

        useCase.aplicarSnapshot(
                categorias,
                profesionales,
                horarios,
                5L
        );

        verify(persistencePort).reemplazarCatalogo(
                categorias,
                profesionales,
                horarios,
                5L
        );
    }

}
