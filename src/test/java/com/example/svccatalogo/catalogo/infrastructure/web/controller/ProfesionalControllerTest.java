package com.example.svccatalogo.catalogo.infrastructure.web.controller;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.infrastructure.web.handler.ProfesionalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfesionalControllerTest {

    private static final String RUTA = "/api/catalogo/profesionales";

    private BuscarProfesionalesPort buscarPort;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        buscarPort = mock(BuscarProfesionalesPort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfesionalController(buscarPort))
                .setControllerAdvice(new ProfesionalExceptionHandler())
                .build();
    }

    @Test
    void devuelveElDtoDelProfesionalConSusCamposPublicos() throws Exception {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");
        when(buscarPort.buscarProfesionales(new CriteriosBusquedaProfesional(null, null, null)))
                .thenReturn(List.of(new Profesional(10L, 1L, "Ana", "García", false, fecha, fecha)));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [{"id":10,"categoryId":1,"firstName":"Ana","lastName":"García",
                          "enabled":false,"createdAt":"2026-01-15T10:00:00Z",
                          "updatedAt":"2026-01-15T10:00:00Z"}]
                        """));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consultasValidas")
    void delegaLosFiltrosEnElCasoDeUsoYDevuelveListaVacia(
            String descripcion, String categoria, String nombre, String habilitado,
            CriteriosBusquedaProfesional esperados) throws Exception {
        when(buscarPort.buscarProfesionales(esperados)).thenReturn(List.of());
        MockHttpServletRequestBuilder consulta = get(RUTA);
        if (categoria != null) {
            consulta.param("categoriaId", categoria);
        }
        if (nombre != null) {
            consulta.param("nombre", nombre);
        }
        if (habilitado != null) {
            consulta.param("habilitado", habilitado);
        }

        mockMvc.perform(consulta)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));

        verify(buscarPort).buscarProfesionales(esperados);
        verifyNoMoreInteractions(buscarPort);
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("parametrosInvalidos")
    void rechazaParametrosInvalidosSinEjecutarLaBusqueda(String parametro, String valor) throws Exception {
        mockMvc.perform(get(RUTA).param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").isString());

        verifyNoInteractions(buscarPort);
    }

    private static Stream<Arguments> consultasValidas() {
        return Stream.of(
                Arguments.of("Sin filtros", null, null, null,
                        new CriteriosBusquedaProfesional(null, null, null)),
                Arguments.of("Categoria", "2", null, null,
                        new CriteriosBusquedaProfesional(2L, null, null)),
                Arguments.of("Nombre con espacios", null, "  aNa  ", null,
                        new CriteriosBusquedaProfesional(null, "aNa", null)),
                Arguments.of("Habilitados", null, null, "true",
                        new CriteriosBusquedaProfesional(null, null, true)),
                Arguments.of("Deshabilitados", null, null, "false",
                        new CriteriosBusquedaProfesional(null, null, false)),
                Arguments.of("Combinados", "2", "Ana", "false",
                        new CriteriosBusquedaProfesional(2L, "Ana", false)),
                Arguments.of("Boolean sin distinguir mayusculas", null, null, " FALSE ",
                        new CriteriosBusquedaProfesional(null, null, false)),
                Arguments.of("Filtros vacios", "", "  ", "",
                        new CriteriosBusquedaProfesional(null, null, null))
        );
    }

    @Test
    void delegaFechaYHoraSinCalcularDisponibilidadEnElControlador() throws Exception {
        var criterios = new CriteriosBusquedaProfesional(1L, "Ana", true,
                LocalDate.of(2026, 11, 9), LocalTime.of(9, 30));
        when(buscarPort.buscarProfesionales(criterios)).thenReturn(List.of());

        mockMvc.perform(get(RUTA).param("categoriaId", "1").param("nombre", "Ana")
                        .param("habilitado", "true").param("fecha", "2026-11-09").param("hora", "09:30"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(buscarPort).buscarProfesionales(criterios);
    }

    @Test
    void permiteFechaSinHora() throws Exception {
        var criterios = new CriteriosBusquedaProfesional(null, null, null, LocalDate.of(2026, 11, 9), null);
        when(buscarPort.buscarProfesionales(criterios)).thenReturn(List.of());

        mockMvc.perform(get(RUTA).param("fecha", "2026-11-09"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(buscarPort).buscarProfesionales(criterios);
    }

    @ParameterizedTest
    @MethodSource("disponibilidadInvalida")
    void rechazaFechaYHoraInvalidas(String fecha, String hora) throws Exception {
        MockHttpServletRequestBuilder consulta = get(RUTA);
        if (fecha != null) {
            consulta.param("fecha", fecha);
        }
        if (hora != null) {
            consulta.param("hora", hora);
        }
        mockMvc.perform(consulta)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(buscarPort);
    }

    private static Stream<Arguments> disponibilidadInvalida() {
        return Stream.of(
                Arguments.of(null, "09:00"),
                Arguments.of("", "09:00"),
                Arguments.of("2026-02-30", null),
                Arguments.of("09/11/2026", null),
                Arguments.of("2026-13-01", null),
                Arguments.of("ayer", null),
                Arguments.of("2026-11-09", "24:00"),
                Arguments.of("2026-11-09", "09:60"),
                Arguments.of("2026-11-09", "nueve"),
                Arguments.of("2026-11-09", "-01:00")
        );
    }

    private static Stream<Arguments> parametrosInvalidos() {
        return Stream.of(
                Arguments.of("categoriaId", "0"),
                Arguments.of("categoriaId", "-1"),
                Arguments.of("categoriaId", "abc"),
                Arguments.of("categoriaId", "1.5"),
                Arguments.of("categoriaId", "9223372036854775808"),
                Arguments.of("habilitado", "quizas"),
                Arguments.of("habilitado", "1"),
                Arguments.of("habilitado", "0"),
                Arguments.of("habilitado", "yes"),
                Arguments.of("habilitado", "no"),
                Arguments.of("habilitado", "on"),
                Arguments.of("habilitado", "off")
        );
    }
}
