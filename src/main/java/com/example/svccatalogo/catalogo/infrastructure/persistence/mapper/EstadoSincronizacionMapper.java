package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EstadoSincronizacionEntity;

public class EstadoSincronizacionMapper {

    private EstadoSincronizacionMapper() {
    }

    public static EstadoSincronizacion toDomain(
            EstadoSincronizacionEntity entity) {

        return new EstadoSincronizacion(
                entity.getAppliedVersion(),
                entity.getAppliedAt(),
                entity.getLastAttemptAt(),
                entity.getPhase(),
                entity.getSafeDiagnostic()
        );
    }

    public static EstadoSincronizacionEntity toEntity(
            EstadoSincronizacion estado) {

        return new EstadoSincronizacionEntity(
                1L,
                estado.getAppliedVersion(),
                estado.getAppliedAt(),
                estado.getLastAttemptAt(),
                estado.getPhase(),
                estado.getSafeDiagnostic()
        );
    }
}
