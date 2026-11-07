package com.example.svccatalogo.catalogo.infrastructure.redis;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
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
import org.springframework.data.redis.core.HashOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoRedisAdapterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> operaciones;
    private CatalogoRedisAdapter adapter;
    private HashOperations<String, String, String> hashes;

    @BeforeEach
    void configurar() {
        redisTemplate = mock(StringRedisTemplate.class);
        operaciones = mock(ValueOperations.class);

        hashes = mock(HashOperations.class);

        when(redisTemplate.<String, String>opsForHash())
                .thenReturn(hashes);

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

    @Test
    void debeObtenerProfesionalDesdeRedis() {
        String json = """
            {
              "id": 15,
              "categoryId": 1,
              "firstName": "Ana",
              "lastName": "García",
              "enabled": false,
              "createdAt": "2026-01-15T10:00:00Z",
              "updatedAt": "2026-01-16T10:00:00Z"
            }
            """;

        when(hashes.get("catedra:sync:professionals", "15"))
                .thenReturn(json);

        Profesional profesional =
                adapter.obtenerProfesional(15L).orElseThrow();

        assertEquals(15L, profesional.getId());
        assertEquals(1L, profesional.getCategoryId());
        assertEquals("Ana", profesional.getFirstName());
        assertFalse(profesional.isEnabled());
    }

    @Test
    void debeObtenerCategoriaDesdeRedis() {
        String json = """
            {
              "id": 1,
              "name": "Peluquería",
              "description": "Servicios de cabello",
              "enabled": true,
              "createdAt": "2026-01-15T10:00:00Z",
              "updatedAt": "2026-01-15T10:00:00Z"
            }
            """;

        when(hashes.get("catedra:sync:professional-categories", "1"))
                .thenReturn(json);

        Categoria categoria =
                adapter.obtenerCategoria(1L).orElseThrow();

        assertEquals(1L, categoria.getId());
        assertEquals("Peluquería", categoria.getName());
        assertTrue(categoria.isEnabled());
    }

    @Test
    void debeObtenerHorarioSemanalDesdeRedis() {
        String json = """
            {
              "id": 100,
              "professionalId": 15,
              "dayOfWeek": "MONDAY",
              "startTime": "09:00:00",
              "endTime": "13:00:00",
              "slotDurationMinutes": 30,
              "enabled": true,
              "createdAt": "2026-01-15T10:00:00Z",
              "updatedAt": "2026-01-15T10:00:00Z"
            }
            """;

        when(hashes.get("catedra:sync:weekly-schedules", "100"))
                .thenReturn(json);

        HorarioSemanal horario =
                adapter.obtenerHorarioSemanal(100L).orElseThrow();

        assertEquals(100L, horario.getId());
        assertEquals(15L, horario.getProfessionalId());
        assertEquals(30, horario.getSlotDurationMinutes());
        assertTrue(horario.isEnabled());
    }

    @Test
    void debeDevolverVacioSiLaEntidadNoExiste() {
        when(hashes.get("catedra:sync:professionals", "15"))
                .thenReturn(null);

        assertTrue(adapter.obtenerProfesional(15L).isEmpty());
    }

    @Test
    void debeRechazarEntidadConIdentificadorDistinto() {
        String json = """
            {
              "id": 99,
              "categoryId": 1,
              "firstName": "Ana",
              "lastName": "García",
              "enabled": true
            }
            """;

        when(hashes.get("catedra:sync:professionals", "15"))
                .thenReturn(json);

        assertThrows(
                IllegalStateException.class,
                () -> adapter.obtenerProfesional(15L)
        );
    }

    @Test
    void debeRechazarEntidadSinEstadoEnabled() {
        String json = """
            {
              "id": 15,
              "categoryId": 1,
              "firstName": "Ana",
              "lastName": "García"
            }
            """;

        when(hashes.get("catedra:sync:professionals", "15"))
                .thenReturn(json);

        assertThrows(
                RuntimeException.class,
                () -> adapter.obtenerProfesional(15L)
        );
    }
}
