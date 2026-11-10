package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;

public interface EstadoSincronizacionRepositoryPort {

    EstadoSincronizacion obtenerEstado();

    // Mantiene el bloqueo hasta finalizar la transaccion del caso de uso.
    EstadoSincronizacion obtenerEstadoConBloqueo();

    EstadoSincronizacion guardarEstado(EstadoSincronizacion estado);
}
