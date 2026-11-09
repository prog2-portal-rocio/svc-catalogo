package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.application.exception.ProfesionalNoEncontradoException;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultarProfesionalUseCaseTest {

    private ProfesionalRepositoryPort profesionales;
    private CategoriaRepositoryPort categorias;
    private ConsultarProfesionalUseCase useCase;

    @BeforeEach
    void configurar() {
        profesionales = mock(ProfesionalRepositoryPort.class);
        categorias = mock(CategoriaRepositoryPort.class);
        useCase = new ConsultarProfesionalUseCase(profesionales, categorias);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void consultaSinOcultarEstadosDeHabilitacion(boolean profesionalHabilitado, boolean categoriaHabilitada) {
        var profesional = new Profesional(10L, 1L, "Ana", "Garcia", profesionalHabilitado, null, null);
        var categoria = new Categoria(1L, "Peluqueria", null, categoriaHabilitada, null, null);
        when(profesionales.findById(10L)).thenReturn(Optional.of(profesional));
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));

        var detalle = useCase.consultarProfesional(10L);

        assertSame(profesional, detalle.profesional());
        assertSame(categoria, detalle.categoria());
        verify(profesionales).findById(10L);
        verify(categorias).findById(1L);
        verifyNoMoreInteractions(profesionales, categorias);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rechazaIdInvalidoSinConsultarPersistencia(Long id) {
        assertThrows(IllegalArgumentException.class, () -> useCase.consultarProfesional(id));
        verifyNoInteractions(profesionales, categorias);
    }

    @Test
    void informaProfesionalInexistenteSinConsultarCategoria() {
        when(profesionales.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ProfesionalNoEncontradoException.class, () -> useCase.consultarProfesional(10L));
        verifyNoInteractions(categorias);
    }

    @Test
    void categoriaAusenteEsUnaInconsistenciaYNoUnProfesionalInexistente() {
        when(profesionales.findById(10L)).thenReturn(Optional.of(
                new Profesional(10L, 1L, "Ana", "Garcia", true, null, null)));
        when(categorias.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> useCase.consultarProfesional(10L));
    }
}
