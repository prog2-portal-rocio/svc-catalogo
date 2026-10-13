## Servicio de Catálogo

Servicio backend desarrollado para el proyecto integrador de Programación II, correspondiente a un sistema distribuido de gestión de turnos.

### Descripción

El servicio de catálogo tiene como finalidad proporcionar información de profesionales y prestaciones a los demás componentes del sistema.

Su responsabilidad principal es mantener una copia local de la información del catálogo, permitiendo realizar consultas sin depender constantemente del servicio central.

Para mantener los datos actualizados, el servicio contempla mecanismos de sincronización inicial y actualización incremental.
## Tecnologías

- Java 25
- Spring Boot 4.1.1
- Maven
- Spring Web MVC
- Lombok

## Requisitos

- JDK 25.
El proyecto incluye Maven Wrapper, por lo que no es necesario instalar Maven por separado.

## Ejecución

Desde la raíz del proyecto:

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

## Arquitectura

El proyecto seguirá una arquitectura hexagonal, con separación entre dominio, aplicación e infraestructura, según la estructura de referencia utilizada en la cátedra.

Cada funcionalidad se organizará de forma independiente, procurando mantener la lógica de negocio desacoplada de las tecnologías externas.