package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.in.AplicarSnapshotCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoSnapshotPersistencePort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AplicarSnapshotCatalogoUseCase
        implements AplicarSnapshotCatalogoPort {

    private final CatalogoSnapshotPersistencePort persistencePort;

    public AplicarSnapshotCatalogoUseCase(
            CatalogoSnapshotPersistencePort persistencePort) {
        this.persistencePort = Objects.requireNonNull(persistencePort);
    }

    @Override
    public void aplicarSnapshot(
            List<Categoria> categorias,
            List<Profesional> profesionales,
            List<HorarioSemanal> horarios,
            long version) {

        Objects.requireNonNull(categorias, "Las categorias son obligatorias");
        Objects.requireNonNull(profesionales, "Los profesionales son obligatorios");
        Objects.requireNonNull(horarios, "Los horarios son obligatorios");

        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version del snapshot no puede ser negativa"
            );
        }

        persistencePort.reemplazarCatalogo(
                categorias,
                profesionales,
                horarios,
                version
        );
    }
}
