package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class EstadoSincronizacionPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @Test
    void recuperaEstadoInicialCreadoPorFlyway() {
        EstadoSincronizacion estado = estadoRepository.obtenerEstado();

        assertNull(estado.getAppliedVersion());
        assertNull(estado.getAppliedAt());
        assertEquals("NOT_STARTED", estado.getPhase());
        assertEquals(
                "Sin sincronizaciones realizadas",
                estado.getSafeDiagnostic()
        );
    }

    @Test
    void guardaYRecuperaEstadoDeSincronizacion() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        EstadoSincronizacion nuevoEstado = new EstadoSincronizacion(
                15L,
                fecha,
                fecha,
                "SUCCESS",
                "Sincronizacion completada"
        );

        estadoRepository.guardarEstado(nuevoEstado);

        EstadoSincronizacion recuperado = estadoRepository.obtenerEstado();

        assertEquals(15L, recuperado.getAppliedVersion());
        assertEquals(fecha, recuperado.getAppliedAt());
        assertEquals(fecha, recuperado.getLastAttemptAt());
        assertEquals("SUCCESS", recuperado.getPhase());
        assertEquals(
                "Sincronizacion completada",
                recuperado.getSafeDiagnostic()
        );
    }
}
