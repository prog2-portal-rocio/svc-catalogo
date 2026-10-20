package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoSnapshotPersistencePort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EstadoSincronizacionEntity;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.CategoriaMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.ProfesionalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.HorarioSemanalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.CategoriaJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.ProfesionalJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.HorarioSemanalJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EstadoSincronizacionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class CatalogoSnapshotPersistenceAdapter
        implements CatalogoSnapshotPersistencePort {

    private static final Long ESTADO_ID = 1L;

    private final CategoriaJpaRepository categoriaRepository;
    private final ProfesionalJpaRepository profesionalRepository;
    private final HorarioSemanalJpaRepository horarioRepository;
    private final EstadoSincronizacionJpaRepository estadoRepository;

    public CatalogoSnapshotPersistenceAdapter(
            CategoriaJpaRepository categoriaRepository,
            ProfesionalJpaRepository profesionalRepository,
            HorarioSemanalJpaRepository horarioRepository,
            EstadoSincronizacionJpaRepository estadoRepository) {

        this.categoriaRepository = categoriaRepository;
        this.profesionalRepository = profesionalRepository;
        this.horarioRepository = horarioRepository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public void reemplazarCatalogo(
            List<Categoria> categorias,
            List<Profesional> profesionales,
            List<HorarioSemanal> horarios,
            long version) {

        Objects.requireNonNull(categorias, "Las categorias son obligatorias");
        Objects.requireNonNull(profesionales, "Los profesionales son obligatorios");
        Objects.requireNonNull(horarios, "Los horarios son obligatorios");

        if (version < 0) {
            throw new IllegalArgumentException(
                    "La version del catalogo no puede ser negativa"
            );
        }

        EstadoSincronizacionEntity estado = estadoRepository
                .buscarPorIdConBloqueo(ESTADO_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado de sincronizacion"
                ));

        Long versionActual = estado.getAppliedVersion();

        if (versionActual != null && version <= versionActual) {
            return;
        }

        // Eliminar los datos anteriores: de hijos a padres.
        horarioRepository.deleteAllInBatch();
        profesionalRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();

        // Guardar el nuevo snapshot: de padres a hijos.
        categoriaRepository.saveAllAndFlush(
                categorias.stream()
                        .map(CategoriaMapper::toEntity)
                        .toList()
        );

        profesionalRepository.saveAllAndFlush(
                profesionales.stream()
                        .map(ProfesionalMapper::toEntity)
                        .toList()
        );

        horarioRepository.saveAllAndFlush(
                horarios.stream()
                        .map(HorarioSemanalMapper::toEntity)
                        .toList()
        );

        // La version se actualiza solo despues de guardar los datos.
        Instant ahora = Instant.now();

        EstadoSincronizacionEntity nuevoEstado =
                new EstadoSincronizacionEntity(
                        ESTADO_ID,
                        version,
                        ahora,
                        ahora,
                        "SUCCESS",
                        "Snapshot aplicado correctamente"
                );

        estadoRepository.save(nuevoEstado);
    }
}
