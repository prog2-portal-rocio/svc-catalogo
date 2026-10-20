package com.example.svccatalogo.catalogo.infrastructure.persistence.mapper;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.HorarioSemanalEntity;

public class HorarioSemanalMapper {

    private HorarioSemanalMapper() {
    }

    public static HorarioSemanal toDomain(HorarioSemanalEntity entity) {
        return new HorarioSemanal(
                entity.getId(),
                entity.getProfessionalId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getSlotDurationMinutes(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static HorarioSemanalEntity toEntity(HorarioSemanal horario) {
        return new HorarioSemanalEntity(
                horario.getId(),
                horario.getProfessionalId(),
                horario.getDayOfWeek(),
                horario.getStartTime(),
                horario.getEndTime(),
                horario.getSlotDurationMinutes(),
                horario.isEnabled(),
                horario.getCreatedAt(),
                horario.getUpdatedAt()
        );
    }
}
