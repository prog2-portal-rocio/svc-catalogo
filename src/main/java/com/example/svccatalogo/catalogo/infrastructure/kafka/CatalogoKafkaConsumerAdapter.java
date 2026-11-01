package com.example.svccatalogo.catalogo.infrastructure.kafka;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Component
public class CatalogoKafkaConsumerAdapter {

    private final ProcesarNotificacionCatalogoUseCase procesarPort;
    private final JsonMapper jsonMapper;

    public CatalogoKafkaConsumerAdapter(
            ProcesarNotificacionCatalogoUseCase procesarPort,
            JsonMapper jsonMapper) {

        this.procesarPort = Objects.requireNonNull(procesarPort);
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
    }

    @KafkaListener(topics = "${catalogo.kafka.topic}")
    public void consumir(
            ConsumerRecord<String, String> registro,
            Acknowledgment acknowledgment) {

        NotificacionCatalogo notificacion =
                convertirNotificacion(registro.value());

        String versionEsperada =
                String.valueOf(notificacion.nuevaVersion());

        if (!versionEsperada.equals(registro.key())) {
            throw new IllegalArgumentException(
                    "La clave Kafka no coincide con la version notificada"
            );
        }

        procesarPort.procesar(notificacion);

        acknowledgment.acknowledge();
    }

    private NotificacionCatalogo convertirNotificacion(String mensaje) {
        try {
            JsonNode contenido = jsonMapper.readTree(mensaje);

            if (contenido == null || !contenido.isObject()) {
                throw new IllegalArgumentException(
                        "La notificacion Kafka debe ser un objeto JSON"
                );
            }

            JsonNode tipo = contenido.path("eventType");
            JsonNode esquema = contenido.path("schemaVersion");
            JsonNode identificador = contenido.path("eventId");
            JsonNode version = contenido.path("newVersion");
            JsonNode origen = contenido.path("source");
            JsonNode fecha = contenido.path("occurredAt");

            if (!tipo.isString()
                    || !"CatalogUpdated".equals(tipo.asString())) {
                throw new IllegalArgumentException(
                        "Tipo de evento Kafka no valido"
                );
            }

            if (!esquema.isIntegralNumber()
                    || !esquema.canConvertToLong()
                    || esquema.longValue() != 1L) {
                throw new IllegalArgumentException(
                        "Version del esquema Kafka no soportada"
                );
            }

            if (!identificador.isString()
                    || identificador.asString().isBlank()
                    || identificador.asString().length() > 255) {
                throw new IllegalArgumentException(
                        "Identificador del evento Kafka no valido"
                );
            }

            if (!version.isIntegralNumber()
                    || !version.canConvertToLong()
                    || version.longValue() < 0) {
                throw new IllegalArgumentException(
                        "Version del catalogo no valida"
                );
            }

            if (!origen.isString() || origen.asString().isBlank()) {
                throw new IllegalArgumentException(
                        "Origen del evento Kafka no valido"
                );
            }

            if (!fecha.isString()) {
                throw new IllegalArgumentException(
                        "Fecha del evento Kafka no valida"
                );
            }

            try {
                Instant.parse(fecha.asString());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Fecha del evento Kafka no valida", e
                );
            }

            return new NotificacionCatalogo(
                    identificador.asString(),
                    version.longValue()
            );

        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "El mensaje Kafka no contiene un JSON valido", e
            );
        }
    }
}
