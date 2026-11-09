package com.example.svccatalogo.catalogo.infrastructure.web.controller;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarProfesionalPort;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarHorariosProfesionalPort;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.infrastructure.web.dto.ProfesionalResponse;
import com.example.svccatalogo.catalogo.infrastructure.web.dto.ProfesionalDetalleResponse;
import com.example.svccatalogo.catalogo.infrastructure.web.dto.HorarioSemanalResponse;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/catalogo/profesionales")
public class ProfesionalController {

    private final BuscarProfesionalesPort buscarProfesionalesPort;
    private final ConsultarProfesionalPort consultarProfesionalPort;
    private final ConsultarHorariosProfesionalPort consultarHorariosPort;

    public ProfesionalController(BuscarProfesionalesPort buscarProfesionalesPort,
                                 ConsultarProfesionalPort consultarProfesionalPort,
                                 ConsultarHorariosProfesionalPort consultarHorariosPort) {
        this.buscarProfesionalesPort = buscarProfesionalesPort;
        this.consultarProfesionalPort = consultarProfesionalPort;
        this.consultarHorariosPort = consultarHorariosPort;
    }

    @InitBinder
    void configurarParametros(WebDataBinder binder) {
        binder.registerCustomEditor(Boolean.class, new CustomBooleanEditor("true", "false", true));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProfesionalResponse>> buscarProfesionales(
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "habilitado", required = false) Boolean habilitado,
            @RequestParam(name = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "hora", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora) {

        if (categoriaId != null && categoriaId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoriaId debe ser mayor que cero");
        }

        var criterios = new CriteriosBusquedaProfesional(categoriaId, nombre, habilitado, fecha, hora);
        List<ProfesionalResponse> respuesta = buscarProfesionalesPort.buscarProfesionales(criterios)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfesionalDetalleResponse> consultarProfesional(@PathVariable("id") Long id) {
        var detalle = consultarProfesionalPort.consultarProfesional(id);
        var profesional = detalle.profesional();
        var categoria = detalle.categoria();
        var respuesta = new ProfesionalDetalleResponse(
                profesional.getId(), profesional.getCategoryId(), profesional.getFirstName(),
                profesional.getLastName(), profesional.isEnabled(), profesional.getCreatedAt(),
                profesional.getUpdatedAt(), new ProfesionalDetalleResponse.CategoriaResponse(
                        categoria.getId(), categoria.getName(), categoria.getDescription(),
                        categoria.isEnabled(), categoria.getCreatedAt(), categoria.getUpdatedAt()));
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping(value = "/{id}/horarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HorarioSemanalResponse>> consultarHorarios(@PathVariable("id") Long id) {
        return ResponseEntity.ok(consultarHorariosPort.consultarHorarios(id).stream()
                .map(this::toHorarioResponse).toList());
    }

    private HorarioSemanalResponse toHorarioResponse(HorarioSemanal horario) {
        return new HorarioSemanalResponse(
                horario.getId(), horario.getProfessionalId(), horario.getDayOfWeek(), horario.getStartTime(),
                horario.getEndTime(), horario.getSlotDurationMinutes(), horario.isEnabled(),
                horario.getCreatedAt(), horario.getUpdatedAt());
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
