package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BuscarProfesionalesUseCase implements BuscarProfesionalesPort {

    private final ProfesionalRepositoryPort profesionalRepository;

    public BuscarProfesionalesUseCase(ProfesionalRepositoryPort profesionalRepository) {
        this.profesionalRepository = Objects.requireNonNull(profesionalRepository);
    }

    @Override
    public List<Profesional> buscarProfesionales(CriteriosBusquedaProfesional criterios) {
        Objects.requireNonNull(criterios, "Los criterios de busqueda son obligatorios");
        return profesionalRepository.buscar(criterios);
    }
}
