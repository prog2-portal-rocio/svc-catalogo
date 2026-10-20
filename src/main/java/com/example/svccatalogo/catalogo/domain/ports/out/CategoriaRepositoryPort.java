package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepositoryPort {

    Optional<Categoria> findById(Long id);

    List<Categoria> findAll();

    Categoria save(Categoria categoria);
}
