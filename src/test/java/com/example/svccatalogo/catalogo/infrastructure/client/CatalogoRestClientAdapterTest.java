package com.example.svccatalogo.catalogo.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CatalogoRestClientAdapterTest {

    @Test
    void debeEnviarGetConJwtYObtenerSnapshot() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        builder.build(),
                        new CatalogoSnapshotMapper(),
                        "https://catedra.example.test",
                        "token-de-prueba"
                );

        String json = """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  "professionalCategories": [],
                  "professionals": [],
                  "weeklySchedules": []
                }
                """;

        servidor.expect(requestTo(
                        "https://catedra.example.test/api/synchronization/snapshot"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var snapshot = adaptador.obtenerSnapshot();

        assertEquals(7L, snapshot.version());
        assertTrue(snapshot.categorias().isEmpty());
        assertTrue(snapshot.profesionales().isEmpty());
        assertTrue(snapshot.horarios().isEmpty());

        servidor.verify();
    }

    @Test
    void debeRechazarTokenNoAutorizado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        builder.build(),
                        new CatalogoSnapshotMapper(),
                        "https://catedra.example.test",
                        "token-invalido"
                );

        servidor.expect(requestTo(
                        "https://catedra.example.test/api/synchronization/snapshot"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-invalido"))
                .andRespond(withUnauthorizedRequest());

        assertThrows(
                org.springframework.web.client.HttpClientErrorException.Unauthorized.class,
                adaptador::obtenerSnapshot
        );

        servidor.verify();
    }

    @Test
    void debeInformarErrorDelServidorCentral() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        builder.build(),
                        new CatalogoSnapshotMapper(),
                        "https://catedra.example.test",
                        "token-de-prueba"
                );

        servidor.expect(requestTo(
                        "https://catedra.example.test/api/synchronization/snapshot"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(
                org.springframework.web.client.HttpServerErrorException.class,
                adaptador::obtenerSnapshot
        );

        servidor.verify();
    }

    @Test
    void debeRechazarUrlNoConfigurada() {
        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        RestClient.create(),
                        new CatalogoSnapshotMapper(),
                        "",
                        "token-de-prueba"
                );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                adaptador::obtenerSnapshot
        );

        assertEquals(
                "La URL de la API de catedra no esta configurada",
                error.getMessage()
        );
    }

    @Test
    void debeRechazarTokenNoConfigurado() {
        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        RestClient.create(),
                        new CatalogoSnapshotMapper(),
                        "https://catedra.example.test",
                        ""
                );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                adaptador::obtenerSnapshot
        );

        assertEquals(
                "El JWT tecnico de catedra no esta configurado",
                error.getMessage()
        );
    }

    @Test
    void debeDeserializarSnapshotCompleto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor =
                MockRestServiceServer.bindTo(builder).build();

        CatalogoRestClientAdapter adaptador =
                new CatalogoRestClientAdapter(
                        builder.build(),
                        new CatalogoSnapshotMapper(),
                        "https://catedra.example.test",
                        "token-de-prueba"
                );

        String json = """
            {
              "snapshotVersion": 7,
              "generatedAt": "2026-09-10T13:44:59Z",
              "professionalCategories": [
                {
                  "id": 1,
                  "name": "Clinica medica",
                  "description": "Atencion general",
                  "enabled": true,
                  "createdAt": "2026-09-10T13:40:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
              ],
              "professionals": [
                {
                  "id": 15,
                  "categoryId": 1,
                  "firstName": "Ana",
                  "lastName": "Gomez",
                  "enabled": true,
                  "createdAt": "2026-09-10T13:40:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
              ],
              "weeklySchedules": [
                {
                  "id": 21,
                  "professionalId": 15,
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00:00",
                  "endTime": "12:00:00",
                  "slotDurationMinutes": 30,
                  "enabled": true,
                  "createdAt": "2026-09-10T13:40:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
              ]
            }
            """;

        servidor.expect(requestTo(
                        "https://catedra.example.test/api/synchronization/snapshot"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var snapshot = adaptador.obtenerSnapshot();

        assertEquals(7L, snapshot.version());
        assertEquals(1, snapshot.categorias().size());
        assertEquals("Clinica medica",
                snapshot.categorias().get(0).getName());

        assertEquals(1, snapshot.profesionales().size());
        assertEquals("Ana",
                snapshot.profesionales().get(0).getFirstName());

        assertEquals(1, snapshot.horarios().size());
        assertEquals(java.time.DayOfWeek.MONDAY,
                snapshot.horarios().get(0).getDayOfWeek());
        assertEquals(java.time.LocalTime.of(9, 0),
                snapshot.horarios().get(0).getStartTime());

        servidor.verify();
    }
}
