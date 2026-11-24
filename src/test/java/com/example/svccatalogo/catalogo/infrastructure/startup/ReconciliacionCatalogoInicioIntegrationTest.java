package com.example.svccatalogo.catalogo.infrastructure.startup;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import com.example.svccatalogo.catalogo.infrastructure.security.JwtTestContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "catalogo.reconciliacion-inicio.enabled=true",
        "catalogo.reconciliacion-periodica.enabled=true",
        "catalogo.reconciliacion-periodica.intervalo-ms=50",
        "catalogo.reconciliacion-periodica.demora-inicial-ms=50",
        "spring.kafka.listener.auto-startup=false"
})
@ContextConfiguration(initializers = JwtTestContextInitializer.class)
@Import(ReconciliacionCatalogoInicioIntegrationTest.FuentesLocales.class)
@Testcontainers
@DirtiesContext
class ReconciliacionCatalogoInicioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CatalogoRemotoPort remoto;

    @Autowired
    private CatalogoRedisPort redis;

    @Test
    void applicationReadyYReconciliacionPeriodicaRecuperanSinKafka() {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(7L, jdbc.queryForObject(
                        "SELECT applied_version FROM estado_sincronizacion WHERE id = 1", Long.class)));

        assertEquals("SUCCESS", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class));
        assertEquals("Categoria inicial", jdbc.queryForObject("SELECT name FROM categorias", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eventos_catalogo_procesados", Integer.class));
        verify(remoto).obtenerSnapshot();
        when(redis.obtenerVersionMasAntiguaDisponible()).thenReturn(7L);
        when(redis.obtenerCambios(8L)).thenReturn(Optional.of(
                new CambiosCatalogo(8L, List.of(1L), List.of(), List.of())));
        when(redis.obtenerCategoria(1L)).thenReturn(Optional.of(
                new Categoria(1L, "Cambio sin Kafka", null, true, null, null)));
        when(redis.obtenerVersionActual()).thenReturn(8L);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(8L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class));
            assertEquals("Cambio sin Kafka", jdbc.queryForObject("SELECT name FROM categorias", String.class));
        });
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eventos_catalogo_procesados", Integer.class));
        var fechaAplicada = jdbc.queryForObject("SELECT applied_at FROM estado_sincronizacion", Timestamp.class);
        doThrow(new RedisConnectionFailureException("Fallo transitorio simulado"))
                .when(redis).obtenerVersionActual();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals("ERROR", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class)));
        assertEquals(8L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class));
        assertEquals(fechaAplicada, jdbc.queryForObject("SELECT applied_at FROM estado_sincronizacion", Timestamp.class));
        doReturn(8L).when(redis).obtenerVersionActual();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals("SUCCESS", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class)));
        // Coincidencia de versiones: no reescribe catálogo ni la fecha de su aplicación.
        assertEquals(fechaAplicada, jdbc.queryForObject("SELECT applied_at FROM estado_sincronizacion", Timestamp.class));
        doThrow(new ResourceAccessException("REST temporalmente inaccesible"))
                .when(remoto).obtenerSnapshot();
        doReturn(9L).when(redis).obtenerVersionActual();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals("ERROR", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class)));
        assertEquals(8L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class));
        assertEquals("Cambio sin Kafka", jdbc.queryForObject("SELECT name FROM categorias", String.class));
        doReturn(new SnapshotCatalogo(9L, List.of(new Categoria(1L, "Recuperado", null, true, null, null)), List.of(), List.of()))
                .when(remoto).obtenerSnapshot();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(9L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class)));
        // Error dentro de PostgreSQL: el snapshot viola una FK, y todo el reemplazo debe revertirse.
        doReturn(new SnapshotCatalogo(10L, List.of(new Categoria(1L, "No debe persistir", null, true, null, null)),
                List.of(new Profesional(10L, 999L, "Ana", "Perez", true, null, null)),
                List.of())).when(remoto).obtenerSnapshot();
        doReturn(10L).when(redis).obtenerVersionActual();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals("ERROR", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class)));
        assertEquals(9L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class));
        assertEquals("Recuperado", jdbc.queryForObject("SELECT name FROM categorias", String.class));
        doReturn(new SnapshotCatalogo(10L, List.of(new Categoria(1L, "Tras rollback", null, true, null, null)), List.of(), List.of()))
                .when(remoto).obtenerSnapshot();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(10L, jdbc.queryForObject("SELECT applied_version FROM estado_sincronizacion", Long.class)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FuentesLocales {

        @Bean
        @Primary
        CatalogoRemotoPort remotoDePrueba() {
            var remoto = mock(CatalogoRemotoPort.class);
            when(remoto.obtenerSnapshot()).thenReturn(new SnapshotCatalogo(7L,
                    List.of(new Categoria(1L, "Categoria inicial", null, true, null, null)), List.of(), List.of()));
            return remoto;
        }

        @Bean
        @Primary
        CatalogoRedisPort redisDePrueba() {
            var redis = mock(CatalogoRedisPort.class);
            when(redis.obtenerVersionActual()).thenReturn(7L);
            return redis;
        }
    }
}
