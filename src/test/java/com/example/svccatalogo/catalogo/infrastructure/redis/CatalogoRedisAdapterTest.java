package com.example.svccatalogo.catalogo.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;
import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import java.util.List;
import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.Optional;
import com.example.svccatalogo.catalogo.application.exception.FuenteCatalogoNoDisponibleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

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

        adapter = new CatalogoRedisAdapter(
                redisTemplate,
                JsonMapper.builder().build()
        );
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

        Optional<CambiosCatalogo> cambios = adapter.obtenerCambios(5L);

        assertTrue(cambios.isEmpty());
    }


    @Test
    void debeObtenerCambiosDeUnaVersion() {
        String json = """
            {
              "version": 7,
              "changes": {
                "professionalCategories": [1, 2],
                "professionals": [15],
                "weeklySchedules": [21, 22]
              }
            }
            """;

        when(operaciones.get("catedra:sync:changes:7"))
                .thenReturn(json);

        Optional<CambiosCatalogo> cambios = adapter.obtenerCambios(7L);

        assertTrue(cambios.isPresent());

        CambiosCatalogo resultado = cambios.orElseThrow();

        assertEquals(7L, resultado.version());
        assertEquals(List.of(1L, 2L), resultado.categorias());
        assertEquals(List.of(15L), resultado.profesionales());
        assertEquals(List.of(21L, 22L), resultado.horariosSemanales());
    }

    @Test
    void debeRechazarVersionNegativaAlConsultarCambios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.obtenerCambios(-1L)
        );

        verify(operaciones, never()).get(anyString());
    }

    @Test
    void debeTraducirErrorCuandoRedisNoEstaDisponible() {
        when(operaciones.get("catedra:sync:current-version"))
                .thenThrow(new RedisConnectionFailureException(
                        "No se pudo conectar a Redis"
                ));

        FuenteCatalogoNoDisponibleException excepcion = assertThrows(
                FuenteCatalogoNoDisponibleException.class,
                () -> adapter.obtenerVersionActual()
        );

        assertInstanceOf(
                RedisConnectionFailureException.class,
                excepcion.getCause()
        );
    }

    @ParameterizedTest
    @MethodSource("jsonInvalidos")
    void debeRechazarCambiosInvalidos(String json) {
        when(operaciones.get("catedra:sync:changes:7"))
                .thenReturn(json);

        assertThrows(
                IllegalStateException.class,
                () -> adapter.obtenerCambios(7L)
        );
    }

    private static Stream<String> jsonInvalidos() {
        return Stream.of(
                """
                {
                  "version": 5,
                  "changes": {
                    "professionalCategories": [],
                    "professionals": [],
                    "weeklySchedules": []
                  }
                }
                """,
                "esto no es JSON",
                """
                {
                  "version": 7
                }
                """,
                """
                {
                  "version": 7,
                  "changes": {
                    "professionalCategories": [],
                    "professionals": "15",
                    "weeklySchedules": []
                  }
                }
                """,
                """
                {
                  "version": 7,
                  "changes": {
                    "professionalCategories": [],
                    "professionals": [{"id": 15}],
                    "weeklySchedules": []
                  }
                }
                """
        );
    }
}
