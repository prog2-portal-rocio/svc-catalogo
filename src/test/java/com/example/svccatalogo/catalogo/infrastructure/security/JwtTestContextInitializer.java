package com.example.svccatalogo.catalogo.infrastructure.security;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

public class JwtTestContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final KeyPair CLAVES = generarClaves();
    private static final String PUBLIC_KEY_LOCATION = guardarClavePublica();

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "jwt-de-prueba", Map.of(
                        "catalogo.security.jwt.public-key-location", PUBLIC_KEY_LOCATION,
                        "catalogo.security.jwt.issuer", "svc-turnos",
                        "catalogo.security.jwt.audience", "svc-catalogo-api"
                )));
    }

    static KeyPair claves() {
        return CLAVES;
    }

    static String ubicacionClavePublica() {
        return PUBLIC_KEY_LOCATION;
    }

    static KeyPair generarClaves() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudieron generar las claves de prueba", e);
        }
    }

    private static String guardarClavePublica() {
        try {
            var archivo = Files.createTempFile("catalogo-jwt-public-", ".pem");
            archivo.toFile().deleteOnExit();
            String contenido = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{'\n'})
                            .encodeToString(CLAVES.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(archivo, contenido, StandardCharsets.US_ASCII);
            return archivo.toUri().toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la clave publica de prueba", e);
        }
    }
}
