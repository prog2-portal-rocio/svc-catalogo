package com.example.svccatalogo.catalogo.infrastructure.web.controller;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.infrastructure.web.dto.EstadoSincronizacionResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo/sincronizacion")
public class EstadoSincronizacionController {

    private final ConsultarEstadoSincronizacionPort consultarEstadoPort;

    public EstadoSincronizacionController(
            ConsultarEstadoSincronizacionPort consultarEstadoPort) {
        this.consultarEstadoPort = consultarEstadoPort;
    }

    @GetMapping(value = "/estado", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EstadoSincronizacionResponse> consultarEstado() {
        EstadoSincronizacion estado = consultarEstadoPort.consultarEstado();

        EstadoSincronizacionResponse respuesta = new EstadoSincronizacionResponse(
                estado.getAppliedVersion(),
                estado.getAppliedAt(),
                estado.getLastAttemptAt(),
                estado.getPhase(),
                estado.getSafeDiagnostic()
        );

        return ResponseEntity.ok(respuesta);
    }
}
