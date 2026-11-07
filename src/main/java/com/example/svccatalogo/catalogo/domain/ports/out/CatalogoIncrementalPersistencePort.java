package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;

public interface CatalogoIncrementalPersistencePort {

    void aplicarVersion(
            long versionEsperada,
            long nuevaVersion,
            List<Categoria> categorias,
            List<Profesional> profesionales,
            List<HorarioSemanal> horarios
    );
}
