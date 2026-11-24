package com.example.svccatalogo.catalogo.infrastructure.kafka;

import com.example.svccatalogo.catalogo.application.exception.FuenteCatalogoNoDisponibleException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.Assert;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reentrega acotada sin descartar registros; ante agotamiento conserva la parada segura. */
public class RecuperacionCatalogoKafkaErrorHandler extends DefaultErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionCatalogoKafkaErrorHandler.class);
    private final ThreadLocal<Runnable> parada;
    private final CommonContainerStoppingErrorHandler detener = new CommonContainerStoppingErrorHandler();

    public RecuperacionCatalogoKafkaErrorHandler(long intervalo, long reintentos) {
        this(intervalo, reintentos, new ThreadLocal<>());
    }

    private RecuperacionCatalogoKafkaErrorHandler(long intervalo, long reintentos, ThreadLocal<Runnable> parada) {
        // El recoverer nunca descarta/recupera un registro: detiene el contenedor y lanza excepción.
        super((registro, error) -> Objects.requireNonNull(parada.get(), "Falta contexto de parada Kafka").run(), backoff(intervalo, reintentos));
        this.parada = parada;
        setClassifications(Map.of(
                TransientDataAccessException.class, true,
                DataAccessResourceFailureException.class, true,
                FuenteCatalogoNoDisponibleException.class, true,
                CannotCreateTransactionException.class, true,
                TransactionSystemException.class, true,
                TransactionTimedOutException.class, true,
                ResourceAccessException.class, true,
                HttpServerErrorException.class, true), false);
        setResetStateOnExceptionChange(false);
        setResetStateOnRecoveryFailure(false);
        setCommitRecovered(false);
        setAckAfterHandle(false);
        setRetryListeners((registro, error, intento) ->
                LOG.warn("Reentrega de catalogo: particion={}, offset={}, intento={}",
                        registro.partition(), registro.offset(), intento));
    }

    private static FixedBackOff backoff(long intervalo, long reintentos) {
        Assert.isTrue(intervalo > 0 && intervalo <= 10000, "Intervalo Kafka entre 1 y 10000 ms");
        Assert.isTrue(reintentos >= 0 && reintentos <= 10, "Reintentos Kafka entre 0 y 10");
        return new FixedBackOff(intervalo, reintentos);
    }

    @Override
    public void handleRemaining(Exception error, List<ConsumerRecord<?, ?>> registros,
                                Consumer<?, ?> consumer, MessageListenerContainer container) {
        parada.set(() -> {
            LOG.error("Consumidor de catalogo detenido sin confirmar; requiere intervencion tras error permanente o reintentos agotados");
            detener.handleRemaining(new IllegalStateException("No se pudo procesar la notificacion de catalogo"),
                    registros, consumer, container);
        });
        try {
            super.handleRemaining(error, registros, consumer, container);
        } finally {
            parada.remove();
        }
    }

    @Override
    public void handleOtherException(Exception error, Consumer<?, ?> consumer,
                                     MessageListenerContainer container, boolean batchListener) {
        LOG.error("Consumidor de catalogo detenido por fallo sin registro; requiere intervencion");
        detener.handleOtherException(new IllegalStateException("Fallo de consumo sin registro"), consumer, container, batchListener);
    }
}
