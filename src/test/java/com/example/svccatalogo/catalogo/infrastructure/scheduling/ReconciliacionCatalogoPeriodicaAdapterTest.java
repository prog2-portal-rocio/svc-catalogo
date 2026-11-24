package com.example.svccatalogo.catalogo.infrastructure.scheduling;

import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReconciliacionCatalogoPeriodicaAdapterTest {
    @Test
    void noSeActivaPorDefecto() {
        new ApplicationContextRunner().withUserConfiguration(ReconciliacionCatalogoPeriodicaAdapter.class)
                .withBean(ReconciliarCatalogoPort.class, () -> mock(ReconciliarCatalogoPort.class))
                .run(contexto -> assertEquals(0, contexto.getBeansOfType(ReconciliacionCatalogoPeriodicaAdapter.class).size()));
    }

    @Test
    void falloNoGeneraBucleYPermiteLaSiguienteEjecucion() {
        var puerto = mock(ReconciliarCatalogoPort.class);
        doThrow(new IllegalStateException("causa externa que no se debe registrar")).doNothing().when(puerto).reconciliarCatalogo();
        var tarea = new ReconciliacionCatalogoPeriodicaAdapter(puerto);
        assertDoesNotThrow(tarea::reconciliar);
        verify(puerto).reconciliarCatalogo();
        assertDoesNotThrow(tarea::reconciliar);
        verify(puerto, times(2)).reconciliarCatalogo();
    }

    @Test
    void seActivaSoloConHabilitacionExplicita() {
        new ApplicationContextRunner().withUserConfiguration(ReconciliacionCatalogoPeriodicaAdapter.class)
                .withBean(ReconciliarCatalogoPort.class, () -> mock(ReconciliarCatalogoPort.class))
                .withPropertyValues("catalogo.reconciliacion-periodica.enabled=true")
                .run(contexto -> assertEquals(1, contexto.getBeansOfType(ReconciliacionCatalogoPeriodicaAdapter.class).size()));
    }
}
