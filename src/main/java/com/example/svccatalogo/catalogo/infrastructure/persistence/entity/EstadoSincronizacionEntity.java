package com.example.svccatalogo.catalogo.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "estado_sincronizacion")
public class EstadoSincronizacionEntity {

    @Id
    private Long id;

    @Column(name = "applied_version")
    private Long appliedVersion;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "phase", nullable = false, length = 20)
    private String phase;

    @Column(name = "safe_diagnostic", length = 255)
    private String safeDiagnostic;

    protected EstadoSincronizacionEntity() {
    }

    public EstadoSincronizacionEntity(
            Long id,
            Long appliedVersion,
            Instant appliedAt,
            Instant lastAttemptAt,
            String phase,
            String safeDiagnostic) {

        this.id = id;
        this.appliedVersion = appliedVersion;
        this.appliedAt = appliedAt;
        this.lastAttemptAt = lastAttemptAt;
        this.phase = phase;
        this.safeDiagnostic = safeDiagnostic;
    }

    public Long getId() {
        return id;
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
