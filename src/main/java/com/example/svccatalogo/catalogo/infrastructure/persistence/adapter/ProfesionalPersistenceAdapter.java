package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.ProfesionalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.ProfesionalJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProfesionalPersistenceAdapter implements ProfesionalRepositoryPort {

    private final ProfesionalJpaRepository repository;

    public ProfesionalPersistenceAdapter(ProfesionalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Profesional> findById(Long id) {
        return repository.findById(id)
                .map(ProfesionalMapper::toDomain);
    }

    @Override
    public List<Profesional> findAll() {
        return repository.findAll().stream()
                .map(ProfesionalMapper::toDomain)
                .toList();
    }

    @Override
    public Profesional save(Profesional profesional) {
        var entity = ProfesionalMapper.toEntity(profesional);
        var saved = repository.save(entity);
        return ProfesionalMapper.toDomain(saved);
    }
}
