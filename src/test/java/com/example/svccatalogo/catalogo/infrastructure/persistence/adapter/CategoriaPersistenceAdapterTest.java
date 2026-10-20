package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
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
class CategoriaPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Test
    void guardaYRecuperaUnaCategoria() {
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

        Categoria recuperada = categoriaRepository.findById(1L)
                .orElseThrow();

        assertEquals(1L, recuperada.getId());
        assertEquals("Peluquería", recuperada.getName());
        assertEquals("Servicios de cabello", recuperada.getDescription());
        assertTrue(recuperada.isEnabled());
        assertEquals(fecha, recuperada.getCreatedAt());
        assertEquals(fecha, recuperada.getUpdatedAt());
    }

    @Test
    void listaLasCategoriasGuardadas() {
        Categoria categoria = new Categoria(
                2L,
                "Masajes",
                null,
                true,
                null,
                null
        );

        categoriaRepository.save(categoria);

        assertTrue(categoriaRepository.findAll().stream()
                .anyMatch(c -> c.getId().equals(2L)));
    }
}
