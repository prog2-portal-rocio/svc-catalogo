package com.example.svccatalogo.catalogo.domain.ports.out;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;

import java.time.Instant;

public interface EstadoSincronizacionRepositoryPort {

    EstadoSincronizacion obtenerEstado();

    // Mantiene el bloqueo hasta finalizar la transaccion del caso de uso.
    EstadoSincronizacion obtenerEstadoConBloqueo();

    // No modifica la version ni la fecha del ultimo catalogo aplicado.
    void registrarResultadoIntento(Instant finalizadoEn, boolean exitoso);

    EstadoSincronizacion guardarEstado(EstadoSincronizacion estado);
}
