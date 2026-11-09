package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.in.BuscarProfesionalesPort;
import com.example.svccatalogo.catalogo.domain.ports.out.ProfesionalRepositoryPort;
import com.example.svccatalogo.catalogo.domain.ports.out.HorarioSemanalRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BuscarProfesionalesUseCase implements BuscarProfesionalesPort {

    private final ProfesionalRepositoryPort profesionalRepository;
    private final HorarioSemanalRepositoryPort horarioRepository;

    public BuscarProfesionalesUseCase(ProfesionalRepositoryPort profesionalRepository,
                                     HorarioSemanalRepositoryPort horarioRepository) {
        this.profesionalRepository = Objects.requireNonNull(profesionalRepository);
        this.horarioRepository = Objects.requireNonNull(horarioRepository);
    }

    @Override
    public List<Profesional> buscarProfesionales(CriteriosBusquedaProfesional criterios) {
        Objects.requireNonNull(criterios, "Los criterios de busqueda son obligatorios");
        List<Profesional> profesionales = profesionalRepository.buscar(criterios);
        if (criterios.fecha() == null || profesionales.isEmpty()) {
            return profesionales;
        }
        List<Long> ids = profesionales.stream().map(Profesional::getId).toList();
        Set<Long> disponibles = horarioRepository
                .buscarHabilitadosPorProfesionalesYDia(ids, criterios.fecha().getDayOfWeek())
                .stream()
                .filter(horario -> horario.tieneBloqueDisponible(criterios.fecha(), criterios.hora()))
                .map(HorarioSemanal::getProfessionalId)
                .collect(Collectors.toSet());
        return profesionales.stream().filter(profesional -> disponibles.contains(profesional.getId())).toList();
    }
}
