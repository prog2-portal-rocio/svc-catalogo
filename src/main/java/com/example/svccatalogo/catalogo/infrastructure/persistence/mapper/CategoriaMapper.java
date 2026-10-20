package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.CategoriaEntity;

public class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static Categoria toDomain(CategoriaEntity entity) {
        return new Categoria(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CategoriaEntity toEntity(Categoria categoria) {
        return new CategoriaEntity(
                categoria.getId(),
                categoria.getName(),
                categoria.getDescription(),
                categoria.isEnabled(),
                categoria.getCreatedAt(),
                categoria.getUpdatedAt()
        );
    }
}
