package com.example.svccatalogo.catalogo.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record HorarioSemanalResponse(
        Long id,
        Long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
