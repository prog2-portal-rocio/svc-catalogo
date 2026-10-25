package com.example.svccatalogo.catalogo.infrastructure.client.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public record SnapshotResponseDto(
        Long snapshotVersion,
        Instant generatedAt,
        List<CategoriaResponseDto> professionalCategories,
        List<ProfesionalResponseDto> professionals,
        List<HorarioSemanalResponseDto> weeklySchedules
) {

    public record CategoriaResponseDto(
            Long id,
            String name,
            String description,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ProfesionalResponseDto(
            Long id,
            Long categoryId,
            String firstName,
            String lastName,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record HorarioSemanalResponseDto(
            Long id,
            Long professionalId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes,
            Boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
