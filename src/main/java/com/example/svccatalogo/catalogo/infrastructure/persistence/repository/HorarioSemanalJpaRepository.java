package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.HorarioSemanalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioSemanalJpaRepository extends JpaRepository<HorarioSemanalEntity, Long> {
}
