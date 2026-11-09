package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;

public interface HorarioSemanalRepositoryPort {

    Optional<HorarioSemanal> findById(Long id);

    List<HorarioSemanal> findAll();

    List<HorarioSemanal> buscarPorProfesional(Long profesionalId);

    List<HorarioSemanal> buscarHabilitadosPorProfesionalesYDia(List<Long> profesionalIds, DayOfWeek dia);

    HorarioSemanal save(HorarioSemanal horario);
}
