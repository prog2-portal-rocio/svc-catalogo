package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EventoCatalogoProcesadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoCatalogoProcesadoJpaRepository
        extends JpaRepository<EventoCatalogoProcesadoEntity, String> {

    @Modifying
    @Query(value = """
            INSERT INTO eventos_catalogo_procesados
                (event_id, version_catalogo)
            VALUES (:eventId, :version)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(
            @Param("eventId") String eventId,
            @Param("version") long version
    );
}
