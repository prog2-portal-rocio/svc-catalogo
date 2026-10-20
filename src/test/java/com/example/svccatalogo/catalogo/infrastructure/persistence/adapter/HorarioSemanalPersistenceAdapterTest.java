package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.CategoriaRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class HorarioSemanalPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private CategoriaRepositoryPort categoriaRepository;

    @Autowired
    private ProfesionalRepositoryPort profesionalRepository;

    @Autowired
    private HorarioSemanalRepositoryPort horarioRepository;

    @Test
    void guardaYRecuperaUnHorarioSemanal() {
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");

        Categoria categoria = new Categoria(
                1L, "Peluquería", "Servicios de cabello",
                true, fecha, fecha
        );
        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                10L, 1L, "Ana", "García",
                true, fecha, fecha
        );
        profesionalRepository.save(profesional);

        HorarioSemanal horario = new HorarioSemanal(
                100L,
                10L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30,
                true,
                fecha,
                fecha
        );

        horarioRepository.save(horario);

        HorarioSemanal recuperado = horarioRepository.findById(100L)
                .orElseThrow();

        assertEquals(100L, recuperado.getId());
        assertEquals(10L, recuperado.getProfessionalId());
        assertEquals(DayOfWeek.MONDAY, recuperado.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), recuperado.getStartTime());
        assertEquals(LocalTime.of(13, 0), recuperado.getEndTime());
        assertEquals(30, recuperado.getSlotDurationMinutes());
        assertTrue(recuperado.isEnabled());
        assertEquals(fecha, recuperado.getCreatedAt());
        assertEquals(fecha, recuperado.getUpdatedAt());
    }

    @Test
    void listaLosHorariosGuardados() {
        Categoria categoria = new Categoria(
                2L, "Masajes", null,
                true, null, null
        );
        categoriaRepository.save(categoria);

        Profesional profesional = new Profesional(
                11L, 2L, "Lucas", "Pérez",
                true, null, null
        );
        profesionalRepository.save(profesional);

        HorarioSemanal horario = new HorarioSemanal(
                101L,
                11L,
                DayOfWeek.FRIDAY,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                60,
                false,
                null,
                null
        );
        horarioRepository.save(horario);

        assertTrue(horarioRepository.findAll().stream()
                .anyMatch(h -> h.getId().equals(101L)));
    }
}
