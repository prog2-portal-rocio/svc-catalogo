package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;

public interface AplicarSnapshotCatalogoPort {

    void aplicarSnapshot(
            List<Categoria> categorias,
            List<Profesional> profesionales,
            List<HorarioSemanal> horarios,
            long version
    );
}

