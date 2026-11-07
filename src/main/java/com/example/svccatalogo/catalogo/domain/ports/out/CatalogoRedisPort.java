package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;
import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.Optional;

public interface CatalogoRedisPort {

    long obtenerVersionActual();

    long obtenerVersionMasAntiguaDisponible();

    Optional<CambiosCatalogo> obtenerCambios(long version);

    Optional<Categoria> obtenerCategoria(long id);

    Optional<Profesional> obtenerProfesional(long id);

    Optional<HorarioSemanal> obtenerHorarioSemanal(long id);
}
