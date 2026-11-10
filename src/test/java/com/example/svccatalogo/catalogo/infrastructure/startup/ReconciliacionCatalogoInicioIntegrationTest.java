package com.example.svccatalogo.catalogo.infrastructure.startup;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "catalogo.reconciliacion-inicio.enabled=true",
        "spring.kafka.listener.auto-startup=false"
})
@ContextConfiguration(initializers = JwtTestContextInitializer.class)
@Import(ReconciliacionCatalogoInicioIntegrationTest.FuentesLocales.class)
@Testcontainers
class ReconciliacionCatalogoInicioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CatalogoRemotoPort remoto;

    @Test
    void applicationReadyReconciliaAutomaticamenteSinNingunEventoKafka() {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(7L, jdbc.queryForObject(
                        "SELECT applied_version FROM estado_sincronizacion WHERE id = 1", Long.class)));

        assertEquals("SUCCESS", jdbc.queryForObject("SELECT phase FROM estado_sincronizacion", String.class));
        assertEquals("Categoria inicial", jdbc.queryForObject("SELECT name FROM categorias", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM eventos_catalogo_procesados", Integer.class));
        verify(remoto).obtenerSnapshot();
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
