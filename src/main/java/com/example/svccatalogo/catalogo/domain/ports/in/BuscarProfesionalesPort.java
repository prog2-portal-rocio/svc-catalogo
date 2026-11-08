package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.CriteriosBusquedaProfesional;
import com.example.svccatalogo.catalogo.domain.model.Profesional;

import java.util.List;

public interface BuscarProfesionalesPort {

    List<Profesional> buscarProfesionales(CriteriosBusquedaProfesional criterios);
}
