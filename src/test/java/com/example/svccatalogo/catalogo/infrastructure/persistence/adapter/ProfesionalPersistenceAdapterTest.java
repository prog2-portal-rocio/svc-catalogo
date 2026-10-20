package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
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
class ProfesionalPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private ProfesionalRepositoryPort profesionalRepository;

    @Test
    void guardaYRecuperaUnProfesionalConSuCategoria() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L,
                "Peluquería",
                "Servicios de cabello",
                true,
                fecha,
                fecha
        );

        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                10L,
                1L,
                "Ana",
                "García",
                true,
                fecha,
                fecha
        );

        profesionalRepository.save(profesional);

        Profesional recuperado = profesionalRepository.findById(10L)
                .orElseThrow();

        assertEquals(10L, recuperado.getId());
        assertEquals(1L, recuperado.getCategoryId());
        assertEquals("Ana", recuperado.getFirstName());
        assertEquals("García", recuperado.getLastName());
        assertTrue(recuperado.isEnabled());
        assertEquals(fecha, recuperado.getCreatedAt());
        assertEquals(fecha, recuperado.getUpdatedAt());
    }

    @Test
    void listaLosProfesionalesGuardados() {
        Categoria categoria = new Categoria(
                2L, "Masajes", null,
                true, null, null
        );

        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                11L, 2L, "Lucas", "Pérez",
                false, null, null
        );

        profesionalRepository.save(profesional);

        assertTrue(profesionalRepository.findAll().stream()
                .anyMatch(p -> p.getId().equals(11L)));
    }
}
