package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.AplicarSnapshotCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SincronizarCatalogoUseCase
        implements SincronizarCatalogoPort {

    private final CatalogoRemotoPort catalogoRemotoPort;
    private final AplicarSnapshotCatalogoPort aplicarSnapshotPort;

    public SincronizarCatalogoUseCase(
            CatalogoRemotoPort catalogoRemotoPort,
            AplicarSnapshotCatalogoPort aplicarSnapshotPort) {

        this.catalogoRemotoPort =
                Objects.requireNonNull(catalogoRemotoPort);

        this.aplicarSnapshotPort =
                Objects.requireNonNull(aplicarSnapshotPort);
    }

    @Override
    public void sincronizarCatalogo() {
        SnapshotCatalogo snapshot =
                catalogoRemotoPort.obtenerSnapshot();

        aplicarSnapshotPort.aplicarSnapshot(
                snapshot.categorias(),
                snapshot.profesionales(),
                snapshot.horarios(),
                snapshot.version()
        );
    }
}
