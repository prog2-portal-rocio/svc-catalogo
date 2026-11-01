package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EventoCatalogoProcesadoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventoCatalogoProcesadoAdapterTest {

    private EventoCatalogoProcesadoJpaRepository repository;
    private EventoCatalogoProcesadoAdapter adapter;

    @BeforeEach
    void configurar() {
        repository = mock(EventoCatalogoProcesadoJpaRepository.class);
        adapter = new EventoCatalogoProcesadoAdapter(repository);
    }

    @ParameterizedTest
    @CsvSource({
            "1, true",
            "0, false"
    })
    void debeIndicarSiElEventoEsNuevo(
            int filasInsertadas,
            boolean esperado) {

        when(repository.insertarSiNoExiste("evento-123", 7L))
                .thenReturn(filasInsertadas);

        boolean resultado =
                adapter.registrarSiNoExiste("evento-123", 7L);

        assertEquals(esperado, resultado);

        verify(repository).insertarSiNoExiste("evento-123", 7L);
    }

    @Test
    void debeRechazarIdentificadorNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.registrarSiNoExiste(null, 7L)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarIdentificadorVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.registrarSiNoExiste("  ", 7L)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarIdentificadorDemasiadoLargo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.registrarSiNoExiste("a".repeat(256), 7L)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarVersionNegativa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.registrarSiNoExiste("evento-123", -1L)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void debeRechazarResultadoInesperadoDelRepositorio() {
        when(repository.insertarSiNoExiste("evento-123", 7L))
                .thenReturn(2);

        assertThrows(
                IllegalStateException.class,
                () -> adapter.registrarSiNoExiste("evento-123", 7L)
        );
    }
}
