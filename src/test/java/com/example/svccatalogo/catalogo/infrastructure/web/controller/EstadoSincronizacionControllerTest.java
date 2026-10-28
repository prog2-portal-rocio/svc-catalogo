package com.example.svccatalogo.catalogo.infrastructure.web.controller;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EstadoSincronizacionControllerTest {

    private ConsultarEstadoSincronizacionPort consultarEstadoPort;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        consultarEstadoPort = mock(ConsultarEstadoSincronizacionPort.class);

        EstadoSincronizacionController controller =
                new EstadoSincronizacionController(consultarEstadoPort);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void debeDevolverEstadoConSnapshotAplicado() throws Exception {
        Instant fecha = Instant.parse("2026-10-27T20:00:00Z");

        EstadoSincronizacion estado = new EstadoSincronizacion(
                1L,
                fecha,
                fecha,
                "SUCCESS",
                null
        );

        when(consultarEstadoPort.consultarEstado()).thenReturn(estado);

        mockMvc.perform(get("/api/catalogo/sincronizacion/estado"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.appliedVersion").value(1))
                .andExpect(jsonPath("$.phase").value("SUCCESS"))
                .andExpect(jsonPath("$.appliedAt").value("2026-10-27T20:00:00Z"));

        verify(consultarEstadoPort).consultarEstado();
    }

    @Test
    void debeDevolverEstadoInicialSinSnapshot() throws Exception {
        EstadoSincronizacion estadoInicial = new EstadoSincronizacion(
                null,
                null,
                null,
                "NOT_STARTED",
                null
        );

        when(consultarEstadoPort.consultarEstado()).thenReturn(estadoInicial);

        mockMvc.perform(get("/api/catalogo/sincronizacion/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("NOT_STARTED"))
                .andExpect(jsonPath("$.appliedVersion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.appliedAt").value(org.hamcrest.Matchers.nullValue()));

        verify(consultarEstadoPort).consultarEstado();
    }
}
