package com.example.svccatalogo.catalogo.infrastructure.redis;

import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CatalogoRedisAdapter implements CatalogoRedisPort {

    private static final String VERSION_ACTUAL =
            "catedra:sync:current-version";

    private static final String VERSION_MAS_ANTIGUA =
            "catedra:sync:oldest-available-version";

    private static final String PREFIJO_CAMBIOS =
            "catedra:sync:changes:";

    private final StringRedisTemplate redisTemplate;

    public CatalogoRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long obtenerVersionActual() {
        return leerVersion(VERSION_ACTUAL);
    }

    @Override
    public long obtenerVersionMasAntiguaDisponible() {
        return leerVersion(VERSION_MAS_ANTIGUA);
    }

    @Override
    public Optional<String> obtenerCambios(long version) {
        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version no puede ser negativa"
            );
        }

        String cambios = redisTemplate.opsForValue()
                .get(PREFIJO_CAMBIOS + version);

        return Optional.ofNullable(cambios);
    }

    private long leerVersion(String clave) {
        String valor = redisTemplate.opsForValue().get(clave);

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "No se encontro una version valida en Redis: " + clave
            );
        }

        try {
            long version = Long.parseLong(valor);

            if (version < 0) {
                throw new NumberFormatException("Version negativa");
            }

            return version;

        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "La version almacenada en Redis no es valida: " + clave,
                    e
            );
        }
    }
}
