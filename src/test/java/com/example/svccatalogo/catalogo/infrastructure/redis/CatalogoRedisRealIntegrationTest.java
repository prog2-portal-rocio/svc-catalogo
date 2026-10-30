package com.example.svccatalogo.catalogo.infrastructure.redis;

import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(
        named = "REDIS_PASSWORD",
        matches = ".+"
)
class CatalogoRedisRealIntegrationTest {

    @Test
    void debeConsultarCatalogoDesdeRedisReal() {
        String host = System.getenv("REDIS_HOST");
        int port = Integer.parseInt(System.getenv("REDIS_PORT"));
        String username = System.getenv("REDIS_USERNAME");
        String password = System.getenv("REDIS_PASSWORD");

        RedisStandaloneConfiguration configuracion =
                new RedisStandaloneConfiguration(host, port);

        configuracion.setUsername(username);
        configuracion.setPassword(RedisPassword.of(password));

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(configuracion);

        factory.afterPropertiesSet();

        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();

            CatalogoRedisAdapter adapter = new CatalogoRedisAdapter(
                    template,
                    JsonMapper.builder().build()
            );

            long versionActual = adapter.obtenerVersionActual();
            long versionMasAntigua =
                    adapter.obtenerVersionMasAntiguaDisponible();

            assertTrue(versionActual > 0);
            assertTrue(versionMasAntigua > 0);
            assertTrue(versionMasAntigua <= versionActual);

            Optional<CambiosCatalogo> cambios =
                    adapter.obtenerCambios(versionActual);

            assertTrue(cambios.isPresent());
            assertEquals(versionActual, cambios.orElseThrow().version());

        } finally {
            factory.destroy();
        }
    }
}
