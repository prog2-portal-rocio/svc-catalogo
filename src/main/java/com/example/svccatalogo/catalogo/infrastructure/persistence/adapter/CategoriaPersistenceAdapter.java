package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.CategoriaMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.CategoriaJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaPersistenceAdapter implements CategoriaRepositoryPort {

    private final CategoriaJpaRepository repository;

    public CategoriaPersistenceAdapter(CategoriaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        return repository.findById(id)
                .map(CategoriaMapper::toDomain);
    }

    @Override
    public List<Categoria> findAll() {
        return repository.findAll().stream()
                .map(CategoriaMapper::toDomain)
                .toList();
    }

    @Override
    public Categoria save(Categoria categoria) {
        var entity = CategoriaMapper.toEntity(categoria);
        var saved = repository.save(entity);
        return CategoriaMapper.toDomain(saved);
    }
}
