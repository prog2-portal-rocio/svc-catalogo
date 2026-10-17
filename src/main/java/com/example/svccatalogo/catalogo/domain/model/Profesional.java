package com.example.svccatalogo.catalogo.domain.model;

import java.time.Instant;

public class Profesional {

    private final Long id;
    private final Long categoryId;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Profesional(Long id, Long categoryId, String firstName,
                       String lastName, boolean enabled,
                       Instant createdAt, Instant updatedAt) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del profesional debe ser positivo");
        }
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("El ID de la categoria debe ser positivo");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }

        this.id = id;
        this.categoryId = categoryId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
