package com.example.svccatalogo.catalogo.infrastructure.redis;

import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import java.util.ArrayList;
import java.util.List;
import com.example.svccatalogo.catalogo.application.exception.FuenteCatalogoNoDisponibleException;
import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.function.Supplier;

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

    private final JsonMapper jsonMapper;

    public CatalogoRedisAdapter(
            StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
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
    public Optional<CambiosCatalogo> obtenerCambios(long version) {
        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version no puede ser negativa"
            );
        }

        String cambios = ejecutarConsulta(
                () -> redisTemplate.opsForValue().get(PREFIJO_CAMBIOS + version)
        );

        if (cambios == null) {
            return Optional.empty();
        }

        try {
            JsonNode contenido = jsonMapper.readTree(cambios);

            if (!contenido.isObject()) {
                throw new IllegalStateException(
                        "Los cambios de Redis no tienen un formato valido"
                );
            }

            JsonNode versionRecibida = contenido.path("version");

            if (!versionRecibida.isIntegralNumber()
                    || !versionRecibida.canConvertToLong()
                    || versionRecibida.longValue() != version) {
                throw new IllegalStateException(
                        "La version de los cambios no coincide con la solicitada"
                );
            }

            JsonNode detalleCambios = contenido.path("changes");

            if (!detalleCambios.isObject()) {
                throw new IllegalStateException(
                        "No se encontro la estructura de cambios en Redis"
                );
            }

            validarListasCambios(detalleCambios);

            CambiosCatalogo resultado = new CambiosCatalogo(
                    version,
                    obtenerIdentificadores(detalleCambios.path("professionalCategories")),
                    obtenerIdentificadores(detalleCambios.path("professionals")),
                    obtenerIdentificadores(detalleCambios.path("weeklySchedules"))
            );

            return Optional.of(resultado);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Los cambios de Redis no contienen un JSON valido", e
            );
        }
    }

    private void validarListasCambios(JsonNode detalleCambios) {
        String[] campos = {
                "professionalCategories",
                "professionals",
                "weeklySchedules"
        };

        for (String campo : campos) {
            JsonNode lista = detalleCambios.path(campo);

            if (!lista.isArray()) {
                throw new IllegalStateException(
                        "La lista de cambios no es valida: " + campo
                );
            }

            for (JsonNode identificador : lista) {
                if (!identificador.isIntegralNumber()
                        || !identificador.canConvertToLong()
                        || identificador.longValue() < 0) {
                    throw new IllegalStateException(
                            "Identificador invalido en la lista: " + campo
                    );
                }
            }
        }
    }

    private List<Long> obtenerIdentificadores(JsonNode lista) {
        List<Long> identificadores = new ArrayList<>();

        for (JsonNode identificador : lista) {
            identificadores.add(identificador.longValue());
        }

        return identificadores;
    }

    private long leerVersion(String clave) {
        String valor = ejecutarConsulta(
                () -> redisTemplate.opsForValue().get(clave)
        );

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

    private <T> T ejecutarConsulta(Supplier<T> consulta) {
        try {
            return consulta.get();
        } catch (RedisConnectionFailureException e) {
            throw new FuenteCatalogoNoDisponibleException(
                    "No se pudo establecer conexion con Redis", e
            );
        }
    }
}
