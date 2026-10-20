package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;

public interface CatalogoSnapshotPersistencePort {

    void reemplazarCatalogo(
            List<Categoria> categorias,
            List<Profesional> profesionales,
            List<HorarioSemanal> horarios,
            long version
    );
}
