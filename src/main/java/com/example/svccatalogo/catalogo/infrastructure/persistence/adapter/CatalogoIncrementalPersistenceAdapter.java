package com.example.svccatalogo.catalogo.infrastructure.persistence.adapter;

import com.example.svccatalogo.catalogo.domain.model.Categoria;
import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;
import com.example.svccatalogo.catalogo.domain.model.Profesional;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoIncrementalPersistencePort;
import com.example.svccatalogo.catalogo.infrastructure.persistence.entity.EstadoSincronizacionEntity;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.CategoriaMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.HorarioSemanalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.mapper.ProfesionalMapper;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.CategoriaJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.EstadoSincronizacionJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.HorarioSemanalJpaRepository;
import com.example.svccatalogo.catalogo.infrastructure.persistence.repository.ProfesionalJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class CatalogoIncrementalPersistenceAdapter
        implements CatalogoIncrementalPersistencePort {

    private static final Long ESTADO_ID = 1L;

    private final CategoriaJpaRepository categorias;
    private final ProfesionalJpaRepository profesionales;
    private final HorarioSemanalJpaRepository horarios;
    private final EstadoSincronizacionJpaRepository estados;

    public CatalogoIncrementalPersistenceAdapter(
            CategoriaJpaRepository categorias,
            ProfesionalJpaRepository profesionales,
            HorarioSemanalJpaRepository horarios,
            EstadoSincronizacionJpaRepository estados
    ) {
        this.categorias = categorias;
        this.profesionales = profesionales;
        this.horarios = horarios;
        this.estados = estados;
    }

    @Override
    @Transactional
    public void aplicarVersion(
            long versionEsperada,
            long nuevaVersion,
            List<Categoria> categoriasActualizadas,
            List<Profesional> profesionalesActualizados,
            List<HorarioSemanal> horariosActualizados
    ) {
        Objects.requireNonNull(categoriasActualizadas);
        Objects.requireNonNull(profesionalesActualizados);
        Objects.requireNonNull(horariosActualizados);

        if (versionEsperada < 0
                || nuevaVersion <= versionEsperada) {
            throw new IllegalArgumentException(
                    "La nueva version debe ser mayor que la esperada"
            );
        }

        EstadoSincronizacionEntity estado = estados
                .buscarPorIdConBloqueo(ESTADO_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado de sincronizacion"
                ));

        Long versionActual = estado.getAppliedVersion();

        if (versionActual == null) {
            throw new IllegalStateException(
                    "Se requiere un snapshot inicial"
            );
        }

        if (versionActual >= nuevaVersion) {
            return;
        }

        if (versionActual != versionEsperada) {
            throw new IllegalStateException(
                    "La version local no coincide con la esperada"
            );
        }

        categorias.saveAllAndFlush(
                categoriasActualizadas.stream()
                        .map(CategoriaMapper::toEntity)
                        .toList()
        );

        profesionales.saveAllAndFlush(
                profesionalesActualizados.stream()
                        .map(ProfesionalMapper::toEntity)
                        .toList()
        );

        horarios.saveAllAndFlush(
                horariosActualizados.stream()
                        .map(HorarioSemanalMapper::toEntity)
                        .toList()
        );

        Instant ahora = Instant.now();

        estados.save(new EstadoSincronizacionEntity(
                ESTADO_ID,
                nuevaVersion,
                ahora,
                ahora,
                "SUCCESS",
                "Version incremental aplicada"
        ));
    }
}
