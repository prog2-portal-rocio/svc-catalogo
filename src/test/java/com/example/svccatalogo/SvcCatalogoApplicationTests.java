package com.example.svccatalogo;

import com.example.svccatalogo.catalogo.infrastructure.security.JwtTestContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ContextConfiguration(initializers = JwtTestContextInitializer.class)
@SpringBootTest(properties = "catalogo.reconciliacion-inicio.enabled=false")
@Testcontainers
class SvcCatalogoApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Test
    void contextLoads() {
    }
}
