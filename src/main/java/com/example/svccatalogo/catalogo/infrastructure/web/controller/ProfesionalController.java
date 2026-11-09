package com.example.svccatalogo.catalogo.infrastructure.web.controller;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.infrastructure.web.dto.ProfesionalResponse;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/profesionales")
public class ProfesionalController {

    private final BuscarProfesionalesPort buscarProfesionalesPort;

    public ProfesionalController(BuscarProfesionalesPort buscarProfesionalesPort) {
        this.buscarProfesionalesPort = buscarProfesionalesPort;
    }

    @InitBinder
    void configurarParametros(WebDataBinder binder) {
        binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor("true", "false", true));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProfesionalResponse>> buscarProfesionales(
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "habilitado", required = false) Boolean habilitado) {

        if (categoriaId != null && categoriaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoriaId debe ser mayor que cero");
        }

        var criterios = new CriteriosBusquedaProfesional(categoriaId, nombre, habilitado);
        List<ProfesionalResponse> respuesta = buscarProfesionalesPort.buscarProfesionales(criterios)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    private ProfesionalResponse toResponse(Profesional profesional) {
        return new ProfesionalResponse(
                profesional.getId(),
                profesional.getCategoryId(),
                profesional.getFirstName(),
                profesional.getLastName(),
                profesional.isEnabled(),
                profesional.getCreatedAt(),
                profesional.getUpdatedAt()
        );
    }
}
