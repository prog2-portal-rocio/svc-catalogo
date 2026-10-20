package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;

public interface EstadoSincronizacionRepositoryPort {

    EstadoSincronizacion obtenerEstado();

    EstadoSincronizacion guardarEstado(EstadoSincronizacion estado);
}
