package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.HorarioSemanal;

import java.util.List;

public interface ConsultarHorariosProfesionalPort {

    List<HorarioSemanal> consultarHorarios(Long profesionalId);
}
