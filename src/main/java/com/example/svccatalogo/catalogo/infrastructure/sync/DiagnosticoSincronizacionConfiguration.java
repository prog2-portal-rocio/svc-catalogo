package com.example.svccatalogo.catalogo.infrastructure.sync;

import com.example.svccatalogo.catalogo.application.usecases.ProcesarNotificacionCatalogoService;
import com.example.svccatalogo.catalogo.application.usecases.ReconciliarCatalogoUseCase;
import com.example.svccatalogo.catalogo.application.usecases.SincronizarCatalogoIncrementalUseCase;
import com.example.svccatalogo.catalogo.application.usecases.SincronizarCatalogoUseCase;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import com.example.svccatalogo.catalogo.domain.ports.in.ReconciliarCatalogoPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Decoradores exteriores a los proxies transaccionales de los casos de uso existentes. */
@Configuration(proxyBeanMethods = false)
public class DiagnosticoSincronizacionConfiguration {

    @Bean
    @Primary
    public SincronizarCatalogoPort sincronizacionConDiagnostico(
            SincronizarCatalogoUseCase delegado, DiagnosticoSincronizacionExecutor diagnostico) {
        return () -> diagnostico.ejecutar(delegado::sincronizarCatalogo, true);
    }

    @Bean
    @Primary
    public SincronizarCatalogoIncrementalPort incrementalConDiagnostico(
            SincronizarCatalogoIncrementalUseCase delegado, DiagnosticoSincronizacionExecutor diagnostico) {
        return () -> diagnostico.ejecutar(delegado::sincronizarCatalogoIncremental, true);
    }

    @Bean
    @Primary
    public ReconciliarCatalogoPort reconciliacionConDiagnostico(
            ReconciliarCatalogoUseCase delegado, DiagnosticoSincronizacionExecutor diagnostico) {
        return () -> diagnostico.ejecutar(delegado::reconciliarCatalogo, true);
    }

    @Bean
    @Primary
    public ProcesarNotificacionCatalogoUseCase notificacionConDiagnostico(
            ProcesarNotificacionCatalogoService delegado, DiagnosticoSincronizacionExecutor diagnostico) {
        // Descartar una notificacion duplicada no acredita una nueva sincronizacion exitosa.
        return notificacion -> diagnostico.ejecutar(() -> delegado.procesar(notificacion), false);
    }
}
