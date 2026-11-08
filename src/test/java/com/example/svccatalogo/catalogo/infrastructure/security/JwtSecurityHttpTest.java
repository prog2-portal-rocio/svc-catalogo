package com.example.svccatalogo.catalogo.infrastructure.security;

import com.example.svccatalogo.catalogo.domain.model.EstadoSincronizacion;
import com.example.svccatalogo.catalogo.domain.ports.in.ConsultarEstadoSincronizacionPort;
import com.example.svccatalogo.catalogo.infrastructure.web.controller.EstadoSincronizacionController;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EstadoSincronizacionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ContextConfiguration(initializers = JwtTestContextInitializer.class)
class JwtSecurityHttpTest {

    private static final String ESTADO = "/api/catalogo/sincronizacion/estado";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarEstadoSincronizacionPort estadoPort;

    @Test
    void rechazaSolicitudSinToken() throws Exception {
        mockMvc.perform(get(ESTADO))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));

        verifyNoInteractions(estadoPort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokensInvalidos")
    void rechazaTokensInvalidos(String descripcion, String token) throws Exception {
        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(estadoPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "catalogo:read", "catalogo:sync:read-extra"})
    void rechazaTokenValidoSinPermiso(String scope) throws Exception {
        String token = firmar(claimsValidos().claim("scope", scope).build());

        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estadoPort);
    }

    @Test
    void rechazaTokenValidoSinClaimScope() throws Exception {
        String token = firmar(claimsValidos().claim("scope", null).build());

        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estadoPort);
    }

    @Test
    void permiteTokenConScopeYConservaRespuestaDeEstado() throws Exception {
        Instant fecha = Instant.parse("2026-10-27T20:00:00Z");
        when(estadoPort.consultarEstado()).thenReturn(new EstadoSincronizacion(
                7L, fecha, fecha, "SUCCESS", "Version incremental aplicada"));
        String token = firmar(claimsValidos()
                .claim("scope", "catalogo:read catalogo:sync:read").build());

        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"appliedVersion":7,"appliedAt":"2026-10-27T20:00:00Z",
                         "lastAttemptAt":"2026-10-27T20:00:00Z","phase":"SUCCESS",
                         "safeDiagnostic":"Version incremental aplicada"}
                        """))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(estadoPort).consultarEstado();
    }

    @Test
    void permiteConsultarEstadoInicialConTokenAutorizado() throws Exception {
        when(estadoPort.consultarEstado()).thenReturn(new EstadoSincronizacion(
                null, null, null, "NOT_STARTED", null));
        String token = firmar(claimsValidos().build());

        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"appliedVersion":null,"appliedAt":null,"lastAttemptAt":null,
                         "phase":"NOT_STARTED","safeDiagnostic":null}
                        """));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/profesionales", "/api/catalogo/sincronizacion/estado/otra", "/",
            "/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/api"
    })
    void deniegaRutasNoAutorizadasAunqueElTokenSeaValido(String ruta) throws Exception {
        String token = firmar(claimsValidos().build());

        mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estadoPort);
    }

    @Test
    void noPublicaMetadataOAuthSinAutenticacion() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verifyNoInteractions(estadoPort);
    }

    @Test
    void deniegaOtrosMetodosSobreElEndpoint() throws Exception {
        String token = firmar(claimsValidos().build());

        mockMvc.perform(post(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estadoPort);
    }

    @Test
    void noConservaAutenticacionEntreSolicitudes() throws Exception {
        when(estadoPort.consultarEstado()).thenReturn(new EstadoSincronizacion(
                null, null, null, "NOT_STARTED", null));
        String token = firmar(claimsValidos().build());
        mockMvc.perform(get(ESTADO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(ESTADO)).andExpect(status().isUnauthorized());

        verify(estadoPort, times(1)).consultarEstado();
    }

    private static JWTClaimsSet.Builder claimsValidos() {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer("svc-turnos")
                .audience(List.of("svc-turnos-api", "svc-catalogo-api"))
                .subject("123")
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(300)))
                .claim("scope", "catalogo:sync:read");
    }

    private static String firmar(JWTClaimsSet claims) throws JOSEException {
        return firmar(claims, JwtTestContextInitializer.claves(), JWSAlgorithm.RS256);
    }

    private static String firmar(JWTClaimsSet claims, KeyPair claves, JWSAlgorithm algoritmo)
            throws JOSEException {
        SignedJWT token = new SignedJWT(new JWSHeader(algoritmo), claims);
        token.sign(new RSASSASigner(claves.getPrivate()));
        return token.serialize();
    }

    private static Stream<Arguments> tokensInvalidos() throws JOSEException {
        byte[] secreto = new byte[32];
        new SecureRandom().nextBytes(secreto);
        SignedJWT tokenHs256 = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsValidos().build());
        tokenHs256.sign(new MACSigner(secreto));

        return Stream.of(
                Arguments.of("Malformado", "no-es-un-jwt"),
                Arguments.of("Firma con otra clave", firmar(claimsValidos().build(),
                        JwtTestContextInitializer.generarClaves(), JWSAlgorithm.RS256)),
                Arguments.of("Contenido alterado", alterarContenido(firmar(claimsValidos().build()))),
                Arguments.of("RS512 no permitido", firmar(claimsValidos().build(),
                        JwtTestContextInitializer.claves(), JWSAlgorithm.RS512)),
                Arguments.of("HS256 no permitido", tokenHs256.serialize()),
                Arguments.of("Sin firma", new PlainJWT(claimsValidos().build()).serialize()),
                Arguments.of("Vencido", firmar(claimsValidos()
                        .expirationTime(Date.from(Instant.now().minusSeconds(5))).build())),
                Arguments.of("Sin expiracion", firmar(claimsValidos().expirationTime(null).build())),
                Arguments.of("Emisor incorrecto", firmar(claimsValidos().issuer("catedra").build())),
                Arguments.of("Sin emisor", firmar(claimsValidos().issuer(null).build())),
                Arguments.of("Audiencia incorrecta", firmar(claimsValidos().audience("svc-turnos-api").build())),
                Arguments.of("Sin audiencia", firmar(claimsValidos().audience((String) null).build())),
                Arguments.of("Sin sujeto", firmar(claimsValidos().subject(null).build())),
                Arguments.of("Sujeto vacio", firmar(claimsValidos().subject("").build())),
                Arguments.of("Sujeto en blanco", firmar(claimsValidos().subject("   ").build())),
                Arguments.of("Aun no vigente", firmar(claimsValidos()
                        .notBeforeTime(Date.from(Instant.now().plusSeconds(300))).build()))
        );
    }

    private static String alterarContenido(String token) {
        String[] partes = token.split("\\.");
        String contenido = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"sub\":\"123\"", "\"sub\":\"456\"");
        partes[1] = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(contenido.getBytes(StandardCharsets.UTF_8));
        return String.join(".", partes);
    }
}
