package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;

public interface CatalogoRemotoPort {

    SnapshotCatalogo obtenerSnapshot();
}
