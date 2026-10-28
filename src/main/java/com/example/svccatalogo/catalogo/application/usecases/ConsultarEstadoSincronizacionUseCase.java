package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ConsultarEstadoSincronizacionUseCase
        implements ConsultarEstadoSincronizacionPort {

    private final EstadoSincronizacionRepositoryPort estadoRepositoryPort;

    public ConsultarEstadoSincronizacionUseCase(
            EstadoSincronizacionRepositoryPort estadoRepositoryPort) {

        this.estadoRepositoryPort =
                Objects.requireNonNull(estadoRepositoryPort);
    }

    @Override
    public EstadoSincronizacion consultarEstado() {
        return estadoRepositoryPort.obtenerEstado();
    }
}
