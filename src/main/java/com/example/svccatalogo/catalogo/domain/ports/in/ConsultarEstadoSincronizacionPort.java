package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;

public interface ConsultarEstadoSincronizacionPort {

    EstadoSincronizacion consultarEstado();
}
