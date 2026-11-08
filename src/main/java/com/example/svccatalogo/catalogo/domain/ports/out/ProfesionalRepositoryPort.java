package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;

import java.util.List;
import java.util.Optional;

public interface ProfesionalRepositoryPort {

    Optional<Profesional> findById(Long id);

    List<Profesional> findAll();

    List<Profesional> buscar(CriteriosBusquedaProfesional criterios);

    Profesional save(Profesional profesional);
}
