package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.ProfesionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfesionalJpaRepository extends JpaRepository<ProfesionalEntity, Long> {
}
