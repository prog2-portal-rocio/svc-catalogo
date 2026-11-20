<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-77%2F100-green) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-8.5%25-orange) ![%POO](https://img.shields.io/badge/%25POO-80%25-yellow) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-5-red) ![D2](https://img.shields.io/badge/D2-100-green) ![D4](https://img.shields.io/badge/D4-80-green) ![D5](https://img.shields.io/badge/D5-100-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-100-green)

**Calidad (heurístico, 7 dimensiones):** índice **77/100** · build **OK** · aprobar **no (7/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

## Servicio de Catálogo

Servicio backend desarrollado para el proyecto integrador de Programación II, correspondiente a un sistema distribuido de gestión de turnos.

### Descripción

El servicio de catálogo tiene como finalidad proporcionar información de profesionales y prestaciones a los demás componentes del sistema.

Su responsabilidad principal es mantener una copia local de la información del catálogo, permitiendo realizar consultas sin depender constantemente del servicio central.

Para mantener los datos actualizados, el servicio contempla mecanismos de sincronización inicial y actualización incremental.

## Tecnologías

- Java 25 (Amazon Corretto).
- Spring Boot 4.1.1.
- Spring Web MVC.
- Spring Data JPA.
- PostgreSQL 18.
- Flyway.
- Docker Compose.
- Testcontainers.
- Maven y JaCoCo.
- Lombok.

## Requisitos

- JDK 25.
- Docker Engine y Docker Compose.

El proyecto incluye Maven Wrapper, por lo que no es
necesario instalar Maven por separado.

## Ejecución con Docker

La imagen utiliza Java 25 aunque la terminal tenga seleccionado Java 21. La
construcción usa Maven Wrapper; la imagen final solo contiene JRE 25 y el JAR,
con usuario sin privilegios (UID 10001). Las bases oficiales están fijadas por
digest en el Dockerfile. La suite con Testcontainers se ejecuta fuera del build.

1. Preparar `.env` desde `.env.example` y completar la configuración privada.
   Conservar las credenciales de una base existente: cambiarlas en `.env` no
   cambia las del volumen PostgreSQL ya inicializado.
2. Definir `CATALOGO_JWT_PUBLIC_KEY_FILE` como ruta absoluta de un archivo PEM
   **público** RSA X.509, mínimo 2048 bits, correspondiente al emisor Turnos.
   Debe ser legible por UID 10001. Se monta solo ese archivo, en modo lectura;
   nunca montar la clave privada ni guardarla en la imagen/repositorio.
   `CATALOGO_JWT_PUBLIC_KEY_LOCATION` sigue siendo la opción para Java en el host;
   dentro del contenedor Compose la fija a `file:/run/keys/turnos-public.pem`.
3. Configurar REST/Redis/Kafka con los valores asignados por cátedra. Los campos
   vacíos y localhost del ejemplo no representan infraestructura real.

```bash
# Desde la raíz; config --quiet valida sin imprimir secretos.
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 svc-catalogo
curl -i http://localhost:8081/api/catalogo/profesionales
```

El GET sin JWT debe devolver **401**. Para comprobar datos, usar Postman con un
JWT legítimo de Turnos: RS256, iss=svc-turnos, aud que incluya svc-catalogo-api y
scope catalogo:read. El diagnóstico requiere catalogo:sync:read; no conceder ese
permiso a un usuario común solo para la demostración. No imprimir ni exportar
tokens. No existe un endpoint health público.

| Configuración externa | Uso |
| --- | --- |
| CATALOGO_DB_NAME, CATALOGO_DB_USER, CATALOGO_DB_PASSWORD | Misma configuración PostgreSQL existente; volumen persistente conservado |
| POSTGRES_PORT | Host 5432 por defecto, no cambia a 5433 (usado por Turnos) |
| CATALOGO_HTTP_PORT | Puerto HTTP del host, 8081 por defecto; contenedor usa 8080 |
| CATALOGO_JWT_PUBLIC_KEY_FILE | Ruta del archivo público en el host, exclusiva del montaje Compose |
| CATALOGO_JWT_ISSUER, CATALOGO_JWT_AUDIENCE | svc-turnos y svc-catalogo-api por defecto |
| CATEDRA_API_BASE_URL, CATEDRA_API_TOKEN | API y JWT **técnico**, independiente del JWT de usuario; token sin prefijo Bearer |
| REDIS_HOST, REDIS_PORT, REDIS_USERNAME, REDIS_PASSWORD | Redis de cátedra, accesible desde el contenedor |
| KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID, KAFKA_CATALOGO_TOPIC | Valores de la integración asignada |
| KAFKA_LISTENER_AUTO_STARTUP | false por defecto; true para consumir eventos reales |
| CATALOGO_RECONCILIACIONINICIO_ENABLED | Propiedad existente catalogo.reconciliacion-inicio.enabled, true por defecto |

Spring se conecta a `jdbc:postgresql://postgres:5432/...` por la red Compose,
no por el puerto publicado en el host. Espera a que PostgreSQL esté healthy;
Flyway aplica V1/V2 y Hibernate valida el esquema. No se crean Redis ni Kafka
locales. El healthcheck de la aplicación verifica **solo HTTP y el rechazo 401**,
no frescura del catálogo, salud de Kafka o éxito de reconciliación.

La reconciliación corre en segundo plano: sin URL/token REST o Redis accesible,
falla de forma observable en logs y conserva la copia previa; el servidor HTTP
puede seguir funcionando. En base vacía no hay datos reales para mostrar hasta
completar la sincronización. Solo para una prueba local sin integración puede
ponerse CATALOGO_RECONCILIACIONINICIO_ENABLED=false; no presentarla como catálogo
sincronizado. Esta rama no incorpora la corrección diagnóstica del issue #38.

Para reiniciar y detener **sin borrar la base**:

```bash
docker compose restart
docker compose stop
# Para iniciar de nuevo los contenedores detenidos:
docker compose up -d
```

No ejecutar down -v ni comandos prune. Las políticas unless-stopped permiten
reiniciar contenedores conservando el volumen. Usar el mismo directorio/nombre
de proyecto Compose para reutilizarlo. Si cambia un puerto ocupado, ajustar
POSTGRES_PORT o CATALOGO_HTTP_PORT, sin tocar los de otros proyectos.

**Conectividad y problemas frecuentes:** localhost dentro del contenedor es el
propio contenedor. Para cátedra usar sus direcciones reales por ZeroTier; el host
conectado no garantiza que el bridge Docker tenga rutas, DNS y permisos de
forwarding equivalentes. Comprobar también los advertised listeners de Kafka.
No modificar el firewall o usar network_mode:host a ciegas. Si falta el PEM,
Compose falla; si es inválido o ilegible, Spring no arranca. Si se ejecuta Java en
el host, detener primero svc-catalogo para no competir por HTTP/BD y configurar
sus variables del host por separado. Antes de compartir logs, revisar que no
contengan información privada; no publicar la salida completa de compose config.

## Ejecución con Java en el host

Para ejecutar el servicio por primera vez, seguir las
instrucciones de la sección **Persistencia local**.

Una vez configurada la base de datos y cargadas las variables
de entorno, iniciar la aplicación con:

```bash
./mvnw spring-boot:run
```

La aplicación se inicia en el puerto `8080`.

Para detenerla, utilizar `Ctrl + C`.

## Compilación y pruebas

Para ejecutar las pruebas:

```bash
./mvnw test
```

Para compilar y generar el archivo JAR:

```bash
./mvnw package
```

Los archivos generados se guardan en `target/`.

En Windows, utilizar `mvnw.cmd` en lugar de `./mvnw`.


## Persistencia local

El servicio utiliza PostgreSQL 18 para mantener una copia local
del catálogo y registrar el estado de sincronización.

La persistencia se implementa mediante Spring Data JPA y las
migraciones de la base de datos se gestionan con Flyway.

### Requisitos adicionales

- Docker Engine y Docker Compose.
- JDK 25.

### Configuración de la base de datos

1. Crear el archivo `.env` a partir del ejemplo:

   ```bash
   cp .env.example .env
   ```

2. Editar `.env` y configurar una contraseña en
   `CATALOGO_DB_PASSWORD`.

   El archivo `.env` está excluido de Git y no debe subirse
   al repositorio.

3. Configurar también CATALOGO_JWT_PUBLIC_KEY_FILE según la sección Docker
   (el Compose completo valida ese montaje), e iniciar solamente PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

4. Comprobar el estado del contenedor:

   ```bash
   docker compose ps
   ```

   El servicio `postgres` debe aparecer como `healthy`.

### Ejecución de la aplicación

Cargar las variables de entorno en la terminal:

```bash
set -a
source .env
set +a
```

Iniciar Spring Boot:

```bash
./mvnw spring-boot:run
```

Flyway ejecutará automáticamente las migraciones pendientes
al iniciar la aplicación.

### Pruebas

Para ejecutar todas las pruebas y generar el informe de cobertura:

```bash
./mvnw clean verify
```

Las pruebas de integración utilizan Testcontainers para iniciar
una instancia temporal de PostgreSQL. Por lo tanto, requieren
Docker disponible, pero no necesitan utilizar la base de datos
local del archivo `compose.yaml`.

### Sincronización del catálogo

La persistencia contempla:

- Categorías.
- Profesionales.
- Horarios semanales.
- Estado de sincronización y versión aplicada.

El reemplazo de un snapshot se realiza dentro de una transacción,
respetando las relaciones entre las entidades.

Si una operación falla, la transacción revierte sus cambios.
Las versiones iguales o anteriores a la última aplicada se ignoran.

## Arquitectura

El proyecto seguirá una arquitectura hexagonal, con separación entre dominio, aplicación e infraestructura, según la estructura de referencia utilizada en la cátedra.

Cada funcionalidad se organizará de forma independiente, procurando mantener la lógica de negocio desacoplada de las tecnologías externas.
