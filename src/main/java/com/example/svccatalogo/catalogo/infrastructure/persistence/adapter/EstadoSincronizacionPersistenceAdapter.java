package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.EstadoSincronizacionMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EstadoSincronizacionJpaRepository;
import org.springframework.stereotype.Repository;

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
    public EstadoSincronizacion guardarEstado(EstadoSincronizacion estado) {
        var entity = EstadoSincronizacionMapper.toEntity(estado);
        var saved = repository.save(entity);
        return EstadoSincronizacionMapper.toDomain(saved);
    }
}
