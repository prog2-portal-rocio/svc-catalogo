package com.example.svccatalogo.catalogo.infrastructure.kafka;

import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "catalogo.kafka.topic=catalogo-prueba",
        "spring.kafka.consumer.group-id=catalogo-integracion-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@Testcontainers
@Import(CatalogoKafkaConsumerIntegrationTest.KafkaTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CatalogoKafkaConsumerIntegrationTest {

    private static final String TOPIC = "catalogo-prueba";
    private static final String GROUP = "catalogo-integracion-test";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @MockitoBean
    private CatalogoRedisPort redisPort;

    @MockitoBean
    private CatalogoRemotoPort remotoPort;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararBaseDeDatos() {
        ContainerTestUtils.waitForAssignment(contenedor(), 1);
        jdbcTemplate.execute("TRUNCATE TABLE eventos_catalogo_procesados");
        jdbcTemplate.execute("TRUNCATE TABLE horarios_semanales, profesionales, categorias");
        jdbcTemplate.update("UPDATE estado_sincronizacion SET applied_version = 5 WHERE id = 1");
    }

    @Test
    @Order(1)
    void debeSincronizarPendientesYDescartarDuplicadosYVersionesAtrasadas() throws Exception {
        configurarIncrementos(7L);

        // La notificacion es de la version 6, pero Redis ya publica la 7.
        long offset = publicar("evento-nuevo", 6L);
        esperarConfirmacion(offset);

        assertEquals(7L, versionAplicada());
        assertEquals(1L, cantidadEventos());
        assertEquals("Categoria actualizada", jdbcTemplate.queryForObject(
                "SELECT name FROM categorias WHERE id = 1", String.class));
        var orden = inOrder(redisPort);
        orden.verify(redisPort).obtenerVersionActual();
        orden.verify(redisPort).obtenerVersionMasAntiguaDisponible();
        orden.verify(redisPort).obtenerCambios(6L);
        orden.verify(redisPort).obtenerCambios(7L);
        verify(redisPort, times(1)).obtenerCategoria(1L);
        verifyNoInteractions(remotoPort);
        clearInvocations(redisPort);

        // Reiniciar el listener preserva la deduplicacion y la version en PostgreSQL.
        detenerConsumidor();
        contenedor().start();
        ContainerTestUtils.waitForAssignment(contenedor(), 1);

        esperarConfirmacion(publicar("evento-nuevo", 6L));
        esperarConfirmacion(publicar("evento-atrasado", 5L));
        esperarConfirmacion(publicar("evento-ya-aplicado", 7L));
        esperarConfirmacion(publicar("evento-fuera-de-orden", 6L));

        assertEquals(7L, versionAplicada());
        assertEquals(4L, cantidadEventos());
        verifyNoInteractions(redisPort, remotoPort);
    }

    @Test
    @Order(2)
    void debeReentregarTrasFalloDeRedisYReinicioSinConfirmarPrematuramente() throws Exception {
        configurarIncrementos(6L);
        when(redisPort.obtenerVersionActual())
                .thenThrow(new IllegalStateException("Redis no disponible"))
                .thenReturn(6L);

        long offset = publicar("evento-recuperable", 6L);
        await().atMost(20, TimeUnit.SECONDS).until(() -> !contenedor().isRunning());
        assertOffsetSinConfirmar(offset);
        assertEquals(5L, versionAplicada());
        assertEquals(0L, cantidadEventos());
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categorias", Long.class));

        contenedor().start();
        ContainerTestUtils.waitForAssignment(contenedor(), 1);
        esperarConfirmacion(offset);

        assertTrue(contenedor().isRunning());
        assertEquals(6L, versionAplicada());
        assertEquals(1L, cantidadEventos());
        verify(redisPort, times(3)).obtenerVersionActual();
        verifyNoInteractions(remotoPort);
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void debeDetenerConsumidorAnteJsonInvalidoSinConfirmarOffset() throws Exception {
        long offset = kafkaTemplate.send(TOPIC, "9", "esto no es JSON")
                .get(15, TimeUnit.SECONDS).getRecordMetadata().offset();

        await().atMost(20, TimeUnit.SECONDS).until(() -> !contenedor().isRunning());

        assertOffsetSinConfirmar(offset);
        assertEquals(0L, cantidadEventos());
        assertEquals(5L, versionAplicada());
        verifyNoInteractions(redisPort, remotoPort);
    }

    private void configurarIncrementos(long version) {
        when(redisPort.obtenerVersionActual()).thenReturn(version);
        when(redisPort.obtenerVersionMasAntiguaDisponible()).thenReturn(5L);
        when(redisPort.obtenerCambios(6L)).thenReturn(Optional.of(
                new CambiosCatalogo(6L, List.of(1L), List.of(), List.of())));
        when(redisPort.obtenerCambios(7L)).thenReturn(Optional.of(
                new CambiosCatalogo(7L, List.of(1L), List.of(), List.of())));
        Instant fecha = Instant.parse("2026-09-10T13:44:59Z");
        when(redisPort.obtenerCategoria(1L)).thenReturn(Optional.of(
                new Categoria(1L, "Categoria actualizada", null, true, fecha, fecha)));
    }

    private long publicar(String eventId, long version) throws Exception {
        String mensaje = """
                {"eventId":"%s", "eventType":"CatalogUpdated", "newVersion":%d,
                 "occurredAt":"2026-09-10T13:44:59Z", "source":"MANUAL", "schemaVersion":1}
                """.formatted(eventId, version);
        return kafkaTemplate.send(TOPIC, Long.toString(version), mensaje)
                .get(15, TimeUnit.SECONDS).getRecordMetadata().offset();
    }

    private void esperarConfirmacion(long offset) {
        try (AdminClient admin = admin()) {
            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
                var metadata = admin.listConsumerGroupOffsets(GROUP)
                        .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS)
                        .get(new TopicPartition(TOPIC, 0));
                assertNotNull(metadata);
                assertEquals(offset + 1, metadata.offset());
            });
        }
    }

    private void assertOffsetSinConfirmar(long offset) throws Exception {
        try (AdminClient admin = admin()) {
            var metadata = admin.listConsumerGroupOffsets(GROUP)
                    .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS)
                    .get(new TopicPartition(TOPIC, 0));
            assertTrue(metadata == null || metadata.offset() <= offset);
        }
    }

    private AdminClient admin() {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    }

    private MessageListenerContainer contenedor() {
        return listenerRegistry.getListenerContainers().iterator().next();
    }

    private void detenerConsumidor() {
        contenedor().stop();
        assertFalse(contenedor().isRunning());
    }

    private Long versionAplicada() {
        return jdbcTemplate.queryForObject(
                "SELECT applied_version FROM estado_sincronizacion WHERE id = 1", Long.class);
    }

    private long cantidadEventos() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eventos_catalogo_procesados", Long.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTestConfig {
        @Bean
        NewTopic topicDePrueba() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }
    }
}
