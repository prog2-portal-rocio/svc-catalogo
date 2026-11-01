package com.example.svccatalogo.catalogo.infrastructure.kafka;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

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

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer("apache/kafka:4.1.0");

    @MockitoBean
    private ProcesarNotificacionCatalogoUseCase procesarPort;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Test
    void debeRecibirEventoKafkaYProcesarNotificacion() throws Exception {

        // Esperamos a que el consumidor tenga una particion asignada.
        var contenedor = listenerRegistry
                .getListenerContainers()
                .iterator()
                .next();

        ContainerTestUtils.waitForAssignment(contenedor, 1);

        String mensaje = """
                {
                  "eventId": "evento-kafka-integracion-1",
                  "eventType": "CatalogUpdated",
                  "newVersion": 7,
                  "occurredAt": "2026-09-10T13:44:59Z",
                  "source": "MANUAL",
                  "schemaVersion": 1
                }
                """;

        // Publicamos el evento en nuestro Kafka temporal.
        kafkaTemplate.send(
                "catalogo-prueba",
                "7",
                mensaje
        ).get(15, TimeUnit.SECONDS);

        // Verificamos que el consumidor invoque el caso de uso.
        verify(procesarPort, timeout(20000)).procesar(
                new NotificacionCatalogo(
                        "evento-kafka-integracion-1",
                        7L
                )
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTestConfig {

        @Bean
        NewTopic topicDePrueba() {
            return TopicBuilder.name("catalogo-prueba")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }

    @Test
    @Order(2)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void debeDetenerConsumidorSiFallaProcesamiento() throws Exception {

        var contenedor = listenerRegistry
                .getListenerContainers()
                .iterator()
                .next();

        ContainerTestUtils.waitForAssignment(contenedor, 1);

        NotificacionCatalogo notificacion =
                new NotificacionCatalogo("evento-kafka-error-1", 8L);

        // Simulamos un fallo de sincronizacion.
        doThrow(new IllegalStateException("Error de sincronizacion"))
                .when(procesarPort)
                .procesar(notificacion);

        String mensaje = """
            {
              "eventId": "evento-kafka-error-1",
              "eventType": "CatalogUpdated",
              "newVersion": 8,
              "occurredAt": "2026-09-10T13:45:00Z",
              "source": "MANUAL",
              "schemaVersion": 1
            }
            """;

        kafkaTemplate.send(
                "catalogo-prueba",
                "8",
                mensaje
        ).get(15, TimeUnit.SECONDS);

        // Confirmo que Kafka entrego el evento.
        verify(procesarPort, timeout(20000))
                .procesar(notificacion);

        // Espero a que el manejador de errores detenga el consumidor.
        long limite = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(15);

        while (contenedor.isRunning()
                && System.nanoTime() < limite) {
            Thread.sleep(100);
        }

        assertFalse(
                contenedor.isRunning(),
                "El consumidor deberia detenerse ante el error"
        );
    }
}
