package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EnabledIfEnvironmentVariable(
        named = "CATEDRA_API_TOKEN",
        matches = ".+"
)
class SincronizarCatalogoRealIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private SincronizarCatalogoPort sincronizarCatalogoPort;

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private ProfesionalRepositoryPort profesionalRepository;

    @Autowired
    private HorarioSemanalRepositoryPort horarioRepository;

    @Autowired
    private EstadoSincronizacionRepositoryPort estadoRepository;

    @Test
    void debeSincronizarCatalogoRealEnPostgreSQL() {
        sincronizarCatalogoPort.sincronizarCatalogo();

        var estado = estadoRepository.obtenerEstado();

        assertNotNull(estado.getAppliedVersion());
        assertEquals("SUCCESS", estado.getPhase());

        System.out.println(
                "Version aplicada: " + estado.getAppliedVersion()
        );
        System.out.println(
                "Categorias guardadas: " + categoriaRepository.findAll().size()
        );
        System.out.println(
                "Profesionales guardados: " + profesionalRepository.findAll().size()
        );
        System.out.println(
                "Horarios guardados: " + horarioRepository.findAll().size()
        );
    }
}
