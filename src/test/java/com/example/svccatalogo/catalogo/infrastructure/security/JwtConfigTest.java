package com.example.svccatalogo.catalogo.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtConfigTest {

    @TempDir
    Path directorio;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class)
            .withPropertyValues(
                    "catalogo.security.jwt.issuer=svc-turnos",
                    "catalogo.security.jwt.audience=svc-catalogo-api");

    @Test
    void creaDecoderConConfiguracionValida() {
        runner.withPropertyValues("catalogo.security.jwt.public-key-location="
                        + JwtTestContextInitializer.ubicacionClavePublica())
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(JwtDecoder.class));
                });
    }

    @Test
    void impideArrancarSinClavePublica() {
        runner.run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void impideArrancarConArchivoInexistente() {
        runner.withPropertyValues("catalogo.security.jwt.public-key-location="
                        + directorio.resolve("inexistente.pem").toUri())
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void impideArrancarConArchivoQueNoEsUnaClavePublica() throws IOException {
        Path archivo = directorio.resolve("invalida.pem");
        Files.writeString(archivo, "No es una clave publica RSA");

        runner.withPropertyValues("catalogo.security.jwt.public-key-location=" + archivo.toUri())
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"catalogo.security.jwt.issuer", "catalogo.security.jwt.audience"})
    void impideArrancarConParametrosVacios(String propiedad) {
        runner.withPropertyValues(
                        "catalogo.security.jwt.public-key-location="
                                + JwtTestContextInitializer.ubicacionClavePublica(),
                        propiedad + "=")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
