package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.infrastructure.security.JwtTestContextInitializer;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(initializers = JwtTestContextInitializer.class)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ProfesionalPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private ProfesionalRepositoryPort profesionalRepository;

    @Autowired
    private BuscarProfesionalesPort buscarProfesionalesPort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HorarioSemanalRepositoryPort horarioRepository;

    @Test
    void guardaYRecuperaUnProfesionalConSuCategoria() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L,
                "Peluquería",
                "Servicios de cabello",
                true,
                fecha,
                fecha
        );

        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                10L,
                1L,
                "Ana",
                "García",
                true,
                fecha,
                fecha
        );

        profesionalRepository.save(profesional);

        Profesional recuperado = profesionalRepository.findById(10L)
                .orElseThrow();

        assertEquals(10L, recuperado.getId());
        assertEquals(1L, recuperado.getCategoryId());
        assertEquals("Ana", recuperado.getFirstName());
        assertEquals("García", recuperado.getLastName());
        assertTrue(recuperado.isEnabled());
        assertEquals(fecha, recuperado.getCreatedAt());
        assertEquals(fecha, recuperado.getUpdatedAt());
    }

    @Test
    void listaLosProfesionalesGuardados() {
        Categoria categoria = new Categoria(
                2L, "Masajes", null,
                true, null, null
        );

        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                11L, 2L, "Lucas", "Pérez",
                false, null, null
        );

        profesionalRepository.save(profesional);

        assertTrue(profesionalRepository.findAll().stream()
                .anyMatch(p -> p.getId().equals(11L)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("busquedas")
    void buscaProfesionalesEnPostgreSQL(
            String descripcion,
            Long categoriaId,
            String nombre,
            Boolean habilitado,
            Set<Long> idsEsperados) {

        cargarProfesionalesParaBusqueda();
        var criterios = new CriteriosBusquedaProfesional(categoriaId, nombre, habilitado);

        var resultados = buscarProfesionalesPort.buscarProfesionales(criterios);

        assertEquals(idsEsperados.size(), resultados.size());
        assertEquals(idsEsperados, resultados.stream()
                .map(Profesional::getId)
                .collect(Collectors.toSet()));
    }

    @Test
    void devuelveUnaColeccionVaciaSiNoHayProfesionales() {
        var criterios = new CriteriosBusquedaProfesional(null, null, null);

        assertTrue(buscarProfesionalesPort.buscarProfesionales(criterios).isEmpty());
    }

    @Test
    void apiBuscaEnPostgreSQLConLosTresFiltros() throws Exception {
        cargarProfesionalesParaBusqueda();

        mockMvc.perform(consultaAutorizada()
                        .param("categoriaId", "2")
                        .param("nombre", "  aNa  ")
                        .param("habilitado", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":13,"categoryId":2,"firstName":"ANA","lastName":"Lopez",
                          "enabled":false,"createdAt":null,"updatedAt":null}]
                        """));
    }

    @Test
    void apiSinFiltrosDevuelveLaCopiaLocalCompleta() throws Exception {
        cargarProfesionalesParaBusqueda();

        mockMvc.perform(consultaAutorizada())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void apiDevuelveListaVaciaParaCategoriaSinCoincidencias() throws Exception {
        cargarProfesionalesParaBusqueda();

        mockMvc.perform(consultaAutorizada().param("categoriaId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void apiRechazaParametrosInvalidosConProblemDetail() throws Exception {
        mockMvc.perform(consultaAutorizada().param("categoriaId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    private MockHttpServletRequestBuilder consultaAutorizada() {
        return get("/api/catalogo/profesionales")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_catalogo:read")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("disponibilidades")
    void buscaDisponibilidadConLaAgendaLocal(String descripcion, String fecha, String hora,
                                            Long categoriaId, String nombre, Boolean habilitado,
                                            Set<Long> esperados) {
        cargarAgendaParaDisponibilidad();
        var criterios = new CriteriosBusquedaProfesional(categoriaId, nombre, habilitado,
                LocalDate.parse(fecha), hora == null ? null : LocalTime.parse(hora));

        var resultados = buscarProfesionalesPort.buscarProfesionales(criterios);

        assertEquals(esperados.size(), resultados.size());
        assertEquals(esperados, resultados.stream().map(Profesional::getId).collect(Collectors.toSet()));
    }

    @Test
    void apiConsultaDisponibilidadLocalConTodosLosFiltros() throws Exception {
        cargarAgendaParaDisponibilidad();

        mockMvc.perform(consultaAutorizada().param("fecha", "2026-11-09").param("hora", "11:30")
                        .param("categoriaId", "1").param("nombre", "  aNa  ").param("habilitado", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":10,"categoryId":1,"firstName":"Ana","lastName":"García",
                          "enabled":true,"createdAt":null,"updatedAt":null}]
                        """));
    }

    @Test
    void apiDevuelveListaVaciaSiNoHayBloquesCompletos() throws Exception {
        cargarAgendaParaDisponibilidad();

        mockMvc.perform(consultaAutorizada().param("fecha", "2026-11-09").param("hora", "12:00"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private void cargarAgendaParaDisponibilidad() {
        cargarProfesionalesParaBusqueda();
        guardarHorario(1L, 10L, DayOfWeek.MONDAY, "09:00", "12:00", 30, true);
        guardarHorario(2L, 10L, DayOfWeek.MONDAY, "11:00", "12:00", 30, true);
        guardarHorario(3L, 12L, DayOfWeek.MONDAY, "09:15", "10:15", 30, true);
        guardarHorario(4L, 12L, DayOfWeek.MONDAY, "14:00", "15:00", 20, true);
        guardarHorario(5L, 11L, DayOfWeek.MONDAY, "09:00", "12:00", 30, true);
        guardarHorario(6L, 14L, DayOfWeek.MONDAY, "09:00", "12:00", 30, true);
        guardarHorario(7L, 15L, DayOfWeek.MONDAY, "09:00", "09:20", 30, true);
        guardarHorario(8L, 17L, DayOfWeek.MONDAY, "09:00", "12:00", 30, false);
        guardarHorario(9L, 17L, DayOfWeek.TUESDAY, "09:00", "12:00", 30, true);
    }

    private void guardarHorario(Long id, Long profesionalId, DayOfWeek dia,
                                String inicio, String fin, int duracion, boolean habilitado) {
        horarioRepository.save(new HorarioSemanal(id, profesionalId, dia,
                LocalTime.parse(inicio), LocalTime.parse(fin), duracion, habilitado, null, null));
    }

    private static Stream<Arguments> disponibilidades() {
        String lunes = "2026-11-09";
        return Stream.of(
                Arguments.of("Fecha sin hora", lunes, null, null, null, null, Set.of(10L, 12L)),
                Arguments.of("Otro lunes", "2026-11-16", null, null, null, null, Set.of(10L, 12L)),
                Arguments.of("Martes", "2026-11-10", null, null, null, null, Set.of(17L)),
                Arguments.of("Dia sin atencion", "2026-11-11", null, null, null, null, Set.of()),
                Arguments.of("Inicio exacto", lunes, "09:00", null, null, null, Set.of(10L)),
                Arguments.of("Alineacion desde cada horario", lunes, "09:15", null, null, null, Set.of(12L)),
                Arguments.of("Ultimo bloque y horarios superpuestos sin duplicados", lunes, "11:30", null, null, null, Set.of(10L)),
                Arguments.of("Hora de cierre", lunes, "12:00", null, null, null, Set.of()),
                Arguments.of("Antes de apertura", lunes, "08:30", null, null, null, Set.of()),
                Arguments.of("Despues de cierre", lunes, "18:00", null, null, null, Set.of()),
                Arguments.of("Desalineado", lunes, "09:10", null, null, null, Set.of()),
                Arguments.of("Segundos desalineados", lunes, "09:00:01", null, null, null, Set.of()),
                Arguments.of("Segundo horario del dia", lunes, "14:00", null, null, null, Set.of(12L)),
                Arguments.of("Ultimo bloque de veinte minutos", lunes, "14:40", null, null, null, Set.of(12L)),
                Arguments.of("Bloque no cabe", lunes, "14:50", null, null, null, Set.of()),
                Arguments.of("Hueco entre horarios", lunes, "13:00", null, null, null, Set.of()),
                Arguments.of("Profesionales deshabilitados", lunes, null, null, null, false, Set.of()),
                Arguments.of("Categoria deshabilitada", lunes, null, 3L, null, true, Set.of()),
                Arguments.of("Horario deshabilitado", lunes, null, null, "Julia", true, Set.of()),
                Arguments.of("Duracion mayor al intervalo", lunes, null, null, "Nombre%Literal", true, Set.of()),
                Arguments.of("Todos los filtros", lunes, "09:30", 1L, " aNA ", true, Set.of(10L)),
                Arguments.of("Filtros incompatibles", lunes, "09:30", 2L, "Ana", true, Set.of()),
                Arguments.of("Categoria inexistente", lunes, null, 999L, null, null, Set.of())
        );
    }

    private void cargarProfesionalesParaBusqueda() {
        categoriaRepository.save(new Categoria(1L, "Peluqueria", null, true, null, null));
        categoriaRepository.save(new Categoria(2L, "Masajes", null, true, null, null));
        categoriaRepository.save(new Categoria(3L, "Categoria deshabilitada", null, false, null, null));

        profesionalRepository.save(new Profesional(10L, 1L, "Ana", "García", true, null, null));
        profesionalRepository.save(new Profesional(11L, 1L, "Anabel", "Perez", false, null, null));
        profesionalRepository.save(new Profesional(12L, 2L, "Lucas", "García", true, null, null));
        profesionalRepository.save(new Profesional(13L, 2L, "ANA", "Lopez", false, null, null));
        profesionalRepository.save(new Profesional(14L, 3L, "Marta", "Diaz", true, null, null));
        profesionalRepository.save(new Profesional(15L, 2L, "Nombre%Literal", "Apellido_con_barra\\", true, null, null));
        profesionalRepository.save(new Profesional(16L, 1L, "Luis", "Ana", false, null, null));
        profesionalRepository.save(new Profesional(17L, 2L, "Julia", "Torres", true, null, null));
    }

    private static Stream<Arguments> busquedas() {
        Set<Long> todos = Set.of(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L);
        return Stream.of(
                Arguments.of("Sin filtros", null, null, null, todos),
                Arguments.of("Categoria sin filtrar habilitacion", 1L, null, null, Set.of(10L, 11L, 16L)),
                Arguments.of("Nombre parcial y mayusculas", null, "aNa", null, Set.of(10L, 11L, 13L, 16L)),
                Arguments.of("Apellido parcial y mayusculas", null, "gArC", null, Set.of(10L, 12L)),
                Arguments.of("Fragmento interior del nombre", null, "uCa", null, Set.of(12L)),
                Arguments.of("Solo habilitados", null, null, true, Set.of(10L, 12L, 14L, 15L, 17L)),
                Arguments.of("Solo deshabilitados", null, null, false, Set.of(11L, 13L, 16L)),
                Arguments.of("Tres filtros con true", 1L, "ana", true, Set.of(10L)),
                Arguments.of("Tres filtros con false", 2L, "ana", false, Set.of(13L)),
                Arguments.of("Categoria inexistente", 999L, null, null, Set.of()),
                Arguments.of("Nombre sin coincidencias", null, "Inexistente", null, Set.of()),
                Arguments.of("Filtros individualmente validos sin coincidencia conjunta", 2L, "ana", true, Set.of()),
                Arguments.of("Espacios exteriores", null, "  Ana  ", null, Set.of(10L, 11L, 13L, 16L)),
                Arguments.of("Nombre vacio", null, "", null, todos),
                Arguments.of("Solo espacios", null, " \t\n ", null, todos),
                Arguments.of("Porcentaje literal", null, "%", null, Set.of(15L)),
                Arguments.of("Guion bajo literal", null, "_", null, Set.of(15L)),
                Arguments.of("Barra invertida literal", null, "\\", null, Set.of(15L)),
                Arguments.of("No omitir categoria deshabilitada", 3L, null, true, Set.of(14L)),
                Arguments.of("No eliminar acentos", null, "garcia", null, Set.of())
        );
    }
}
