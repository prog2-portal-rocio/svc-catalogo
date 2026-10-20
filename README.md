<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-57%2F100-yellow) ![Aprobar](https://img.shields.io/badge/Aprobar-s%C3%AD-brightgreen) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-0.0%25-green) ![%POO](https://img.shields.io/badge/%25POO-90%25-green) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-29-red) ![D2](https://img.shields.io/badge/D2-45-orange) ![D4](https://img.shields.io/badge/D4-0-red) ![D5](https://img.shields.io/badge/D5-95-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-62-yellow)

**Calidad (heurístico, 7 dimensiones):** índice **57/100** · build **OK** · aprobar **sí (8/11)** · destacar **no**
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

## Ejecución

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

3. Iniciar PostgreSQL:

   ```bash
   docker compose up -d
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
