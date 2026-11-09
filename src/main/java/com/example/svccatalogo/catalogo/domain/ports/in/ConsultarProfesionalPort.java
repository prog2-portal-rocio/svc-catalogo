package com.example.svccatalogo.catalogo.domain.ports.in;

import com.example.svccatalogo.catalogo.domain.model.ProfesionalConCategoria;

public interface ConsultarProfesionalPort {

    ProfesionalConCategoria consultarProfesional(Long id);
}
