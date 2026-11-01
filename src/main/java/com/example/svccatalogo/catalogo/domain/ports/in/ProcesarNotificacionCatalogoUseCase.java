package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;

public interface ProcesarNotificacionCatalogoUseCase {

    void procesar(NotificacionCatalogo notificacion);
}
