package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.CambiosCatalogo;

import java.util.Optional;

public interface CatalogoRedisPort {

    long obtenerVersionActual();

    long obtenerVersionMasAntiguaDisponible();

    Optional<CambiosCatalogo> obtenerCambios(long version);
}
