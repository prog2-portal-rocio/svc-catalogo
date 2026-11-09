
package com.example.svccatalogo.catalogo.domain.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class HorarioSemanal {

    private final Long id;
    private final Long professionalId;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Integer slotDurationMinutes;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public HorarioSemanal(Long id, Long professionalId, DayOfWeek dayOfWeek,
                          LocalTime startTime, LocalTime endTime,
                          Integer slotDurationMinutes, boolean enabled,
                          Instant createdAt, Instant updatedAt) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del horario debe ser positivo");
        }
        if (professionalId == null || professionalId <= 0) {
            throw new IllegalArgumentException("El ID del profesional debe ser positivo");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("El dia de la semana es obligatorio");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("El horario de inicio debe ser anterior al de fin");
        }
        if (slotDurationMinutes == null || slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("La duracion del turno debe ser positiva");
        }

        this.id = id;
        this.professionalId = professionalId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean tieneBloqueDisponible(LocalDate fecha, LocalTime hora) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (!enabled || !dayOfWeek.equals(fecha.getDayOfWeek())) {
            return false;
        }

        Duration duracion = Duration.ofMinutes(slotDurationMinutes);
        if (Duration.between(startTime, endTime).compareTo(duracion) < 0) {
            return false;
        }
        if (hora == null) {
            return true;
        }

        Duration desdeInicio = Duration.between(startTime, hora);
        return !desdeInicio.isNegative()
                && Duration.between(hora, endTime).compareTo(duracion) >= 0
                && desdeInicio.toNanos() % duracion.toNanos() == 0;
    }

    public Long getId() { return id; }
    public Long getProfessionalId() { return professionalId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
