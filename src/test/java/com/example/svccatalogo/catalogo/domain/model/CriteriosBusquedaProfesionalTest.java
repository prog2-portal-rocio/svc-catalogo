package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CriteriosBusquedaProfesionalTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "\u2003"})
    void debeOmitirNombreNuloOVacio(String nombre) {
        var criterios = new CriteriosBusquedaProfesional(null, nombre, null);

        assertNull(criterios.nombre());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "  Ana  |Ana",
            "  Ana Maria  |Ana Maria",
            "  Ana  Maria  |Ana  Maria",
            "  García_%  |García_%"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void debeQuitarSoloEspaciosDeLosExtremos(String nombre, String esperado) {
        var criterios = new CriteriosBusquedaProfesional(1L, nombre, false);

        assertEquals(esperado, criterios.nombre());
        assertEquals(1L, criterios.categoriaId());
        assertEquals(Boolean.FALSE, criterios.habilitado());
    }
}
