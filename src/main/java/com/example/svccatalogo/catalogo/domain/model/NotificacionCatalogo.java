package com.example.svccatalogo.catalogo.domain.model;

public record NotificacionCatalogo(
        String eventId,
        long nuevaVersion
) {
    public NotificacionCatalogo {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del evento es obligatorio"
            );
        }

        if (nuevaVersion < 0) {
            throw new IllegalArgumentException(
                    "La version del catalogo no puede ser negativa"
            );
        }
    }
}
