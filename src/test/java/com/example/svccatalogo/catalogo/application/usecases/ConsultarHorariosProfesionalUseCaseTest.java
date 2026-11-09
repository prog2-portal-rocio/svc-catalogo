package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.application.exception.ProfesionalNoEncontradoException;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarHorariosProfesionalUseCaseTest {

    private ProfesionalRepositoryPort profesionales;
    private HorarioSemanalRepositoryPort horarios;
    private ConsultarHorariosProfesionalUseCase useCase;

    @BeforeEach
    void configurar() {
        profesionales = mock(ProfesionalRepositoryPort.class);
        horarios = mock(HorarioSemanalRepositoryPort.class);
        useCase = new ConsultarHorariosProfesionalUseCase(profesionales, horarios);
    }

    @Test
    void devuelveTodosLosHorariosAunqueElProfesionalEsteDeshabilitado() {
        when(profesionales.findById(10L)).thenReturn(Optional.of(
                new Profesional(10L, 1L, "Ana", "Garcia", false, null, null)));
        var habilitado = new HorarioSemanal(1L, 10L, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, null, null);
        var deshabilitado = new HorarioSemanal(2L, 10L, DayOfWeek.FRIDAY,
                LocalTime.of(14, 0), LocalTime.of(18, 0), 60, false, null, null);
        when(horarios.buscarPorProfesional(10L)).thenReturn(List.of(habilitado, deshabilitado));

        assertEquals(List.of(habilitado, deshabilitado), useCase.consultarHorarios(10L));
        verify(profesionales).findById(10L);
        verify(horarios).buscarPorProfesional(10L);
        verifyNoMoreInteractions(profesionales, horarios);
    }

    @Test
    void devuelveListaVaciaParaProfesionalSinHorarios() {
        when(profesionales.findById(10L)).thenReturn(Optional.of(
                new Profesional(10L, 1L, "Ana", "Garcia", true, null, null)));
        when(horarios.buscarPorProfesional(10L)).thenReturn(List.of());

        assertTrue(useCase.consultarHorarios(10L).isEmpty());
    }

    @Test
    void noConfundeProfesionalInexistenteConAgendaVacia() {
        when(profesionales.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ProfesionalNoEncontradoException.class, () -> useCase.consultarHorarios(10L));
        verifyNoInteractions(horarios);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rechazaIdInvalidoSinConsultarPersistencia(Long id) {
        assertThrows(IllegalArgumentException.class, () -> useCase.consultarHorarios(id));
        verifyNoInteractions(profesionales, horarios);
    }
}
