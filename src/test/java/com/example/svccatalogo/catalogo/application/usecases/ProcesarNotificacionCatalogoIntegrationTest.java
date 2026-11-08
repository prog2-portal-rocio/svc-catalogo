package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.infrastructure.kafka.CatalogoKafkaConsumerAdapter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class ProcesarNotificacionCatalogoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @MockitoBean
    private CatalogoRemotoPort catalogoRemotoPort;

    @MockitoBean
    private CatalogoRedisPort redisPort;

    @Autowired
    private CatalogoKafkaConsumerAdapter consumidor;

    @Autowired
    private ProcesarNotificacionCatalogoUseCase procesarPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventoCatalogoProcesadoPort eventosPort;

    @Autowired
    private ConsultarEstadoSincronizacionPort estadoPort;

    @Autowired
    private SincronizarCatalogoIncrementalPort incrementalPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepararBaseDeDatos() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE eventos_catalogo_procesados"
        );

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
    void debeRegistrarEventoNuevoDespuesDeSincronizar() {
        when(catalogoRemotoPort.obtenerSnapshot())
                .thenReturn(snapshot(7L));

        procesarPort.procesar(
                new NotificacionCatalogo("evento-123", 7L)
        );

        assertEquals(1, cantidadEventosProcesados());
        assertEquals(7L, versionAplicada());

        verify(catalogoRemotoPort).obtenerSnapshot();
    }

    @Test
    void debeIgnorarEventoDuplicado() {
        when(catalogoRemotoPort.obtenerSnapshot())
                .thenReturn(snapshot(7L));

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-123", 7L);

        procesarPort.procesar(notificacion);
        procesarPort.procesar(notificacion);

        assertEquals(1, cantidadEventosProcesados());

        verify(catalogoRemotoPort, times(1))
                .obtenerSnapshot();
    }

    @Test
    void debeRevertirRegistroSiSincronizacionFalla() {
        when(catalogoRemotoPort.obtenerSnapshot())
                .thenThrow(new IllegalStateException(
                        "Error al obtener el snapshot"
                ));

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-123", 7L);

        assertThrows(
                IllegalStateException.class,
                () -> procesarPort.procesar(notificacion)
        );

        assertEquals(0, cantidadEventosProcesados());
        assertNull(versionAplicada());
    }

    @Test
    void debeRevertirIncrementosSiRedisNoAlcanzaVersionNotificada() {
        configurarIncremento(6L);
        ConsumerRecord<String, String> registro = registro(7L);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        assertThrows(IllegalStateException.class,
                () -> consumidor.consumir(registro, acknowledgment));

        assertEquals(5L, versionAplicada());
        assertEquals(0, cantidadEventosProcesados());
        assertEquals(0L, cantidadCategorias());
        verifyNoInteractions(acknowledgment, catalogoRemotoPort);

        // La reentrega debe funcionar cuando Redis publique la version pendiente.
        when(redisPort.obtenerVersionActual()).thenReturn(7L);
        when(redisPort.obtenerCambios(7L)).thenReturn(Optional.of(
                new CambiosCatalogo(7L, List.of(), List.of(), List.of())));
        consumidor.consumir(registro, acknowledgment);

        assertEquals(7L, versionAplicada());
        assertEquals(1, cantidadEventosProcesados());
        assertEquals(1L, cantidadCategorias());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void debeRevertirEventoYDatosSiFallaPersistenciaIncremental() {
        configurarIncremento(6L);
        when(redisPort.obtenerCambios(6L)).thenReturn(Optional.of(
                new CambiosCatalogo(6L, List.of(1L), List.of(10L), List.of())));
        Instant fecha = Instant.parse("2026-09-10T13:44:59Z");
        when(redisPort.obtenerProfesional(10L)).thenReturn(Optional.of(
                new Profesional(10L, 999L, "Ana", "Perez", true, fecha, fecha)));
        ConsumerRecord<String, String> registro = registro(6L);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        assertThrows(RuntimeException.class,
                () -> consumidor.consumir(registro, acknowledgment));

        assertEquals(5L, versionAplicada());
        assertEquals(0, cantidadEventosProcesados());
        assertEquals(0L, cantidadCategorias());
        verifyNoInteractions(acknowledgment, catalogoRemotoPort);

        when(redisPort.obtenerProfesional(10L)).thenReturn(Optional.of(
                new Profesional(10L, 1L, "Ana", "Perez", true, fecha, fecha)));
        consumidor.consumir(registro, acknowledgment);

        assertEquals(6L, versionAplicada());
        assertEquals(1, cantidadEventosProcesados());
        assertEquals(1L, cantidadCategorias());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void debeRecuperarSnapshotAnteDiscontinuidad() {
        configurarIncremento(7L);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(6L);
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(8L));

        procesarPort.procesar(new NotificacionCatalogo("evento-snapshot", 7L));

        assertEquals(8L, versionAplicada());
        assertEquals(1, cantidadEventosProcesados());
        verify(redisPort, never()).obtenerCambios(anyLong());
        verify(catalogoRemotoPort).obtenerSnapshot();
    }

    @Test
    void noDebeConfirmarSiSnapshotFallaAnteDiscontinuidad() {
        configurarIncremento(7L);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(6L);
        when(catalogoRemotoPort.obtenerSnapshot())
                .thenThrow(new IllegalStateException("Snapshot no disponible"));
        ConsumerRecord<String, String> registro = registro(7L);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        assertThrows(IllegalStateException.class,
                () -> consumidor.consumir(registro, acknowledgment));

        assertEquals(5L, versionAplicada());
        assertEquals(0, cantidadEventosProcesados());
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void debeRevertirSnapshotQueNoAlcanzaVersionNotificada() {
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(6L));
        ConsumerRecord<String, String> registro = registro(7L);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        assertThrows(IllegalStateException.class,
                () -> consumidor.consumir(registro, acknowledgment));

        assertNull(versionAplicada());
        assertEquals(0, cantidadEventosProcesados());
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void debeDeduplicarConUnaNuevaInstanciaDelServicioUsandoEstadoPersistido() {
        configurarIncremento(6L);
        NotificacionCatalogo notificacion = new NotificacionCatalogo("evento-persistido", 6L);
        procesarPort.procesar(notificacion);
        clearInvocations(redisPort);

        // Recrear el servicio sin compartir memoria del procesamiento anterior.
        var nuevoServicio = new ProcesarNotificacionCatalogoService(
                eventosPort, estadoPort, incrementalPort);
        new TransactionTemplate(transactionManager).executeWithoutResult(
                estado -> nuevoServicio.procesar(notificacion));

        assertEquals(6L, versionAplicada());
        assertEquals(1, cantidadEventosProcesados());
        assertEquals(1L, cantidadCategorias());
        verifyNoInteractions(redisPort, catalogoRemotoPort);
    }

    private void configurarIncremento(long versionRedis) {
        jdbcTemplate.update("UPDATE estado_sincronizacion SET applied_version = 5 WHERE id = 1");
        when(redisPort.obtenerVersionActual()).thenReturn(versionRedis);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(5L);
        when(redisPort.obtenerCambios(6L)).thenReturn(Optional.of(
                new CambiosCatalogo(6L, List.of(1L), List.of(), List.of())));
        Instant fecha = Instant.parse("2026-09-10T13:44:59Z");
        when(redisPort.obtenerCategoria(1L)).thenReturn(Optional.of(
                new Categoria(1L, "Categoria actualizada", null, true, fecha, fecha)));
    }

    private long cantidadCategorias() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categorias", Long.class);
    }

    private ConsumerRecord<String, String> registro(long version) {
        String mensaje = """
                {"eventId":"evento-reentrega", "eventType":"CatalogUpdated",
                 "newVersion":%d, "occurredAt":"2026-09-10T13:44:59Z",
                 "source":"MANUAL", "schemaVersion":1}
                """.formatted(version);
        return new ConsumerRecord<>("catalogo-prueba", 0, 0L, Long.toString(version), mensaje);
    }

    private SnapshotCatalogo snapshot(long version) {
        return new SnapshotCatalogo(
                version,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private long cantidadEventosProcesados() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM eventos_catalogo_procesados",
                Long.class
        );
    }

    private Long versionAplicada() {
        return jdbcTemplate.queryForObject(
                """
                SELECT applied_version
                FROM estado_sincronizacion
                WHERE id = 1
                """,
                Long.class
        );
    }

    @Test
    void debeEvitarProcesarDosVecesUnEventoConcurrente() throws Exception {
        when(catalogoRemotoPort.obtenerSnapshot())
                .thenReturn(snapshot(7L));

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-concurrente", 7L);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> primera = executor.submit(() -> {
                esperarInicio(inicio);
                procesarPort.procesar(notificacion);
            });

            Future<?> segunda = executor.submit(() -> {
                esperarInicio(inicio);
                procesarPort.procesar(notificacion);
            });

            inicio.countDown();

            primera.get(30, TimeUnit.SECONDS);
            segunda.get(30, TimeUnit.SECONDS);

            assertEquals(1, cantidadEventosProcesados());
            assertEquals(7L, versionAplicada());

            verify(catalogoRemotoPort, times(1))
                    .obtenerSnapshot();

        } finally {
            executor.shutdownNow();
        }
    }

    private void esperarInicio(CountDownLatch inicio) {
        try {
            inicio.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Se interrumpio la prueba de concurrencia", e
            );
        }
    }
}
