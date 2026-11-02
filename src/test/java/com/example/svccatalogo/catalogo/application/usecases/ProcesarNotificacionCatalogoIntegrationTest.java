package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
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

    @Autowired
    private ProcesarNotificacionCatalogoUseCase procesarPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
