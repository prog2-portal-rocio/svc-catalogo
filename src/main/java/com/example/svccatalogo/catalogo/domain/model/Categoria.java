package com.example.svccatalogo.catalogo.domain.model;

import java.time.Instant;

public class Categoria {

    private final Long id;
    private final String name;
    private final String description;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Categoria(Long id, String name, String description,
                     boolean enabled, Instant createdAt, Instant updatedAt) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID de la categoria debe ser positivo");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
