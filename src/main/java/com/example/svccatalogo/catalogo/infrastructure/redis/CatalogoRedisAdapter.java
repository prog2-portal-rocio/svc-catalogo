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
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.infrastructure.client.CatalogoSnapshotMapper;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.CategoriaResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.ProfesionalResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.HorarioSemanalResponseDto;

import java.util.Optional;

@Repository
public class CatalogoRedisAdapter implements CatalogoRedisPort {

    private static final String VERSION_ACTUAL =
            "catedra:sync:current-version";

    private static final String VERSION_MAS_ANTIGUA =
            "catedra:sync:oldest-available-version";

    private static final String PREFIJO_CAMBIOS =
            "catedra:sync:changes:";

    private static final String HASH_CATEGORIAS =
            "catedra:sync:professional-categories";

    private static final String HASH_PROFESIONALES =
            "catedra:sync:professionals";

    private static final String HASH_HORARIOS =
            "catedra:sync:weekly-schedules";

    private final StringRedisTemplate redisTemplate;

    private final JsonMapper jsonMapper;

    private final CatalogoSnapshotMapper snapshotMapper =
            new CatalogoSnapshotMapper();

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

    @Override
    public Optional<Categoria> obtenerCategoria(long id) {
        return leerEntidad(
                HASH_CATEGORIAS,
                id,
                CategoriaResponseDto.class
        ).map(dto -> {
            validarIdentificador(id, dto.id());
            return snapshotMapper.toCategoria(dto);
        });
    }

    @Override
    public Optional<Profesional> obtenerProfesional(long id) {
        return leerEntidad(
                HASH_PROFESIONALES,
                id,
                ProfesionalResponseDto.class
        ).map(dto -> {
            validarIdentificador(id, dto.id());
            return snapshotMapper.toProfesional(dto);
        });
    }

    @Override
    public Optional<HorarioSemanal> obtenerHorarioSemanal(long id) {
        return leerEntidad(
                HASH_HORARIOS,
                id,
                HorarioSemanalResponseDto.class
        ).map(dto -> {
            validarIdentificador(id, dto.id());
            return snapshotMapper.toHorarioSemanal(dto);
        });
    }

    private <T> Optional<T> leerEntidad(
            String claveHash,
            long id,
            Class<T> tipoDto
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador debe ser positivo"
            );
        }

        String json = ejecutarConsulta(
                () -> redisTemplate.<String, String>opsForHash()
                        .get(claveHash, Long.toString(id))
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            T entidad = jsonMapper.readValue(json, tipoDto);

            if (entidad == null) {
                throw new IllegalStateException(
                        "La entidad de Redis no puede ser nula"
                );
            }

            return Optional.of(entidad);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "El contenido de la entidad en Redis no es valido",
                    e
            );
        }
    }

    private void validarIdentificador(
            long idSolicitado,
            Long idRecibido
    ) {
        if (idRecibido == null
                || idRecibido.longValue() != idSolicitado) {
            throw new IllegalStateException(
                    "El identificador de Redis no coincide con el solicitado"
            );
        }
    }
}
