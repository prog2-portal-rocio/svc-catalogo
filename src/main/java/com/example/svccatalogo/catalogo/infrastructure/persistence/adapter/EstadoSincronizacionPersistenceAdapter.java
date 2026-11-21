package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EstadoSincronizacionEntity;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.EstadoSincronizacionMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EstadoSincronizacionJpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Repository
public class EstadoSincronizacionPersistenceAdapter
        implements EstadoSincronizacionRepositoryPort {

    private static final Long ESTADO_ID = 1L;

    private final EstadoSincronizacionJpaRepository repository;

    public EstadoSincronizacionPersistenceAdapter(
            EstadoSincronizacionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public EstadoSincronizacion obtenerEstado() {
        return repository.findById(ESTADO_ID)
                .map(EstadoSincronizacionMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado de sincronizacion del catalogo"
                ));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EstadoSincronizacion obtenerEstadoConBloqueo() {
        return repository.buscarPorIdConBloqueo(ESTADO_ID)
                .map(EstadoSincronizacionMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado de sincronizacion del catalogo"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarResultadoIntento(Instant finalizadoEn, boolean exitoso) {
        Instant fecha = Objects.requireNonNull(finalizadoEn).truncatedTo(ChronoUnit.MICROS);
        var actual = repository.buscarPorIdConBloqueo(ESTADO_ID)
                .orElseThrow(() -> new IllegalStateException("No existe el estado de sincronizacion"));
        // Una notificacion diagnostica demorada no debe pisar un resultado mas reciente.
        if (actual.getLastAttemptAt() != null && actual.getLastAttemptAt().isAfter(fecha)) {
            return;
        }
        repository.save(new EstadoSincronizacionEntity(ESTADO_ID,
                actual.getAppliedVersion(), actual.getAppliedAt(), fecha,
                exitoso ? "SUCCESS" : "ERROR",
                exitoso ? "Ultimo intento de sincronizacion completado"
                        : "Fallo el ultimo intento de sincronizacion; se conserva el ultimo catalogo valido"));
    }

    @Override
    public EstadoSincronizacion guardarEstado(EstadoSincronizacion estado) {
        var entity = EstadoSincronizacionMapper.toEntity(estado);
        var saved = repository.save(entity);
        return EstadoSincronizacionMapper.toDomain(saved);
    }
}
