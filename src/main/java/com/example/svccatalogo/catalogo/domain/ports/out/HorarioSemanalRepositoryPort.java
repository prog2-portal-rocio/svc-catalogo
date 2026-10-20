package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;
import java.util.Optional;

public interface HorarioSemanalRepositoryPort {

    Optional<HorarioSemanal> findById(Long id);

    List<HorarioSemanal> findAll();

    HorarioSemanal save(HorarioSemanal horario);
}
