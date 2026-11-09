package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.application.exception.ProfesionalNoEncontradoException;
import com.example.svccatalogo.catalogo.domain.model.ProfesionalConCategoria;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarProfesionalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ConsultarProfesionalUseCase implements ConsultarProfesionalPort {

    private final ProfesionalRepositoryPort profesionales;
    private final CategoriaRepositoryPort categorias;

    public ConsultarProfesionalUseCase(ProfesionalRepositoryPort profesionales, CategoriaRepositoryPort categorias) {
        this.profesionales = Objects.requireNonNull(profesionales);
        this.categorias = Objects.requireNonNull(categorias);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfesionalConCategoria consultarProfesional(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del profesional debe ser positivo");
        }
        var profesional = profesionales.findById(id)
                .orElseThrow(() -> new ProfesionalNoEncontradoException(id));
        var categoria = categorias.findById(profesional.getCategoryId())
                .orElseThrow(() -> new IllegalStateException("No existe la categoria del profesional"));
        return new ProfesionalConCategoria(profesional, categoria);
    }
}
