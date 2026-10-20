package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EstadoSincronizacionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstadoSincronizacionJpaRepository
        extends JpaRepository<EstadoSincronizacionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EstadoSincronizacionEntity e WHERE e.id = :id")
    Optional<EstadoSincronizacionEntity> buscarPorIdConBloqueo(
            @Param("id") Long id
    );
}
