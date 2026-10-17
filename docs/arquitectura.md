# Arquitectura del servicio de catálogo

## 1. Objetivo

El servicio `svc-catalogo` forma parte de un sistema distribuido de gestión de turnos.

Su responsabilidad es mantener una copia local de categorías, profesionales y horarios semanales, permitir consultas sobre esa información y gestionar su sincronización con el servicio central.

## 2. Enfoque arquitectónico

Se utilizará **arquitectura hexagonal**, con una organización del código por funcionalidades (*slices*).

Este enfoque permite separar la lógica de negocio de los detalles técnicos, como la persistencia, las APIs REST y los sistemas de mensajería.

La comunicación entre los componentes se realizará mediante puertos y adaptadores, favoreciendo la separación de responsabilidades y la independencia de las tecnologías utilizadas.

## 3. Organización inicial

Se define inicialmente un slice denominado `catalogo`, que agrupará las responsabilidades relacionadas con la información del catálogo local.

La estructura interna seguirá esta organización:

- **domain:** contiene los modelos de negocio y las interfaces de los puertos de entrada y salida.
- **application:** implementa los casos de uso y coordina las operaciones del sistema.
- **infrastructure:** contiene los adaptadores que permiten interactuar con tecnologías y sistemas externos.

Los paquetes específicos se incorporarán progresivamente, a medida que se implementen las funcionalidades que los necesiten.

## 4. Reglas de dependencia

La organización respetará las siguientes reglas:

- El dominio no dependerá de infraestructura ni de frameworks específicos.
- Los casos de uso implementarán los puertos de entrada y utilizarán los puertos de salida cuando necesiten acceder a recursos externos.
- Los adaptadores de infraestructura se comunicarán con la aplicación mediante los puertos correspondientes.
- Los modelos de dominio se mantendrán separados de las entidades de persistencia y de los DTO utilizados en las APIs REST.

## 5. Integraciones

El servicio incorporará progresivamente los componentes necesarios para:

- Obtener información del catálogo mediante REST.
- Mantener y consultar una copia local de los datos.
- Procesar actualizaciones del catálogo mediante Redis y Kafka.
- Exponer operaciones de consulta para otros componentes del sistema.

Los adaptadores se organizarán en paquetes con nombres descriptivos según su responsabilidad.

## 6. Evolución de la arquitectura

La estructura inicial se ampliará a medida que se incorporen nuevas funcionalidades.

Se evaluará la separación de responsabilidades en nuevos slices cuando exista una necesidad concreta, procurando mantener una organización clara y evitar dependencias innecesarias entre componentes.