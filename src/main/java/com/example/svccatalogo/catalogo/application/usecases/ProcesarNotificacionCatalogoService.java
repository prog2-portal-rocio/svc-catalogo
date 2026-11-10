package com.example.svccatalogo.catalogo.application.usecases;

import com.example.svccatalogo.catalogo.domain.model.NotificacionCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.domain.ports.in.ProcesarNotificacionCatalogoUseCase;
import com.example.svccatalogo.catalogo.domain.ports.in.SincronizarCatalogoIncrementalPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EventoCatalogoProcesadoPort;
import com.example.svccatalogo.catalogo.domain.ports.out.EstadoSincronizacionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ProcesarNotificacionCatalogoService
        implements ProcesarNotificacionCatalogoUseCase {

    private final EventoCatalogoProcesadoPort eventosPort;
    private final ConsultarEstadoSincronizacionPort estadoPort;
    private final SincronizarCatalogoIncrementalPort sincronizarPort;
    private final EstadoSincronizacionRepositoryPort estadoRepository;

    public ProcesarNotificacionCatalogoService(
            EventoCatalogoProcesadoPort eventosPort,
            ConsultarEstadoSincronizacionPort estadoPort,
            SincronizarCatalogoIncrementalPort sincronizarPort,
            EstadoSincronizacionRepositoryPort estadoRepository) {

        this.eventosPort = Objects.requireNonNull(eventosPort);
        this.estadoPort = Objects.requireNonNull(estadoPort);
        this.sincronizarPort = Objects.requireNonNull(sincronizarPort);
        this.estadoRepository = Objects.requireNonNull(estadoRepository);
    }

    @Override
    @Transactional(timeoutString = "${catalogo.sincronizacion.transaction-timeout-seconds:60}")
    public void procesar(NotificacionCatalogo notificacion) {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria");

        // Mismo bloqueo y orden de adquisicion que la reconciliacion inicial.
        estadoRepository.obtenerEstadoConBloqueo();

        // El registro y los cambios del catalogo se confirman en la misma transaccion.
        // Si falla la sincronizacion o no alcanza la version notificada, se revierten.
        boolean eventoNuevo = eventosPort.registrarSiNoExiste(
                notificacion.eventId(),
                notificacion.nuevaVersion()
        );

        if (!eventoNuevo) {
            return;
        }

        Long versionLocal =
                estadoPort.consultarEstado().getAppliedVersion();

        if (versionLocal == null
                || versionLocal < notificacion.nuevaVersion()) {
            sincronizarPort.sincronizarCatalogoIncremental();
        }

        Long versionFinal =
                estadoPort.consultarEstado().getAppliedVersion();

        if (versionFinal == null
                || versionFinal < notificacion.nuevaVersion()) {
            throw new IllegalStateException(
                    "No se alcanzo la version notificada del catalogo"
            );
        }
    }
}
