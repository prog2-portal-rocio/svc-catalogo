package com.example.svccatalogo.catalogo.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record CriteriosBusquedaProfesional(
        Long categoriaId,
        String nombre,
        Boolean habilitado,
        LocalDate fecha,
        LocalTime hora
) {

    public CriteriosBusquedaProfesional(Long categoriaId, String nombre, Boolean habilitado) {
        this(categoriaId, nombre, habilitado, null, null);
    }

    public CriteriosBusquedaProfesional {
        if (hora != null && fecha == null) {
            throw new IllegalArgumentException("hora requiere una fecha");
        }
        if (nombre != null) {
            nombre = nombre.strip();
            if (nombre.isEmpty()) {
                nombre = null;
            }
        }
    }
}
