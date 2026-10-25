package com.example.svccatalogo.catalogo.domain.model;

import java.util.List;
import java.util.Objects;

public record SnapshotCatalogo(
        long version,
        List<Categoria> categorias,
        List<Profesional> profesionales,
        List<HorarioSemanal> horarios
) {
    public SnapshotCatalogo {
        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version del snapshot no puede ser negativa"
            );
        }

        Objects.requireNonNull(categorias, "Las categorias son obligatorias");
        Objects.requireNonNull(profesionales, "Los profesionales son obligatorios");
        Objects.requireNonNull(horarios, "Los horarios son obligatorios");

        categorias = List.copyOf(categorias);
        profesionales = List.copyOf(profesionales);
        horarios = List.copyOf(horarios);
    }
}
