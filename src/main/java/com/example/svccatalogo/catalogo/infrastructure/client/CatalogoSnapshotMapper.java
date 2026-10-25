package com.example.svccatalogo.catalogo.infrastructure.client;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.CategoriaResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.ProfesionalResponseDto;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto.HorarioSemanalResponseDto;

import java.util.Objects;

public class CatalogoSnapshotMapper {

    public SnapshotCatalogo toDomain(SnapshotResponseDto dto) {
        Objects.requireNonNull(dto, "El snapshot es obligatorio");
        Objects.requireNonNull(dto.snapshotVersion(),
                "La version del snapshot es obligatoria");
        Objects.requireNonNull(dto.professionalCategories(),
                "Las categorias son obligatorias");
        Objects.requireNonNull(dto.professionals(),
                "Los profesionales son obligatorios");
        Objects.requireNonNull(dto.weeklySchedules(),
                "Los horarios son obligatorios");

        return new SnapshotCatalogo(
                dto.snapshotVersion(),
                dto.professionalCategories().stream()
                        .map(this::toCategoria)
                        .toList(),
                dto.professionals().stream()
                        .map(this::toProfesional)
                        .toList(),
                dto.weeklySchedules().stream()
                        .map(this::toHorarioSemanal)
                        .toList()
        );
    }

    private Categoria toCategoria(CategoriaResponseDto dto) {
        Objects.requireNonNull(dto, "La categoria no puede ser nula");
        return new Categoria(
                dto.id(),
                dto.name(),
                dto.description(),
                Objects.requireNonNull(dto.enabled(), "enabled es obligatorio"),
                dto.createdAt(),
                dto.updatedAt()
        );
    }

    private Profesional toProfesional(ProfesionalResponseDto dto) {
        Objects.requireNonNull(dto, "El profesional no puede ser nulo");
        return new Profesional(
                dto.id(),
                dto.categoryId(),
                dto.firstName(),
                dto.lastName(),
                Objects.requireNonNull(dto.enabled(), "enabled es obligatorio"),
                dto.createdAt(),
                dto.updatedAt()
        );
    }

    private HorarioSemanal toHorarioSemanal(HorarioSemanalResponseDto dto) {
        Objects.requireNonNull(dto, "El horario no puede ser nulo");
        return new HorarioSemanal(
                dto.id(),
                dto.professionalId(),
                dto.dayOfWeek(),
                dto.startTime(),
                dto.endTime(),
                dto.slotDurationMinutes(),
                Objects.requireNonNull(dto.enabled(), "enabled es obligatorio"),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
