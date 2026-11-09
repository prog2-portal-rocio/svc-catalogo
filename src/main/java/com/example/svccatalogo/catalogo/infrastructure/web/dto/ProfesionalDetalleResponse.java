package com.example.svccatalogo.catalogo.infrastructure.web.dto;

import java.time.Instant;

public record ProfesionalDetalleResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        CategoriaResponse category
) {

    public record CategoriaResponse(
            Long id,
            String name,
            String description,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
