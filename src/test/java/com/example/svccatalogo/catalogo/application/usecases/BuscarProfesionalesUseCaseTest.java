package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuscarProfesionalesUseCaseTest {

    private ProfesionalRepositoryPort repository;
    private BuscarProfesionalesUseCase useCase;
    private HorarioSemanalRepositoryPort horarios;

    @BeforeEach
    void configurar() {
        repository = mock(ProfesionalRepositoryPort.class);
        horarios = mock(HorarioSemanalRepositoryPort.class);
        useCase = new BuscarProfesionalesUseCase(repository, horarios);
    }

    @ParameterizedTest
    @MethodSource("criteriosDeBusqueda")
    void debeDelegarLaBusquedaLocalSinAlterarLosCriterios(CriteriosBusquedaProfesional criterios) {
        Profesional profesional = new Profesional(
                10L, 1L, "Ana", "Garcia", true, null, null);
        List<Profesional> esperados = List.of(profesional);
        when(repository.buscar(criterios)).thenReturn(esperados);

        List<Profesional> resultado = useCase.buscarProfesionales(criterios);

        assertEquals(esperados, resultado);
        verify(repository).buscar(criterios);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(horarios);
    }

    @Test
    void debeDevolverUnaColeccionVaciaSiNoHayCoincidencias() {
        var criterios = new CriteriosBusquedaProfesional(999L, null, null);
        when(repository.buscar(criterios)).thenReturn(List.of());

        assertTrue(useCase.buscarProfesionales(criterios).isEmpty());
        verify(repository).buscar(criterios);
    }

    @Test
    void debeRechazarUnObjetoDeCriteriosNulo() {
        assertThrows(NullPointerException.class,
                () -> useCase.buscarProfesionales(null));

        verifyNoInteractions(repository);
    }

    private static Stream<CriteriosBusquedaProfesional> criteriosDeBusqueda() {
        return Stream.of(
                new CriteriosBusquedaProfesional(null, null, null),
                new CriteriosBusquedaProfesional(1L, "  Ana  ", true),
                new CriteriosBusquedaProfesional(1L, "Ana", false)
        );
    }

    @Test
    void filtraBloquesEnDominioConsultandoLosHorariosEnUnSoloLote() {
        LocalDate fecha = LocalDate.of(2026, 11, 9);
        var criterios = new CriteriosBusquedaProfesional(null, null, null, fecha, LocalTime.of(10, 0));
        Profesional ana = new Profesional(10L, 1L, "Ana", "Garcia", true, null, null);
        Profesional lucas = new Profesional(11L, 1L, "Lucas", "Perez", true, null, null);
        when(repository.buscar(criterios)).thenReturn(List.of(ana, lucas));
        when(horarios.buscarHabilitadosPorProfesionalesYDia(List.of(10L, 11L), DayOfWeek.MONDAY))
                .thenReturn(List.of(
                        new HorarioSemanal(1L, 10L, DayOfWeek.MONDAY, LocalTime.of(9, 0),
                                LocalTime.of(12, 0), 30, true, null, null),
                        new HorarioSemanal(2L, 10L, DayOfWeek.MONDAY, LocalTime.of(10, 0),
                                LocalTime.of(11, 0), 30, true, null, null),
                        new HorarioSemanal(3L, 11L, DayOfWeek.MONDAY, LocalTime.of(9, 15),
                                LocalTime.of(12, 0), 30, true, null, null)));

        assertEquals(List.of(ana), useCase.buscarProfesionales(criterios));
        verify(horarios).buscarHabilitadosPorProfesionalesYDia(List.of(10L, 11L), DayOfWeek.MONDAY);
        verifyNoMoreInteractions(horarios);
    }

    @Test
    void noConsultaHorariosSiNoHayCandidatos() {
        var criterios = new CriteriosBusquedaProfesional(null, null, null, LocalDate.of(2026, 11, 9), null);
        when(repository.buscar(criterios)).thenReturn(List.of());

        assertTrue(useCase.buscarProfesionales(criterios).isEmpty());
        verifyNoInteractions(horarios);
    }
}
