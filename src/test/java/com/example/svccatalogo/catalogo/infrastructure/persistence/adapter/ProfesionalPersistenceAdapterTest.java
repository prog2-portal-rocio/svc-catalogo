package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
