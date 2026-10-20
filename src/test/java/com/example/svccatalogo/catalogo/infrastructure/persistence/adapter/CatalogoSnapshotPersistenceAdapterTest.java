package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoSnapshotPersistencePort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class CatalogoSnapshotPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CatalogoSnapshotPersistencePort snapshotPort;

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private ProfesionalRepositoryPort profesionalRepository;

    @Autowired
    private HorarioSemanalRepositoryPort horarioRepository;

    @Autowired
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void aplicaSnapshotCompletoYActualizaVersion() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L, "Peluquería", "Servicios de cabello",
                true, fecha, fecha
        );

        Profesional profesional = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
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

        snapshotPort.reemplazarCatalogo(
                List.of(categoria),
                List.of(profesional),
                List.of(horario),
                1L
        );

        assertEquals(1, categoriaRepository.findAll().size());
        assertEquals(1, profesionalRepository.findAll().size());
        assertEquals(1, horarioRepository.findAll().size());

        assertEquals(
                "Peluquería",
                categoriaRepository.findById(1L).orElseThrow().getName()
        );

        assertEquals(
                1L,
                profesionalRepository.findById(10L).orElseThrow().getCategoryId()
        );

        assertEquals(
                DayOfWeek.MONDAY,
                horarioRepository.findById(100L).orElseThrow().getDayOfWeek()
        );

        var estado = estadoRepository.obtenerEstado();

        assertEquals(1L, estado.getAppliedVersion());
        assertEquals("SUCCESS", estado.getPhase());
        assertNotNull(estado.getAppliedAt());

    }

    @Test
    void revierteCambiosSiFallaLaSincronizacion() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        // Primera sincronización: versión 1 válida.
        Categoria categoriaOriginal = new Categoria(
                1L, "Peluquería", null,
                true, fecha, fecha
        );

        Profesional profesionalOriginal = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
        );

        snapshotPort.reemplazarCatalogo(
                List.of(categoriaOriginal),
                List.of(profesionalOriginal),
                List.of(),
                1L
        );

        // Segunda sincronización: versión 2 inválida.
        Categoria nuevaCategoria = new Categoria(
                2L, "Masajes", null,
                true, fecha, fecha
        );

        // La categoría 999 no existe en el nuevo snapshot.
        Profesional profesionalInvalido = new Profesional(
                20L, 999L, "Lucas", "Pérez",
                true, fecha, fecha
        );

        assertThrows(Exception.class, () ->
                snapshotPort.reemplazarCatalogo(
                        List.of(nuevaCategoria),
                        List.of(profesionalInvalido),
                        List.of(),
                        2L
                )
        );

        // Comprobar que se conservaron los datos originales.
        assertTrue(categoriaRepository.findById(1L).isPresent());
        assertTrue(profesionalRepository.findById(10L).isPresent());

        // Comprobar que no quedaron datos de la versión fallida.
        assertTrue(categoriaRepository.findById(2L).isEmpty());
        assertTrue(profesionalRepository.findById(20L).isEmpty());

        // La versión aplicada debe seguir siendo 1.
        assertEquals(1L, estadoRepository.obtenerEstado().getAppliedVersion());
    }

    @Test
    void reemplazaLosDatosDeUnSnapshotAnterior() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        // Aplicar la versión 1.
        Categoria categoriaAnterior = new Categoria(
                1L, "Peluquería", null,
                true, fecha, fecha
        );

        Profesional profesionalAnterior = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
        );

        snapshotPort.reemplazarCatalogo(
                List.of(categoriaAnterior),
                List.of(profesionalAnterior),
                List.of(),
                1L
        );

        // Aplicar la versión 2 con datos diferentes.
        Categoria categoriaNueva = new Categoria(
                2L, "Masajes", null,
                true, fecha, fecha
        );

        Profesional profesionalNuevo = new Profesional(
                20L, 2L, "Lucas", "Pérez",
                true, fecha, fecha
        );

        snapshotPort.reemplazarCatalogo(
                List.of(categoriaNueva),
                List.of(profesionalNuevo),
                List.of(),
                2L
        );

        // Los registros anteriores deben desaparecer.
        assertTrue(categoriaRepository.findById(1L).isEmpty());
        assertTrue(profesionalRepository.findById(10L).isEmpty());

        // Los nuevos registros deben existir.
        assertTrue(categoriaRepository.findById(2L).isPresent());
        assertTrue(profesionalRepository.findById(20L).isPresent());

        // La versión debe actualizarse.
        assertEquals(
                2L,
                estadoRepository.obtenerEstado().getAppliedVersion()
        );
    }


    @Test
    void ignoraSnapshotsConVersionIgualOAnterior() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoriaOriginal = new Categoria(
                1L, "Peluquería", null,
                true, fecha, fecha
        );

        // Aplicamos correctamente la versión 5.
        snapshotPort.reemplazarCatalogo(
                List.of(categoriaOriginal),
                List.of(),
                List.of(),
                5L
        );

        Categoria categoriaDiferente = new Categoria(
                2L, "Masajes", null,
                true, fecha, fecha
        );

        // Intentamos aplicar nuevamente la versión 5.
        snapshotPort.reemplazarCatalogo(
                List.of(categoriaDiferente),
                List.of(),
                List.of(),
                5L
        );

        // Intentamos aplicar una versión anterior.
        snapshotPort.reemplazarCatalogo(
                List.of(categoriaDiferente),
                List.of(),
                List.of(),
                4L
        );

        // Los datos originales deben permanecer intactos.
        assertEquals(1, categoriaRepository.findAll().size());
        assertTrue(categoriaRepository.findById(1L).isPresent());
        assertTrue(categoriaRepository.findById(2L).isEmpty());

        // La versión aplicada debe seguir siendo 5.
        assertEquals(
                5L,
                estadoRepository.obtenerEstado().getAppliedVersion()
        );
    }


    @Test
    void aplicaSnapshotVacioYEliminaDatosAnteriores() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L, "Peluquería", null,
                true, fecha, fecha
        );

        Profesional profesional = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
        );

        HorarioSemanal horario = new HorarioSemanal(
                100L, 10L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30, true, fecha, fecha
        );

        // Primero aplicamos un catálogo con datos.
        snapshotPort.reemplazarCatalogo(
                List.of(categoria),
                List.of(profesional),
                List.of(horario),
                1L
        );

        // Luego recibimos una nueva versión vacía.
        snapshotPort.reemplazarCatalogo(
                List.of(),
                List.of(),
                List.of(),
                2L
        );

        // Todas las tablas del catálogo deben quedar vacías.
        assertTrue(categoriaRepository.findAll().isEmpty());
        assertTrue(profesionalRepository.findAll().isEmpty());
        assertTrue(horarioRepository.findAll().isEmpty());

        // La nueva versión debe quedar aplicada.
        assertEquals(
                2L,
                estadoRepository.obtenerEstado().getAppliedVersion()
        );
    }


    @Test
    void conservaLaVersionMasRecienteConSnapshotsConcurrentes() throws Exception {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoriaV10 = new Categoria(
                10L, "Categoria version 10", null,
                true, fecha, fecha
        );

        Categoria categoriaV11 = new Categoria(
                11L, "Categoria version 11", null,
                true, fecha, fecha
        );

        CountDownLatch inicio = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<?> tareaV10 = executor.submit(() -> {
                inicio.await();
                snapshotPort.reemplazarCatalogo(
                        List.of(categoriaV10),
                        List.of(),
                        List.of(),
                        10L
                );
                return null;
            });

            Future<?> tareaV11 = executor.submit(() -> {
                inicio.await();
                snapshotPort.reemplazarCatalogo(
                        List.of(categoriaV11),
                        List.of(),
                        List.of(),
                        11L
                );
                return null;
            });

            // Permitimos que ambos hilos comiencen.
            inicio.countDown();

            // Esperamos su finalización y detectamos excepciones.
            tareaV10.get(30, TimeUnit.SECONDS);
            tareaV11.get(30, TimeUnit.SECONDS);
        }

        // La versión final debe ser la más reciente.
        assertEquals(
                11L,
                estadoRepository.obtenerEstado().getAppliedVersion()
        );

        // Deben permanecer únicamente los datos de la versión 11.
        assertEquals(1, categoriaRepository.findAll().size());
        assertTrue(categoriaRepository.findById(11L).isPresent());
        assertTrue(categoriaRepository.findById(10L).isEmpty());
    }
}
