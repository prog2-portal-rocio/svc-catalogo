package com.example.svccatalogo.catalogo.infrastructure.startup;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ReconciliacionCatalogoInicioAdapterTest {

    @Test
    void programaUnaSolaTareaSinBloquearElHiloDeArranque() {
        var port = mock(ReconciliarCatalogoPort.class);
        List<Runnable> tareas = new ArrayList<>();
        Executor executor = tareas::add;
        var adapter = new ReconciliacionCatalogoInicioAdapter(port, executor);

        adapter.iniciar();
        adapter.iniciar();

        verifyNoInteractions(port);
        assertEquals(1, tareas.size());
        tareas.getFirst().run();
        verify(port).reconciliarCatalogo();
    }

    @Test
    void informaElFalloSinAbortarElServicioNiReintentar(CapturedOutput output) {
        var port = mock(ReconciliarCatalogoPort.class);
        doThrow(new IllegalStateException("Fuente no disponible")).when(port).reconciliarCatalogo();
        var adapter = new ReconciliacionCatalogoInicioAdapter(port, Runnable::run);

        assertDoesNotThrow(adapter::iniciar);
        adapter.iniciar();

        verify(port).reconciliarCatalogo();
        verifyNoMoreInteractions(port);
        assertTrue(output.getAll().contains("su vigencia no pudo confirmarse"));
        assertFalse(output.getAll().contains("completada"));
    }
}
