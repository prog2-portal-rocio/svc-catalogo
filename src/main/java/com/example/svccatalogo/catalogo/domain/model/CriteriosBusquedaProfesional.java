package com.example.svccatalogo.catalogo.domain.model;

public record CriteriosBusquedaProfesional(
        Long categoriaId,
        String nombre,
        Boolean habilitado
) {

    public CriteriosBusquedaProfesional {
        if (nombre != null) {
            nombre = nombre.strip();
            if (nombre.isEmpty()) {
                nombre = null;
            }
        }
    }
}
