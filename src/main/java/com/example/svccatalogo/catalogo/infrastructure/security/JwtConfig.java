package com.example.svccatalogo.catalogo.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${catalogo.security.jwt.public-key-location}") Resource publicKeyResource,
            @Value("${catalogo.security.jwt.issuer}") String issuer,
            @Value("${catalogo.security.jwt.audience}") String audience) throws IOException {

        Assert.hasText(issuer, "El emisor JWT es obligatorio");
        Assert.hasText(audience, "La audiencia JWT es obligatoria");

        RSAPublicKey publicKey;
        try (var input = publicKeyResource.getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(input);
        }
        Assert.notNull(publicKey, "La clave publica RSA es obligatoria");
        Assert.isTrue(publicKey.getModulus().bitLength() >= 2048,
                "La clave publica RSA debe tener al menos 2048 bits");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(issuer),
                new JwtAudienceValidator(audience),
                JwtConfig::validarClaimsObligatorios
        ));
        return decoder;
    }

    private static OAuth2TokenValidatorResult validarClaimsObligatorios(Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getExpiresAt() == null) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "El JWT requiere sujeto no vacio y expiracion", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
