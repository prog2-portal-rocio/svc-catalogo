# Pruebas y alcance de verificación

## Ejecución reproducible

Desde la raíz del repositorio, con JDK 25 y Docker accesible para Testcontainers:

```bash
java -version
env -u CATEDRA_API_TOKEN -u REDIS_PASSWORD ./mvnw clean verify
```

Equivale a `./mvnw clean verify` sin esas dos variables heredadas. Se eliminan solo del proceso de pruebas para impedir la activación accidental de pruebas reales. No se modifica `.env`, ninguna credencial persistida ni la configuración de ejecución.

Ejecutar un escenario integrado con PostgreSQL y Kafka aislados:

```bash
env -u CATEDRA_API_TOKEN -u REDIS_PASSWORD ./mvnw -Dtest=CatalogoKafkaConsumerIntegrationTest test
```

Selección unitaria sin Testcontainers ni servicios externos:

```bash
env -u CATEDRA_API_TOKEN -u REDIS_PASSWORD ./mvnw -Dtest=SincronizarCatalogoIncrementalUseCaseTest,ReconciliarCatalogoUseCaseTest,DiagnosticoSincronizacionExecutorTest,CatalogoKafkaConsumerAdapterTest test
```

La selección unitaria no verifica FK, transacciones ni comportamiento del broker. Maven Wrapper puede descargar Maven/dependencias; Testcontainers necesita imágenes disponibles. `-o` evita descargas Maven, no sustituye Docker ni garantiza imágenes locales.

## Matriz de evidencia existente

Fuente: [src/test/java/com/example/svccatalogo](../src/test/java/com/example/svccatalogo). Los nombres terminados en `Test` no implican necesariamente una prueba unitaria: algunos utilizan Spring Boot y contenedores.

| Área | Pruebas representativas | Escenarios y componentes |
| --- | --- | --- |
| Dominio y disponibilidad local | `HorarioSemanalTest`, `CriteriosBusquedaProfesionalTest`, `BuscarProfesionalesUseCaseTest` | Día de semana, bloques completos/alineados, límites, criterios y combinación de filtros. |
| Persistencia y búsqueda | `ProfesionalPersistenceAdapterTest`, `HorarioSemanalPersistenceAdapterTest`, `CategoriaPersistenceAdapterTest` | PostgreSQL 18 aislado, datos locales, habilitación, categorías, múltiples horarios y consultas sin resultados. |
| Snapshot | `CatalogoRestClientAdapterTest`, `CatalogoSnapshotMapperTest`, `SincronizarCatalogoIntegrationTest`, `CatalogoSnapshotPersistenceAdapterTest` | HTTP mediante `MockRestServiceServer`, mapeo, validaciones, reemplazo PostgreSQL y rollback; el puerto remoto se simula en la integración de aplicación. |
| Incremental | `CatalogoRedisAdapterTest`, `SincronizarCatalogoIncrementalUseCaseTest`, `CatalogoIncrementalPersistenceAdapterTest` | Redis/template y puertos simulados; lectura/validación de cambios y entidades; continuidad, versiones, tres intentos y fallback; persistencia real en PostgreSQL aislado. |
| Kafka | `CatalogoKafkaConsumerAdapterTest`, `CatalogoKafkaConsumerIntegrationTest` | JSON/clave/schema, acuse, duplicados, versiones antiguas y reentrega después de fallo. La integración utiliza Kafka 4.1.0 y PostgreSQL 18 mediante Testcontainers; Redis y REST se sustituyen por puertos simulados. |
| Reconciliación y concurrencia | `ReconciliarCatalogoUseCaseTest`, `ProcesarNotificacionCatalogoIntegrationTest`, `ReconciliacionCatalogoInicioAdapterTest`, `ReconciliacionCatalogoInicioIntegrationTest` | Base vacía/atrasada/actualizada, huecos, fallos Redis/REST/PostgreSQL, bloqueo compartido y arranque efectivo por `ApplicationReadyEvent` sin evento Kafka; fuentes externas simuladas. |
| Diagnóstico | `DiagnosticoSincronizacionExecutorTest`, `ProcesarNotificacionCatalogoIntegrationTest`, `SincronizarCatalogoIntegrationTest` | Fallo tras éxito, conservación de datos, registro después de rollback, concurrencia, texto seguro y fallo exclusivo al guardar diagnóstico sin cambiar el resultado funcional. |
| REST y JWT | `ProfesionalControllerTest`, `EstadoSincronizacionControllerTest`, `JwtSecurityHttpTest`, `JwtConfigTest` | Rutas/DTO/400/404, autenticación 401, permisos 403, RS256, issuer, audiencia, vencimiento y protección de rutas no autorizadas. |

