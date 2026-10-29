package com.example.svccatalogo.catalogo.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoRedisAdapterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> operaciones;
    private CatalogoRedisAdapter adapter;

    @BeforeEach
    void configurar() {
        redisTemplate = mock(StringRedisTemplate.class);
        operaciones = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(operaciones);

        adapter = new CatalogoRedisAdapter(redisTemplate);
    }

    @Test
    void debeObtenerVersionActual() {
        when(operaciones.get("catedra:sync:current-version"))
                .thenReturn("7");

        long version = adapter.obtenerVersionActual();

        assertEquals(7L, version);
    }

    @Test
    void debeObtenerVersionMasAntiguaDisponible() {
        when(operaciones.get("catedra:sync:oldest-available-version"))
                .thenReturn("4");

        long version = adapter.obtenerVersionMasAntiguaDisponible();

        assertEquals(4L, version);
    }

    @Test
    void debeRechazarVersionInvalida() {
        when(operaciones.get("catedra:sync:current-version"))
                .thenReturn("abc");

        assertThrows(
                IllegalStateException.class,
                () -> adapter.obtenerVersionActual()
        );
    }

    @Test
    void debeDevolverVacioCuandoNoExistenCambios() {
        when(operaciones.get("catedra:sync:changes:5"))
                .thenReturn(null);

        Optional<String> cambios = adapter.obtenerCambios(5L);

        assertTrue(cambios.isEmpty());
    }

    @Test
    void debeObtenerCambiosDeUnaVersion() {
        String json = """
            {
              "version": 7,
              "categoryIds": [1, 2]
            }
            """;

        when(operaciones.get("catedra:sync:changes:7"))
                .thenReturn(json);

        Optional<String> cambios = adapter.obtenerCambios(7L);

        assertTrue(cambios.isPresent());
        assertEquals(json, cambios.get());
    }

    @Test
    void debeRechazarVersionNegativaAlConsultarCambios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.obtenerCambios(-1L)
        );

        verify(operaciones, never()).get(anyString());
    }
}
