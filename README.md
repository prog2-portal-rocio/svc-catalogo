<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-54%2F100-yellow) ![Aprobar](https://img.shields.io/badge/Aprobar-s%C3%AD-brightgreen) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-23.3%25-orange) ![%POO](https://img.shields.io/badge/%25POO-90%25-green) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-14-red) ![D2](https://img.shields.io/badge/D2-45-orange) ![D4](https://img.shields.io/badge/D4-0-red) ![D5](https://img.shields.io/badge/D5-95-green) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-62-yellow)

**Calidad (heurístico, 7 dimensiones):** índice **54/100** · build **OK** · aprobar **sí (8/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

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
