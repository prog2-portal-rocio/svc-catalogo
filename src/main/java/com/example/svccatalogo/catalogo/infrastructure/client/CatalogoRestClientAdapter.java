package com.example.svccatalogo.catalogo.infrastructure.client;

import com.example.svccatalogo.catalogo.domain.model.SnapshotCatalogo;
import com.example.svccatalogo.catalogo.domain.ports.out.CatalogoRemotoPort;
import com.example.svccatalogo.catalogo.infrastructure.client.dto.SnapshotResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Component
public class CatalogoRestClientAdapter implements CatalogoRemotoPort {

    private final RestClient restClient;
    private final CatalogoSnapshotMapper mapper;
    private final String baseUrl;
    private final String token;

    @Autowired
    public CatalogoRestClientAdapter(
            @Value("${catedra.api.base-url:}") String baseUrl,
            @Value("${catedra.api.token:}") String token) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.mapper = new CatalogoSnapshotMapper();
        this.baseUrl = baseUrl;
        this.token = token;
    }
    CatalogoRestClientAdapter(
            RestClient restClient,
            CatalogoSnapshotMapper mapper,
            String baseUrl,
            String token) {

        this.restClient = java.util.Objects.requireNonNull(restClient);
        this.mapper = java.util.Objects.requireNonNull(mapper);
        this.baseUrl = baseUrl;
        this.token = token;
    }

    @Override
    public SnapshotCatalogo obtenerSnapshot() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "La URL de la API de catedra no esta configurada"
            );
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "El JWT tecnico de catedra no esta configurado"
            );
        }

        SnapshotResponseDto respuesta = restClient.get()
                .uri(baseUrl + "/api/synchronization/snapshot")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(SnapshotResponseDto.class);

        if (respuesta == null) {
            throw new IllegalStateException(
                    "La API de catedra devolvio un snapshot vacio"
            );
        }

        return mapper.toDomain(respuesta);
    }
}
