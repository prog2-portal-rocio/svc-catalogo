package com.example.svccatalogo.catalogo.application.exception;

public class FuenteCatalogoNoDisponibleException
        extends RuntimeException {

    public FuenteCatalogoNoDisponibleException(
            String mensaje,
            Throwable causa) {
        super(mensaje, causa);
    }
}
