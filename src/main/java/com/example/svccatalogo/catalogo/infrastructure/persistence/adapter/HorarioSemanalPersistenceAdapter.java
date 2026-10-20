package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.HorarioSemanalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.HorarioSemanalJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HorarioSemanalPersistenceAdapter implements HorarioSemanalRepositoryPort {

    private final HorarioSemanalJpaRepository repository;

    public HorarioSemanalPersistenceAdapter(HorarioSemanalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<HorarioSemanal> findById(Long id) {
        return repository.findById(id)
                .map(HorarioSemanalMapper::toDomain);
    }

    @Override
    public List<HorarioSemanal> findAll() {
        return repository.findAll().stream()
                .map(HorarioSemanalMapper::toDomain)
                .toList();
    }

    @Override
    public HorarioSemanal save(HorarioSemanal horario) {
        var entity = HorarioSemanalMapper.toEntity(horario);
        var saved = repository.save(entity);
        return HorarioSemanalMapper.toDomain(saved);
    }
}
