package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.ProfesionalEntity;

public class ProfesionalMapper {

    private ProfesionalMapper() {
    }

    public static Profesional toDomain(ProfesionalEntity entity) {
        return new Profesional(
                entity.getId(),
                entity.getCategoryId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProfesionalEntity toEntity(Profesional profesional) {
        return new ProfesionalEntity(
                profesional.getId(),
                profesional.getCategoryId(),
                profesional.getFirstName(),
                profesional.getLastName(),
                profesional.isEnabled(),
                profesional.getCreatedAt(),
                profesional.getUpdatedAt()
        );
    }
}
