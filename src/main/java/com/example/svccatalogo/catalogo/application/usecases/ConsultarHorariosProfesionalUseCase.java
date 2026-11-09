package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.application.exception.ProfesionalNoEncontradoException;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarHorariosProfesionalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ConsultarHorariosProfesionalUseCase implements ConsultarHorariosProfesionalPort {

    private final ProfesionalRepositoryPort profesionales;
    private final HorarioSemanalRepositoryPort horarios;

    public ConsultarHorariosProfesionalUseCase(ProfesionalRepositoryPort profesionales,
                                             HorarioSemanalRepositoryPort horarios) {
        this.profesionales = Objects.requireNonNull(profesionales);
        this.horarios = Objects.requireNonNull(horarios);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioSemanal> consultarHorarios(Long profesionalId) {
        if (profesionalId == null || profesionalId <= 0) {
            throw new IllegalArgumentException("El ID del profesional debe ser positivo");
        }
        profesionales.findById(profesionalId)
                .orElseThrow(() -> new ProfesionalNoEncontradoException(profesionalId));
        return horarios.buscarPorProfesional(profesionalId);
    }
}
