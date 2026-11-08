package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuscarProfesionalesUseCaseTest {

    private ProfesionalRepositoryPort repository;
    private BuscarProfesionalesUseCase useCase;

    @BeforeEach
    void configurar() {
        repository = mock(ProfesionalRepositoryPort.class);
        useCase = new BuscarProfesionalesUseCase(repository);
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
}
