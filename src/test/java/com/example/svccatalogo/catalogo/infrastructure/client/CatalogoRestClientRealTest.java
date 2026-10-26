package com.example.svccatalogo.catalogo.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(
        named = "CATEDRA_API_TOKEN",
        matches = ".+"
)
class CatalogoRestClientRealTest {

    @Test
    void debeObtenerSnapshotDesdeLaCatedra() {
        String baseUrl = Objects.requireNonNull(
                System.getenv("CATEDRA_API_BASE_URL")
        );

        String token = Objects.requireNonNull(
                System.getenv("CATEDRA_API_TOKEN")
        );

        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        RestClient.create(),
                        new CatalogoSnapshotMapper(),
                        baseUrl,
                        token
                );

        var snapshot = adaptador.obtenerSnapshot();

        assertNotNull(snapshot);
        assertTrue(snapshot.version() >= 0);
        assertNotNull(snapshot.categorias());
        assertNotNull(snapshot.profesionales());
        assertNotNull(snapshot.horarios());

        System.out.println(
                "Version del snapshot: " + snapshot.version()
        );
        System.out.println(
                "Categorias: " + snapshot.categorias().size()
        );
        System.out.println(
                "Profesionales: " + snapshot.profesionales().size()
        );
        System.out.println(
                "Horarios semanales: " + snapshot.horarios().size()
        );
    }
}
