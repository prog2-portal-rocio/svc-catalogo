package com.example.svccatalogo.catalogo.infrastructure.web.dto;

import java.time.Instant;

public record EstadoSincronizacionResponse(
        Long appliedVersion,
        Instant appliedAt,
        Instant lastAttemptAt,
        String phase,
        String safeDiagnostic
) {
}
