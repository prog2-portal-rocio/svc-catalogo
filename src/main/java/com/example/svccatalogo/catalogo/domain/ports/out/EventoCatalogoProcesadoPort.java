package com.example.svccatalogo.catalogo.domain.ports.out;

public interface EventoCatalogoProcesadoPort {

    boolean registrarSiNoExiste(String eventId, long version);
}
