package com.example.svccatalogo.catalogo.domain.model;

import java.time.Instant;

public class EstadoSincronizacion {

    private final Long appliedVersion;
    private final Instant appliedAt;
    private final Instant lastAttemptAt;
    private final String phase;
    private final String safeDiagnostic;

    public EstadoSincronizacion(
            Long appliedVersion,
            Instant appliedAt,
            Instant lastAttemptAt,
            String phase,
            String safeDiagnostic) {

        if (appliedVersion != null && appliedVersion < 0) {
            throw new IllegalArgumentException(
                    "La version aplicada no puede ser negativa"
            );
        }

        if (phase == null || phase.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado de sincronizacion es obligatorio"
            );
        }

        this.appliedVersion = appliedVersion;
        this.appliedAt = appliedAt;
        this.lastAttemptAt = lastAttemptAt;
        this.phase = phase;
        this.safeDiagnostic = safeDiagnostic;
    }

    public Long getAppliedVersion() {
        return appliedVersion;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public String getPhase() {
        return phase;
    }

    public String getSafeDiagnostic() {
        return safeDiagnostic;
    }
}
