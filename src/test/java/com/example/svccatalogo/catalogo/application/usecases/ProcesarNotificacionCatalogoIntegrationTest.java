package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.infrastructure.security.JwtTestContextInitializer;
import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.infrastructure.kafka.CatalogoKafkaConsumerAdapter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.kafka.support.Acknowledgment;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
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
import java.util.stream.LongStream;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

@ContextConfiguration(initializers = JwtTestContextInitializer.class)
@SpringBootTest(properties = "catalogo.reconciliacion-inicio.enabled=false")
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

    @Autowired
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @Autowired
    private ReconciliarCatalogoPort reconciliarPort;

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
                eventosPort, estadoPort, incrementalPort, estadoRepository);
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

    @Test
    void reconciliaSinVersionLocalMedianteSnapshotSinRecibirKafka() {
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(7L));
        when(redisPort.obtenerVersionActual()).thenReturn(7L);

        reconciliarPort.reconciliarCatalogo();

        assertEquals(7L, versionAplicada());
        assertEquals(0, cantidadEventosProcesados());
        verify(catalogoRemotoPort).obtenerSnapshot();
        verify(redisPort, never()).obtenerCambios(anyLong());
    }

    @Test
    void reconciliaVersionAtrasadaConElIncrementalExistente() {
        configurarIncremento(6L);

        reconciliarPort.reconciliarCatalogo();

        assertEquals(6L, versionAplicada());
        assertEquals(1L, cantidadCategorias());
        assertEquals(0, cantidadEventosProcesados());
        verifyNoInteractions(catalogoRemotoPort);
    }

    @Test
    void reconciliaHistorialDiscontinuoMedianteSnapshot() {
        configurarIncremento(8L);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(6L);
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(8L));

        reconciliarPort.reconciliarCatalogo();

        assertEquals(8L, versionAplicada());
        verify(catalogoRemotoPort).obtenerSnapshot();
        verify(redisPort, never()).obtenerCambios(anyLong());
    }

    @Test
    void reconciliaCambiosFaltantesMedianteSnapshot() {
        configurarIncremento(7L);
        when(redisPort.obtenerCambios(6L)).thenReturn(Optional.empty());
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(7L));

        reconciliarPort.reconciliarCatalogo();

        assertEquals(7L, versionAplicada());
        verify(catalogoRemotoPort).obtenerSnapshot();
    }

    @Test
    void reconciliacionActualizadaNoModificaElEstadoNiLosDatos() {
        configurarIncremento(5L);
        jdbcTemplate.update("""
                UPDATE estado_sincronizacion SET applied_at = '2026-01-01T00:00:00Z',
                phase = 'SUCCESS', safe_diagnostic = 'Ultima version aplicada' WHERE id = 1
                """);
        var estadoAnterior = filaEstado();

        reconciliarPort.reconciliarCatalogo();

        assertEquals(estadoAnterior, filaEstado());
        assertEquals(0L, cantidadCategorias());
        verifyNoInteractions(catalogoRemotoPort);
        verify(redisPort, never()).obtenerVersionMasAntiguaDisponible();
    }

    @Test
    void falloTemporalRedisConservaLaVersionYPermiteUnaEjecucionPosterior() {
        configurarIncremento(6L);
        when(redisPort.obtenerVersionActual())
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("No disponible"))
                .thenReturn(6L);
        var estadoAnterior = filaEstado();

        assertThrows(RuntimeException.class, reconciliarPort::reconciliarCatalogo);

        assertEquals(estadoAnterior, filaEstado());
        assertEquals(0L, cantidadCategorias());
        verify(redisPort).obtenerVersionActual();

        reconciliarPort.reconciliarCatalogo();
        assertEquals(6L, versionAplicada());
    }

    @Test
    void falloRestNoAvanzaLaVersionNiRegistraExito() {
        when(catalogoRemotoPort.obtenerSnapshot()).thenThrow(new IllegalStateException("REST no disponible"));
        var estadoAnterior = filaEstado();

        assertThrows(IllegalStateException.class, reconciliarPort::reconciliarCatalogo);

        assertEquals(estadoAnterior, filaEstado());
        verify(catalogoRemotoPort).obtenerSnapshot();
        verifyNoInteractions(redisPort);
    }

    @Test
    void falloPostgreSQLRevierteElReemplazoYConservaElCatalogoAnterior() {
        configurarIncremento(6L);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(6L);
        jdbcTemplate.update("INSERT INTO categorias (id, name, enabled) VALUES (1, 'Categoria anterior', true)");
        var snapshotInvalido = new SnapshotCatalogo(6L,
                List.of(new Categoria(2L, "Nueva categoria", null, true, null, null)),
                List.of(new Profesional(10L, 999L, "Ana", "Perez", true, null, null)), List.of());
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshotInvalido);
        var estadoAnterior = filaEstado();

        assertThrows(RuntimeException.class, reconciliarPort::reconciliarCatalogo);

        assertEquals(estadoAnterior, filaEstado());
        assertEquals("Categoria anterior", jdbcTemplate.queryForObject("SELECT name FROM categorias", String.class));
        assertEquals(0, cantidadEventosProcesados());
    }

    @Test
    void snapshotAnteriorALaVersionPublicadaNoSeConfirma() {
        configurarIncremento(7L);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(6L);
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(6L));
        var estadoAnterior = filaEstado();

        assertThrows(IllegalStateException.class, reconciliarPort::reconciliarCatalogo);

        assertEquals(estadoAnterior, filaEstado());
    }

    @Test
    void versionLocalSuperiorARedisNoRetrocede() {
        configurarIncremento(4L);
        var estadoAnterior = filaEstado();

        assertThrows(IllegalStateException.class, reconciliarPort::reconciliarCatalogo);

        assertEquals(estadoAnterior, filaEstado());
        verifyNoInteractions(catalogoRemotoPort);
    }

    @Test
    void redisCambianteAgotaSoloTresIntentosYRecuperaSnapshot() {
        configurarIncremento(6L);
        when(redisPort.obtenerVersionActual()).thenReturn(6L, 7L, 8L, 9L, 10L, 11L, 12L);
        LongStream.rangeClosed(6, 10).forEach(version ->
                when(redisPort.obtenerCambios(version)).thenReturn(Optional.of(
                        new CambiosCatalogo(version, List.of(), List.of(), List.of()))));
        when(catalogoRemotoPort.obtenerSnapshot()).thenReturn(snapshot(12L));

        reconciliarPort.reconciliarCatalogo();

        assertEquals(12L, versionAplicada());
        verify(redisPort, times(7)).obtenerVersionActual();
        verify(catalogoRemotoPort).obtenerSnapshot();
    }

    @Test
    void arranqueYEventosDuplicadosConcurrentesCompartenBloqueoHastaElCommit() throws Exception {
        var leyendoSnapshot = new CountDownLatch(1);
        var liberarSnapshot = new CountDownLatch(1);
        when(catalogoRemotoPort.obtenerSnapshot()).thenAnswer(invocacion -> {
            leyendoSnapshot.countDown();
            if (!liberarSnapshot.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Se agoto la espera de la prueba");
            }
            return snapshot(7L);
        });
        when(redisPort.obtenerVersionActual()).thenReturn(7L);
        var ackPrimero = mock(Acknowledgment.class);
        var ackDuplicado = mock(Acknowledgment.class);
        var executor = Executors.newFixedThreadPool(3);
        try {
            var reconciliacion = executor.submit(reconciliarPort::reconciliarCatalogo);
            assertTrue(leyendoSnapshot.await(10, TimeUnit.SECONDS));
            var primero = executor.submit(() -> consumidor.consumir(registro(7L), ackPrimero));
            var duplicado = executor.submit(() -> consumidor.consumir(registro(7L), ackDuplicado));

            esperarTransaccionesBloqueadas(2);
            assertEquals(0, cantidadEventosProcesados());
            assertNull(versionAplicada());
            verifyNoInteractions(ackPrimero, ackDuplicado);

            liberarSnapshot.countDown();
            reconciliacion.get(20, TimeUnit.SECONDS);
            primero.get(20, TimeUnit.SECONDS);
            duplicado.get(20, TimeUnit.SECONDS);

            assertEquals(7L, versionAplicada());
            assertEquals(1, cantidadEventosProcesados());
            verify(ackPrimero).acknowledge();
            verify(ackDuplicado).acknowledge();
            verify(catalogoRemotoPort).obtenerSnapshot();
        } finally {
            liberarSnapshot.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void reconciliacionEsperaAlEventoKafkaQueYaEstaSincronizando() throws Exception {
        var leyendoSnapshot = new CountDownLatch(1);
        var liberarSnapshot = new CountDownLatch(1);
        when(catalogoRemotoPort.obtenerSnapshot()).thenAnswer(invocacion -> {
            leyendoSnapshot.countDown();
            if (!liberarSnapshot.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Se agoto la espera de la prueba");
            }
            return snapshot(7L);
        });
        when(redisPort.obtenerVersionActual()).thenReturn(7L);
        var acknowledgment = mock(Acknowledgment.class);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var evento = executor.submit(() -> consumidor.consumir(registro(7L), acknowledgment));
            assertTrue(leyendoSnapshot.await(10, TimeUnit.SECONDS));
            var reconciliacion = executor.submit(reconciliarPort::reconciliarCatalogo);

            esperarTransaccionesBloqueadas(1);
            verifyNoInteractions(acknowledgment);
            liberarSnapshot.countDown();
            evento.get(20, TimeUnit.SECONDS);
            reconciliacion.get(20, TimeUnit.SECONDS);

            assertEquals(7L, versionAplicada());
            assertEquals(1, cantidadEventosProcesados());
            verify(acknowledgment).acknowledge();
            verify(catalogoRemotoPort).obtenerSnapshot();
        } finally {
            liberarSnapshot.countDown();
            executor.shutdownNow();
        }
    }

    private void esperarTransaccionesBloqueadas(int cantidad) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_stat_activity
                WHERE datname = current_database() AND wait_event_type = 'Lock'
                AND query LIKE '%estado_sincronizacion%'
                """, Integer.class) >= cantidad);
    }

    private String filaEstado() {
        return jdbcTemplate.queryForObject("SELECT row_to_json(e)::text FROM estado_sincronizacion e", String.class);
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
