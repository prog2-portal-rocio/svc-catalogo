package com.example.svccatalogo.catalogo.infrastructure.persistence.repository;

import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.HorarioSemanalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;

public interface HorarioSemanalJpaRepository extends JpaRepository<HorarioSemanalEntity, Long> {
    List<HorarioSemanalEntity> findByProfessionalIdOrderByIdAsc(Long profesionalId);

    List<HorarioSemanalEntity> findByProfessionalIdInAndDayOfWeekAndEnabledTrue(
            List<Long> profesionalIds, DayOfWeek dia);
}
