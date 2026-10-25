package com.example.svccatalogo.catalogo.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotCatalogoTest {

    @Test
    void debeCrearSnapshotValido() {
        SnapshotCatalogo snapshot = new SnapshotCatalogo(
                7L,
                List.of(),
                List.of(),
                List.of()
        );

        assertEquals(7L, snapshot.version());
        assertTrue(snapshot.categorias().isEmpty());
        assertTrue(snapshot.profesionales().isEmpty());
        assertTrue(snapshot.horarios().isEmpty());
    }

    @Test
    void debeRechazarVersionNegativa() {
        assertThrows(IllegalArgumentException.class, () ->
                new SnapshotCatalogo(
                        -1L,
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void debeRechazarListasNulas() {
        assertThrows(NullPointerException.class, () ->
                new SnapshotCatalogo(1L, null, List.of(), List.of())
        );

        assertThrows(NullPointerException.class, () ->
                new SnapshotCatalogo(1L, List.of(), null, List.of())
        );

        assertThrows(NullPointerException.class, () ->
                new SnapshotCatalogo(1L, List.of(), List.of(), null)
        );
    }

    @Test
    void debeProtegerLasListasDeModificaciones() {
        List<Categoria> categorias = new ArrayList<>();

        SnapshotCatalogo snapshot = new SnapshotCatalogo(
                1L,
                categorias,
                List.of(),
                List.of()
        );

        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.categorias().clear()
        );
    }

    @Test
    void debeConservarSnapshotAunqueCambieLaListaOriginal() {
        List<Categoria> categorias = new ArrayList<>();

        SnapshotCatalogo snapshot = new SnapshotCatalogo(
                1L,
                categorias,
                List.of(),
                List.of()
        );

        Categoria categoria = new Categoria(
                1L,
                "Clinica medica",
                "Atencion clinica general",
                true,
                null,
                null
        );

        categorias.add(categoria);

        assertTrue(snapshot.categorias().isEmpty());
        assertEquals(1, categorias.size());
    }
}
