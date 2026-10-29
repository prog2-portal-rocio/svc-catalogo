package com.example.svccatalogo.catalogo.domain.ports.out;

import java.util.Optional;

public interface CatalogoRedisPort {

    long obtenerVersionActual();

    long obtenerVersionMasAntiguaDisponible();

    Optional<String> obtenerCambios(long version);
}
