package com.example.svccatalogo.catalogo.domain.model;

import java.util.List;
import java.util.Objects;

public record CambiosCatalogo(
        long version,
        List<Long> categorias,
        List<Long> profesionales,
        List<Long> horariosSemanales
) {

    public CambiosCatalogo {
        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version no puede ser negativa"
            );
        }

        categorias = List.copyOf(
                Objects.requireNonNull(categorias)
        );

        profesionales = List.copyOf(
                Objects.requireNonNull(profesionales)
        );

        horariosSemanales = List.copyOf(
                Objects.requireNonNull(horariosSemanales)
        );
    }
}
