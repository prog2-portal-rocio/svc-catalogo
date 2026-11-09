package com.example.svccatalogo.catalogo.infrastructure.web.handler;

import com.example.svccatalogo.catalogo.infrastructure.web.controller.ProfesionalController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(assignableTypes = ProfesionalController.class)
public class ProfesionalExceptionHandler extends ResponseEntityExceptionHandler {
}
