<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-77%2F100-green) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-8.5%25-orange) ![%POO](https://img.shields.io/badge/%25POO-80%25-yellow) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-5-red) ![D2](https://img.shields.io/badge/D2-100-green) ![D4](https://img.shields.io/badge/D4-80-green) ![D5](https://img.shields.io/badge/D5-100-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-100-green)

**Calidad (heurístico, 7 dimensiones):** índice **77/100** · build **OK** · aprobar **no (7/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

# svc-catalogo

Servicio de copia local y sincronización del catálogo de profesionales de Programación II. Expone consultas protegidas para Turnos y los clientes autorizados.

## Tecnologías y requisitos

Java 25, Spring Boot 4.1.1, Maven Wrapper, Spring Data JPA, PostgreSQL 18, Flyway, Redis y Spring Kafka. Pruebas con JUnit, Mockito, Testcontainers y JaCoCo.

- Docker Engine y Docker Compose v2 para el entorno y las integraciones de prueba.
- JDK 25 para Maven en el host; Docker utiliza su propio JDK/JRE 25.
- Acceso a dependencias Maven e imágenes en la primera ejecución.
- Pública RSA externa de Turnos y configuración PostgreSQL. La sincronización requiere REST/Redis de cátedra; el consumo de notificaciones requiere Kafka.

## Configuración

Preparar un archivo privado `.env` a partir de [.env.example](.env.example). Conservar las credenciales de una base existente. No versionar ni imprimir secretos.

| Variables | Uso |
| --- | --- |
| `CATALOGO_DB_NAME`, `CATALOGO_DB_USER`, `CATALOGO_DB_PASSWORD` | Base, usuario y contraseña de PostgreSQL. |
| `POSTGRES_PORT`, `CATALOGO_HTTP_PORT` | Puertos del host: 5432 y 8081 por defecto, publicados en loopback. |
| `CATALOGO_JWT_PUBLIC_KEY_FILE` | Ruta absoluta a la pública RSA X.509 de Turnos, mínimo 2048 bits, legible por UID 10001. Solo lectura en Compose; nunca montar la privada. |
| `CATALOGO_JWT_ISSUER`, `CATALOGO_JWT_AUDIENCE` | Predeterminados `svc-turnos` y `svc-catalogo-api`. |
| `CATEDRA_API_BASE_URL`, `CATEDRA_API_TOKEN` | URL REST y JWT técnico de cátedra, sin prefijo `Bearer`. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD` | Conexión Redis asignada por la integración técnica. |
| `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_GROUP_ID`, `KAFKA_CATALOGO_TOPIC` | Brokers, consumer group y topic completo asignados. |
| `KAFKA_LISTENER_AUTO_STARTUP` | `false` por defecto; habilitar para consumir notificaciones. |
| `CATALOGO_RECONCILIACIONINICIO_ENABLED` | `true` por defecto; reconciliación al arrancar. |

Los valores localhost del ejemplo no representan la infraestructura de cátedra. Formatos, ejecución Java en el host y diagnóstico se especifican en [seguridad y operación](docs/seguridad-y-operacion.md).

## Ejecución con Docker Compose

Desde la raíz del repositorio, comprobar la red externa compartida:

```bash
docker network inspect backends-integracion --format '{{.Name}} ({{.Driver}})'
```

Solo si no existe, crearla una vez:

```bash
docker network create --driver bridge backends-integracion
```

Validar e iniciar:

```bash
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 svc-catalogo
```

`svc-catalogo` pertenece a las redes privada y compartida; PostgreSQL solo a la privada. Turnos utiliza `http://svc-catalogo:8080` en esa red. El volumen `catalogo_postgres_data` se conserva al recrear contenedores usando el mismo directorio/nombre de proyecto Compose. Flyway aplica V1/V2 al iniciar.

Sonda HTTP sin JWT, con puerto predeterminado; se espera **401**:

```bash
curl --silent --output /dev/null --write-out '%{http_code}\n' --max-time 5 http://127.0.0.1:8081/api/catalogo/profesionales
```

Esto comprueba HTTP/seguridad, no catálogo actualizado ni conectividad externa. La consulta de datos requiere JWT legítimo de Turnos con `catalogo:read`. La reconciliación inicial es asíncrona; una base vacía no dispone de datos hasta sincronizar correctamente.

Detener conservando los datos:

```bash
docker compose stop
```

Para iniciar los contenedores detenidos: `docker compose start`. Para aplicar cambios de configuración: `docker compose up -d`. No utilizar `down -v` ni limpiar volúmenes.

## Pruebas

Con JDK 25 y Docker disponible, ejecutar la suite con Testcontainers excluyendo la activación accidental de pruebas externas por credenciales heredadas:

```bash
java -version
env -u CATEDRA_API_TOKEN -u REDIS_PASSWORD ./mvnw clean verify
```

Sin esas variables definidas, el comando habitual es `./mvnw clean verify`. Ejecución dirigida de Kafka y PostgreSQL aislados:

```bash
env -u CATEDRA_API_TOKEN -u REDIS_PASSWORD ./mvnw -Dtest=CatalogoKafkaConsumerIntegrationTest test
```

Véanse [pruebas e informes](docs/pruebas.md) para distinguir simuladores de integraciones reales.

## Documentación técnica

- [Arquitectura y límites](docs/arquitectura.md).
- [Contratos REST](docs/contratos.md).
- [Modelo de datos](docs/modelo-datos.md).
- [Sincronización, Kafka y recuperación](docs/sincronizacion-y-kafka.md).
- [Seguridad, configuración y operación](docs/seguridad-y-operacion.md).
- [Pruebas y alcance de verificación](docs/pruebas.md).
