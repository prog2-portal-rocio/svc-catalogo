package com.example.svccatalogo.catalogo.infrastructure.web.handler;

import com.example.svccatalogo.catalogo.infrastructure.web.controller.ProfesionalController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@RestControllerAdvice(assignableTypes = ProfesionalController.class)
public class ProfesionalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail criteriosInvalidos(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