Flyway ejecuta V1/V2 sobre PostgreSQL de prueba; Hibernate valida el esquema. No se documenta una prueba específica de actualización V1→V2 con datos históricos si no está implementada como tal. La aplicación del esquema y las restricciones se ejercitan mediante los tests de persistencia y contexto.

Las claves de las pruebas JWT se generan como material sintético mediante `JwtTestContextInitializer`; no requieren las claves operativas de Turnos. Los datos de prueba son ficticios. El Compose operativo y sus volúmenes no se utilizan para preparar la base de estas pruebas.

## Pruebas reales opcionales

Existen pruebas adicionales cuya activación depende de variables, no de un perfil Maven explícito:

| Clase | Activación | Alcance |
| --- | --- | --- |
| `CatalogoRestClientRealTest` | `CATEDRA_API_TOKEN` no vacío. | GET real del snapshot; requiere también URL válida. |
| `SincronizarCatalogoRealIntegrationTest` | `CATEDRA_API_TOKEN` no vacío. | Snapshot REST real aplicado en PostgreSQL **aislado** mediante Testcontainers. |
| `CatalogoRedisRealIntegrationTest` | `REDIS_PASSWORD` no vacío. | Lecturas reales usando host, puerto, usuario y contraseña del entorno. |

No se activan como parte del procedimiento aislado anterior. No existe una prueba Redis local con Testcontainers equivalente a toda la integración real; los tests ordinarios del adaptador utilizan mocks. Ninguna de estas pruebas opcionales acredita automáticamente que el contenedor de producción tenga la misma conectividad.

## Informes y CI

[pom.xml](../pom.xml) configura Java 25 y JaCoCo 0.8.14: agente en tests e informe durante `verify`. Resultados Surefire en `target/surefire-reports`; cobertura en `target/site/jacoco/index.html` y `target/site/jacoco/jacoco.xml`.

[CI](../.github/workflows/ci.yml) se activa en PR hacia `main`/`develop`, push a `main` o ejecución manual. Utiliza JDK 25 Corretto y `./mvnw --batch-mode clean verify`; los contenedores necesarios los administran los tests. El análisis SonarQube usa un secreto de GitHub y se ejecuta salvo en PR cuyo destino no sea `main`.

El bloque automático del README y [EVALUACION.md](EVALUACION.md) se conservan sin edición: son resultados de una evaluación concreta, no una certificación de la ejecución actual. La documentación no fija cantidades de tests ni un Quality Gate permanente. Estos resultados deben consultarse para el commit evaluado.

## Límites de verificación

Las pruebas aisladas respaldan contratos y comportamiento local, no accesibilidad real por ZeroTier, credenciales vigentes, políticas de red Docker, brokers anunciados o correspondencia de la pública instalada con los JWT de Turnos. Esos aspectos requieren verificaciones operativas autorizadas en el entorno desplegado.

El arranque HTTP y su sonda 401 no verifican snapshot, incremental ni vigencia. La demostración funcional requiere datos reales sincronizados, consultas con JWT legítimo y evidencia de versión/diagnóstico con permiso específico. La ausencia de errores de compilación o un Quality Gate aprobado no sustituyen esas comprobaciones. La integración KMP tampoco se considera verificada por este backend.
