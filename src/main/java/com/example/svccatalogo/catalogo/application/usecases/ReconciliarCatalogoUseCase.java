package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRedisPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ReconciliarCatalogoUseCase implements ReconciliarCatalogoPort {

    private final EstadoSincronizacionRepositoryPort estados;
    private final SincronizarCatalogoIncrementalPort sincronizacion;
    private final CatalogoRedisPort redis;

    public ReconciliarCatalogoUseCase(EstadoSincronizacionRepositoryPort estados,
                                     SincronizarCatalogoIncrementalPort sincronizacion,
                                     CatalogoRedisPort redis) {
        this.estados = Objects.requireNonNull(estados);
        this.sincronizacion = Objects.requireNonNull(sincronizacion);
        this.redis = Objects.requireNonNull(redis);
    }

    @Override
    @Transactional(timeoutString = "${catalogo.sincronizacion.transaction-timeout-seconds:60}")
    public void reconciliarCatalogo() {
        estados.obtenerEstadoConBloqueo();
        sincronizacion.sincronizarCatalogoIncremental();

        Long versionAplicada = estados.obtenerEstado().getAppliedVersion();
        long versionPublicada = redis.obtenerVersionActual();
        if (versionAplicada == null || versionAplicada != versionPublicada) {
            throw new IllegalStateException("No se confirmo la version vigente del catalogo al reconciliar");
        }
    }
}
