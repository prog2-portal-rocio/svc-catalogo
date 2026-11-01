package com.example.svccatalogo.catalogo.infrastructure.kafka;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoKafkaConsumerAdapterTest {

    private static final String MENSAJE_VALIDO = """
            {
              "eventId": "evento-123",
              "eventType": "CatalogUpdated",
              "newVersion": 7,
              "occurredAt": "2026-09-10T13:44:59Z",
              "source": "MANUAL",
              "schemaVersion": 1
            }
            """;

    private ProcesarNotificacionCatalogoUseCase procesarPort;
    private Acknowledgment acknowledgment;
    private CatalogoKafkaConsumerAdapter adapter;

    @BeforeEach
    void configurar() {
        procesarPort = mock(ProcesarNotificacionCatalogoUseCase.class);
        acknowledgment = mock(Acknowledgment.class);

        adapter = new CatalogoKafkaConsumerAdapter(
                procesarPort,
                JsonMapper.builder().build()
        );
    }

    @Test
    void debeProcesarMensajeValidoYConfirmarlo() {
        adapter.consumir(registro(MENSAJE_VALIDO), acknowledgment);

        var orden = inOrder(procesarPort, acknowledgment);

        orden.verify(procesarPort).procesar(
                new NotificacionCatalogo("evento-123", 7L)
        );

        orden.verify(acknowledgment).acknowledge();
    }

    @Test
    void debeAceptarCamposAdicionales() {
        String mensaje = MENSAJE_VALIDO.replace(
                "\"schemaVersion\": 1",
                "\"schemaVersion\": 1, \"campoExtra\": \"permitido\""
        );

        adapter.consumir(registro(mensaje), acknowledgment);

        verify(procesarPort).procesar(
                new NotificacionCatalogo("evento-123", 7L)
        );

        verify(acknowledgment).acknowledge();
    }

    @Test
    void noDebeConfirmarSiFallaElProcesamiento() {
        doThrow(new IllegalStateException("Error al sincronizar"))
                .when(procesarPort)
                .procesar(any(NotificacionCatalogo.class));

        assertThrows(
                IllegalStateException.class,
                () -> adapter.consumir(
                        registro(MENSAJE_VALIDO),
                        acknowledgment
                )
        );

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void debeRechazarClaveKafkaDistintaDeLaVersion() {
        ConsumerRecord<String, String> registro =
                new ConsumerRecord<>(
                        "topic-prueba",
                        0,
                        0L,
                        "8",
                        MENSAJE_VALIDO
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.consumir(registro, acknowledgment)
        );

        verifyNoInteractions(procesarPort, acknowledgment);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mensajesInvalidos")
    void debeRechazarMensajesInvalidos(
            String descripcion,
            String mensaje) {

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.consumir(
                        registro(mensaje),
                        acknowledgment
                )
        );

        verifyNoInteractions(procesarPort, acknowledgment);
    }

    private ConsumerRecord<String, String> registro(String mensaje) {
        return new ConsumerRecord<>(
                "topic-prueba",
                0,
                0L,
                "7",
                mensaje
        );
    }

    private static Stream<Arguments> mensajesInvalidos() {
        return Stream.of(
                Arguments.of(
                        "JSON mal formado",
                        "esto no es JSON"
                ),
                Arguments.of(
                        "JSON no es objeto",
                        "[]"
                ),
                Arguments.of(
                        "Tipo incorrecto",
                        MENSAJE_VALIDO.replace(
                                "\"CatalogUpdated\"",
                                "\"OtroEvento\""
                        )
                ),
                Arguments.of(
                        "Esquema incorrecto",
                        MENSAJE_VALIDO.replace(
                                "\"schemaVersion\": 1",
                                "\"schemaVersion\": 2"
                        )
                ),
                Arguments.of(
                        "Identificador vacio",
                        MENSAJE_VALIDO.replace(
                                "\"evento-123\"",
                                "\"  \""
                        )
                ),
                Arguments.of(
                        "Version negativa",
                        MENSAJE_VALIDO.replace(
                                "\"newVersion\": 7",
                                "\"newVersion\": -1"
                        )
                ),
                Arguments.of(
                        "Origen faltante",
                        MENSAJE_VALIDO.replace(
                                "\"source\": \"MANUAL\",",
                                ""
                        )
                ),
                Arguments.of(
                        "Fecha incorrecta",
                        MENSAJE_VALIDO.replace(
                                "2026-09-10T13:44:59Z",
                                "fecha-invalida"
                        )
                )
        );
    }
}
