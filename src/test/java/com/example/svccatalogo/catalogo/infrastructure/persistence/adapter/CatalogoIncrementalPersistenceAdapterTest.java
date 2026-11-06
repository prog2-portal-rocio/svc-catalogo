package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoIncrementalPersistencePort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoSnapshotPersistencePort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class CatalogoIncrementalPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CatalogoIncrementalPersistencePort incrementalPort;

    @Autowired
    private CatalogoSnapshotPersistencePort snapshotPort;

    @Autowired
    private CategoriaRepositoryPort categorias;

    @Autowired
    private ProfesionalRepositoryPort profesionales;

    @Autowired
    private HorarioSemanalRepositoryPort horarios;

    @Autowired
    private EstadoSincronizacionRepositoryPort estados;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant FECHA =
            Instant.parse("2026-01-15T10:00:00Z");

    @BeforeEach
    void prepararBaseDeDatos() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE horarios_semanales, profesionales, categorias"
        );

        jdbcTemplate.update("""
                UPDATE estado_sincronizacion
                SET applied_version = NULL,
                    applied_at = NULL,
                    last_attempt_at = NULL,
                    phase = 'NOT_STARTED',
                    safe_diagnostic = 'Sin sincronizaciones realizadas'
                WHERE id = 1
                """);
    }

    private Categoria categoria(boolean habilitada) {
        return new Categoria(
                1L, "Peluquería", null,
                habilitada, FECHA, FECHA
        );
    }

    private Profesional profesional(
            long id, boolean habilitado
    ) {
        return new Profesional(
                id, 1L, "Ana", "García",
                habilitado, FECHA, FECHA
        );
    }

    private HorarioSemanal horario(boolean habilitado) {
        return new HorarioSemanal(
                100L, 10L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30, habilitado, FECHA, FECHA
        );
    }

    private void cargarSnapshotInicial() {
        snapshotPort.reemplazarCatalogo(
                List.of(categoria(true)),
                List.of(
                        profesional(10L, true),
                        profesional(11L, true)
                ),
                List.of(horario(true)),
                10L
        );
    }

    @Test
    void actualizaUnProfesionalSinBorrarOtros() {
        cargarSnapshotInicial();

        incrementalPort.aplicarVersion(
                10L, 11L,
                List.of(),
                List.of(profesional(10L, false)),
                List.of()
        );

        assertFalse(
                profesionales.findById(10L)
                        .orElseThrow().isEnabled()
        );

        assertTrue(
                profesionales.findById(11L)
                        .orElseThrow().isEnabled()
        );

        assertEquals(2, profesionales.findAll().size());
        assertEquals(1, categorias.findAll().size());
        assertEquals(1, horarios.findAll().size());
        assertEquals(11L, estados.obtenerEstado().getAppliedVersion());
    }

    @Test
    void actualizaCategoriasYHorariosConBajasLogicas() {
        cargarSnapshotInicial();

        incrementalPort.aplicarVersion(
                10L, 11L,
                List.of(categoria(false)),
                List.of(),
                List.of(horario(false))
        );

        assertFalse(
                categorias.findById(1L)
                        .orElseThrow().isEnabled()
        );

        assertFalse(
                horarios.findById(100L)
                        .orElseThrow().isEnabled()
        );

        assertEquals(2, profesionales.findAll().size());
        assertEquals(11L, estados.obtenerEstado().getAppliedVersion());
    }

    @Test
    void ignoraVersionesYaAplicadas() {
        cargarSnapshotInicial();

        incrementalPort.aplicarVersion(
                10L, 11L,
                List.of(),
                List.of(profesional(10L, false)),
                List.of()
        );

        incrementalPort.aplicarVersion(
                10L, 11L,
                List.of(),
                List.of(profesional(10L, true)),
                List.of()
        );

        assertFalse(
                profesionales.findById(10L)
                        .orElseThrow().isEnabled()
        );

        assertEquals(11L, estados.obtenerEstado().getAppliedVersion());
    }

    @Test
    void permiteAplicarVariasVersionesEnUnaTransaccion() {
        cargarSnapshotInicial();

        incrementalPort.aplicarVersion(
                10L, 12L,
                List.of(),
                List.of(),
                List.of()
        );

        assertEquals(
                12L,
                estados.obtenerEstado().getAppliedVersion()
        );
    }

    @Test
    void rechazaVersionEsperadaIncorrecta() {
        cargarSnapshotInicial();

        assertThrows(
                IllegalStateException.class,
                () -> incrementalPort.aplicarVersion(
                        11L, 12L,
                        List.of(), List.of(), List.of()
                )
        );

        assertEquals(
                10L,
                estados.obtenerEstado().getAppliedVersion()
        );
    }

    @Test
    void rechazaVersionNuevaMenorOIgualALaEsperada() {
        cargarSnapshotInicial();

        assertThrows(
                IllegalArgumentException.class,
                () -> incrementalPort.aplicarVersion(
                        10L, 10L,
                        List.of(), List.of(), List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> incrementalPort.aplicarVersion(
                        10L, 9L,
                        List.of(), List.of(), List.of()
                )
        );

        assertEquals(
                10L,
                estados.obtenerEstado().getAppliedVersion()
        );
    }

    @Test
    void revierteTodosLosCambiosSiFallaUnaEntidad() {
        cargarSnapshotInicial();

        Categoria nuevaCategoria = new Categoria(
                2L, "Masajes", null,
                true, FECHA, FECHA
        );

        Profesional profesionalInvalido = new Profesional(
                20L, 999L, "Lucas", "Pérez",
                true, FECHA, FECHA
        );

        assertThrows(
                RuntimeException.class,
                () -> incrementalPort.aplicarVersion(
                        10L, 11L,
                        List.of(nuevaCategoria),
                        List.of(profesionalInvalido),
                        List.of()
                )
        );

        assertTrue(categorias.findById(2L).isEmpty());
        assertTrue(profesionales.findById(20L).isEmpty());

        assertTrue(categorias.findById(1L).isPresent());
        assertTrue(profesionales.findById(10L).isPresent());

        assertEquals(10L, estados.obtenerEstado().getAppliedVersion());
    }

    @Test
    void requiereSnapshotInicial() {
        assertThrows(
                IllegalStateException.class,
                () -> incrementalPort.aplicarVersion(
                        0L, 1L,
                        List.of(), List.of(), List.of()
                )
        );
    }
}
