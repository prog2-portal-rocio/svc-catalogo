package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class SincronizarCatalogoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @MockitoBean
    private CatalogoRemotoPort catalogoRemotoPort;

    @Autowired
    private SincronizarCatalogoPort sincronizarCatalogoPort;

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararBaseDeDatos() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE horarios_semanales, profesionales, categorias"
        );

        jdbcTemplate.update("""
                UPDATE estado_sincronizacion
                SET applied_version = NULL,
                    applied_at = NULL,
                    last_attempt_at = NULL,
                    phase = 'NOT_STARTED',
                    safe_diagnostic = 'Sin sincronizaciones realizadas'
                WHERE id = 1
                """);
    }

    @Test
    void debeObtenerYGuardarSnapshotEnPostgreSQL() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                101L,
                "Cardiologia",
                "Atencion cardiologica",
                true,
                fecha,
                fecha
        );

        SnapshotCatalogo snapshot = new SnapshotCatalogo(
                1L,
                List.of(categoria),
                List.of(),
                List.of()
        );

        when(catalogoRemotoPort.obtenerSnapshot())
                .thenReturn(snapshot);

        sincronizarCatalogoPort.sincronizarCatalogo();

        assertEquals(1, categoriaRepository.findAll().size());
        assertEquals(
                "Cardiologia",
                categoriaRepository.findById(101L)
                        .orElseThrow()
                        .getName()
        );

        var estado = estadoRepository.obtenerEstado();

        assertEquals(1L, estado.getAppliedVersion());
        assertEquals("SUCCESS", estado.getPhase());

        verify(catalogoRemotoPort).obtenerSnapshot();
    }
}
