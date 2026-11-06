package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.*;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
public class SincronizarCatalogoIncrementalUseCase
        implements SincronizarCatalogoIncrementalPort {

    private static final int MAX_INTENTOS = 3;

    private final EstadoSincronizacionRepositoryPort estadoRepository;
    private final CatalogoRedisPort redisPort;
    private final CatalogoIncrementalPersistencePort persistencePort;
    private final SincronizarCatalogoPort sincronizacionCompleta;

    public SincronizarCatalogoIncrementalUseCase(
            EstadoSincronizacionRepositoryPort estadoRepository,
            CatalogoRedisPort redisPort,
            CatalogoIncrementalPersistencePort persistencePort,
            SincronizarCatalogoPort sincronizacionCompleta) {

        this.estadoRepository = Objects.requireNonNull(estadoRepository);
        this.redisPort = Objects.requireNonNull(redisPort);
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.sincronizacionCompleta = Objects.requireNonNull(sincronizacionCompleta);
    }

    @Override
    public void sincronizarCatalogoIncremental() {

        EstadoSincronizacion estado = estadoRepository.obtenerEstado();
        Long versionLocal = estado.getAppliedVersion();

        if (versionLocal == null) {
            sincronizacionCompleta.sincronizarCatalogo();
            return;
        }

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {

            long versionObjetivo = redisPort.obtenerVersionActual();

            if (versionLocal == versionObjetivo) {
                return;
            }

            if (versionLocal > versionObjetivo) {
                throw new IllegalStateException(
                        "La version local supera la version publicada en Redis"
                );
            }

            long versionMasAntigua =
                    redisPort.obtenerVersionMasAntiguaDisponible();

            if (versionLocal < versionMasAntigua) {
                sincronizacionCompleta.sincronizarCatalogo();
                return;
            }

            Set<Long> idsCategorias = new LinkedHashSet<>();
            Set<Long> idsProfesionales = new LinkedHashSet<>();
            Set<Long> idsHorarios = new LinkedHashSet<>();

            // Reunir los IDs modificados en todas las versiones pendientes.
            for (long version = versionLocal + 1;
                 version <= versionObjetivo;
                 version++) {

                Optional<CambiosCatalogo> cambiosOpt =
                        redisPort.obtenerCambios(version);

                if (cambiosOpt.isEmpty()) {
                    sincronizacionCompleta.sincronizarCatalogo();
                    return;
                }

                CambiosCatalogo cambios = cambiosOpt.orElseThrow();

                if (cambios.version() != version) {
                    sincronizacionCompleta.sincronizarCatalogo();
                    return;
                }

                idsCategorias.addAll(cambios.categorias());
                idsProfesionales.addAll(cambios.profesionales());
                idsHorarios.addAll(cambios.horariosSemanales());
            }

            // Recuperar el estado vigente, evitando IDs duplicados.
            Optional<List<Categoria>> categorias = recuperarEntidades(
                    idsCategorias,
                    redisPort::obtenerCategoria
            );

            Optional<List<Profesional>> profesionales = recuperarEntidades(
                    idsProfesionales,
                    redisPort::obtenerProfesional
            );

            Optional<List<HorarioSemanal>> horarios = recuperarEntidades(
                    idsHorarios,
                    redisPort::obtenerHorarioSemanal
            );

            if (categorias.isEmpty()
                    || profesionales.isEmpty()
                    || horarios.isEmpty()) {

                sincronizacionCompleta.sincronizarCatalogo();
                return;
            }

            // Si Redis publico otra version durante la lectura,
            // descartamos lo recuperado y volvemos a intentar.
            long versionFinal = redisPort.obtenerVersionActual();

            if (versionFinal != versionObjetivo) {
                continue;
            }

            // Persistir todos los cambios y avanzar a la version objetivo.
            persistencePort.aplicarVersion(
                    versionLocal,
                    versionObjetivo,
                    categorias.orElseThrow(),
                    profesionales.orElseThrow(),
                    horarios.orElseThrow()
            );

            return;
        }

        // Si Redis sigue cambiando, recuperamos un snapshot completo.
        sincronizacionCompleta.sincronizarCatalogo();
    }

    private <T> Optional<List<T>> recuperarEntidades(
            Set<Long> ids,
            Function<Long, Optional<T>> lector) {

        List<T> entidades = new ArrayList<>();

        for (Long id : ids) {
            Optional<T> entidad = lector.apply(id);

            if (entidad.isEmpty()) {
                return Optional.empty();
            }

            entidades.add(entidad.orElseThrow());
        }

        return Optional.of(entidades);
    }
}