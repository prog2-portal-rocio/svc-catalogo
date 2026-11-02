package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EventoCatalogoProcesadoJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class EventoCatalogoProcesadoAdapter
        implements EventoCatalogoProcesadoPort {

    private final EventoCatalogoProcesadoJpaRepository repository;

    public EventoCatalogoProcesadoAdapter(
            EventoCatalogoProcesadoJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public boolean registrarSiNoExiste(String eventId, long version) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del evento es obligatorio"
            );
        }

        if (eventId.length() > 255) {
            throw new IllegalArgumentException(
                    "El identificador del evento es demasiado largo"
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version no puede ser negativa"
            );
        }

        int filasInsertadas =
                repository.insertarSiNoExiste(eventId, version);

        if (filasInsertadas != 0 && filasInsertadas != 1) {
            throw new IllegalStateException(
                    "Resultado inesperado al registrar el evento"
            );
        }

        return filasInsertadas == 1;
    }
}
