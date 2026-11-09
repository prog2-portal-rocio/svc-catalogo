package com.example.svccatalogo.catalogo.infrastructure.web.dto;

import java.time.Instant;

public record ProfesionalResponse(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
