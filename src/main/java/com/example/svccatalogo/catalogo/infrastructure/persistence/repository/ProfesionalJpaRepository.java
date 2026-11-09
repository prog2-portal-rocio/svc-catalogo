package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.ProfesionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.DayOfWeek;

public interface ProfesionalJpaRepository extends JpaRepository<ProfesionalEntity, Long> {

    @Query("""
            SELECT p FROM ProfesionalEntity p
            WHERE (:categoriaId IS NULL OR p.categoryId = :categoriaId)
              AND (:nombre IS NULL
                   OR LOCATE(LOWER(CAST(:nombre AS String)), LOWER(p.firstName)) > 0
                   OR LOCATE(LOWER(CAST(:nombre AS String)), LOWER(p.lastName)) > 0)
              AND (:habilitado IS NULL OR p.enabled = :habilitado)
              AND (:dia IS NULL OR (p.enabled = true
                   AND EXISTS (SELECT c.id FROM CategoriaEntity c
                               WHERE c.id = p.categoryId AND c.enabled = true)
                   AND EXISTS (SELECT h.id FROM HorarioSemanalEntity h
                               WHERE h.professionalId = p.id AND h.enabled = true
                                 AND h.dayOfWeek = :dia)))
            """)
    List<ProfesionalEntity> buscar(
            @Param("categoriaId") Long categoriaId,
            @Param("nombre") String nombre,
            @Param("habilitado") Boolean habilitado,
            @Param("dia") DayOfWeek dia);
}
