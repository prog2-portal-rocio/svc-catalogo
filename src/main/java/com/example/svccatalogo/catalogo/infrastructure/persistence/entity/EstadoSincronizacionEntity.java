package com.example.svccatalogo.catalogo.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "estado_sincronizacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

}
