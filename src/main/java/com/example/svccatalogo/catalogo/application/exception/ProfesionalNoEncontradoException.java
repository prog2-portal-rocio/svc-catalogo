package com.example.svccatalogo.catalogo.application.exception;

public class ProfesionalNoEncontradoException extends RuntimeException {

    public ProfesionalNoEncontradoException(Long id) {
        super("No existe el profesional con ID " + id);
    }
}
