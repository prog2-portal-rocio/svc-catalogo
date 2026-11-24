package com.example.svccatalogo.catalogo.infrastructure.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.listener.MessageListenerContainer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecuperacionCatalogoKafkaErrorHandlerTest {
    @Test
    void errorTransitorioSeReentregaYSeDetieneAlAgotarElLimiteSinDescartar() {
        var handler = new RecuperacionCatalogoKafkaErrorHandler(1, 2);
        Consumer<?, ?> consumer = mock(Consumer.class);
        var container = mock(MessageListenerContainer.class);
        List<ConsumerRecord<?, ?>> registros = List.of(new ConsumerRecord<>("catalogo", 0, 0L, "1", "{}"));
        var error = new TransientDataAccessResourceException("No disponible");
        assertThrows(RuntimeException.class, () -> handler.handleRemaining(error, registros, consumer, container));
        assertThrows(RuntimeException.class, () -> handler.handleRemaining(error, registros, consumer, container));
        verify(container, never()).stopAbnormally(any());
        assertThrows(RuntimeException.class, () -> handler.handleRemaining(error, registros, consumer, container));
        verify(container, timeout(1000)).stopAbnormally(any());
        assertFalse(handler.isAckAfterHandle());
    }

    @Test
    void mensajeInvalidoNoRecibeReintentosAutomaticos() {
        var handler = new RecuperacionCatalogoKafkaErrorHandler(1, 3);
        Consumer<?, ?> consumer = mock(Consumer.class);
        var container = mock(MessageListenerContainer.class);
        List<ConsumerRecord<?, ?>> registros = List.of(new ConsumerRecord<>("catalogo", 0, 0L, "1", "{}"));
        var error = new IllegalArgumentException("JSON invalido");
        assertThrows(RuntimeException.class, () -> handler.handleRemaining(error, registros, consumer, container));
        verify(container, timeout(1000)).stopAbnormally(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 11, Long.MAX_VALUE})
    void noPermiteReintentosIlimitados(long reintentos) {
        assertThrows(IllegalArgumentException.class, () -> new RecuperacionCatalogoKafkaErrorHandler(1, reintentos));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 10001})
    void intervaloDeEsperaAcotado(long intervalo) {
        assertThrows(IllegalArgumentException.class, () -> new RecuperacionCatalogoKafkaErrorHandler(intervalo, 3));
    }
}
