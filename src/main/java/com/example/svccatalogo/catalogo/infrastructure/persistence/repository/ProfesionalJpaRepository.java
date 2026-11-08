package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.ProfesionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfesionalJpaRepository extends JpaRepository<ProfesionalEntity, Long> {

    @Query("""
            SELECT p FROM ProfesionalEntity p
            WHERE (:categoriaId IS NULL OR p.categoryId = :categoriaId)
              AND (:nombre IS NULL
                   OR LOCATE(LOWER(CAST(:nombre AS String)), LOWER(p.firstName)) > 0
                   OR LOCATE(LOWER(CAST(:nombre AS String)), LOWER(p.lastName)) > 0)
              AND (:habilitado IS NULL OR p.enabled = :habilitado)
            """)
    List<ProfesionalEntity> buscar(
            @Param("categoriaId") Long categoriaId,
            @Param("nombre") String nombre,
            @Param("habilitado") Boolean habilitado);
}
