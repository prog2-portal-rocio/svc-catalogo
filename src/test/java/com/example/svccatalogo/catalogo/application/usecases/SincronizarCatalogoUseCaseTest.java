package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.AplicarSnapshotCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SincronizarCatalogoUseCaseTest {

    @Test
    void debeObtenerYAplicarSnapshot() {
        // Simula los dos puertos.
        CatalogoRemotoPort remoto = mock(CatalogoRemotoPort.class);
        AplicarSnapshotCatalogoPort aplicacion =
                mock(AplicarSnapshotCatalogoPort.class);

        // snapshot de prueba.
        SnapshotCatalogo snapshot = new SnapshotCatalogo(
                1L,
                List.of(),
                List.of(),
                List.of()
        );

        // qué devolverá el servidor simulado.
        when(remoto.obtenerSnapshot()).thenReturn(snapshot);

        SincronizarCatalogoUseCase casoDeUso =
                new SincronizarCatalogoUseCase(remoto, aplicacion);

        // Ejecuto la sincronización.
        casoDeUso.sincronizarCatalogo();

        // Verifica que se obtuvo el snapshot.
        verify(remoto).obtenerSnapshot();

        // Verifica que se solicitó aplicar la versión recibida.
        verify(aplicacion).aplicarSnapshot(
                snapshot.categorias(),
                snapshot.profesionales(),
                snapshot.horarios(),
                1L
        );
    }

    @Test
    void noDebeAplicarSnapshotSiFallaLaConsultaRemota() {
        CatalogoRemotoPort remoto = mock(CatalogoRemotoPort.class);
        AplicarSnapshotCatalogoPort aplicacion =
                mock(AplicarSnapshotCatalogoPort.class);

        when(remoto.obtenerSnapshot())
                .thenThrow(new IllegalStateException(
                        "Servicio de catedra no disponible"
                ));

        SincronizarCatalogoUseCase casoDeUso =
                new SincronizarCatalogoUseCase(remoto, aplicacion);

        assertThrows(
                IllegalStateException.class,
                casoDeUso::sincronizarCatalogo
        );

        verify(remoto).obtenerSnapshot();
        verifyNoInteractions(aplicacion);
    }
}
