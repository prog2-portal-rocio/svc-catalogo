# Contratos REST

## API propia

Fuentes: [ProfesionalController](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalController.java), [EstadoSincronizacionController](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/EstadoSincronizacionController.java), [DTO](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/dto) y [SecurityConfig](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/SecurityConfig.java).

Todas las operaciones implementadas son GET y requieren JWT válido. No hay endpoints propios de registro/login, escritura de catálogo, ejecución manual de sincronización, listado independiente de categorías ni salud pública. Las rutas no autorizadas explícitamente se deniegan.

| Método y ruta | Propósito | Permiso | Respuesta correcta |
| --- | --- | --- | --- |
| `GET /api/catalogo/profesionales` | Buscar sobre PostgreSQL local. | `catalogo:read` | 200, array de `ProfesionalResponse`; `[]` sin coincidencias. |
| `GET /api/catalogo/profesionales/{id}` | Consultar profesional y categoría. | `catalogo:read` | 200, `ProfesionalDetalleResponse`. |
| `GET /api/catalogo/profesionales/{id}/horarios` | Consultar reglas semanales del profesional. | `catalogo:read` | 200, array de `HorarioSemanalResponse`; `[]` si existe pero no tiene horarios. |
| `GET /api/catalogo/sincronizacion/estado` | Consultar versión y diagnóstico persistidos. | `catalogo:sync:read` | 200, `EstadoSincronizacionResponse`. |

### Filtros de profesionales

Los parámetros son opcionales y combinables. No se implementan paginación, total de resultados ni orden contractual explícito en la búsqueda.

| Parámetro | Regla |
| --- | --- |
| `categoriaId` | Entero positivo; coincidencia exacta. Una categoría inexistente produce una búsqueda vacía, no 404. |
| `nombre` | Subcadena de nombre **o** apellido, sin distinguir mayúsculas. Se eliminan extremos mediante `strip()`; vacío queda sin filtro. No es búsqueda por nombre completo concatenado. `%` y `_` no se interpretan como comodines de LIKE. |
| `habilitado` | Booleano `true`/`false`, sin distinguir mayúsculas, conforme al editor configurado; no admite `1`, `yes` u otros alias. Ausente/vacío deja el filtro sin valor. |
| `fecha` | Fecha ISO `yyyy-MM-dd`; activa el filtro de agenda semanal local. |
| `hora` | Hora ISO local; requiere `fecha`. Debe coincidir con el inicio de un bloque válido. |

Sin `fecha`, no se excluyen automáticamente profesionales ni categorías deshabilitados: se conserva la semántica del filtro `habilitado`. Con `fecha`, profesional, categoría y horario deben estar habilitados. Por ello `habilitado=false` combinado con disponibilidad no devuelve profesionales.

La agenda se evalúa para el día de semana de la fecha. Sin hora debe caber al menos un turno completo en una jornada habilitada; con hora, esta debe estar alineada a la duración desde el inicio de esa jornada y el bloque debe terminar como máximo al cierre. Se admiten varias jornadas del mismo día; no intervalos que crucen medianoche. No se consultan ocupaciones ni reservas: esta disponibilidad solo acredita existencia de agenda.

La consulta usa `ProfesionalJpaRepository.buscar` y una consulta agrupada de horarios para los candidatos; `HorarioSemanal.tieneBloqueDisponible` evalúa duración y alineación. El controlador no replica esas reglas. Las consultas por ID y horarios conservan elementos deshabilitados; Turnos necesita conocer esos estados para sus validaciones.

### DTO de respuesta

| DTO | Campos |
| --- | --- |
| `ProfesionalResponse` | `id`, `categoryId`, `firstName`, `lastName`, `enabled`, `createdAt`, `updatedAt`. |
| `ProfesionalDetalleResponse` | Los campos anteriores y `category`: `id`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`. |
| `HorarioSemanalResponse` | `id`, `professionalId`, `dayOfWeek`, `startTime`, `endTime`, `slotDurationMinutes`, `enabled`, `createdAt`, `updatedAt`. |
| `EstadoSincronizacionResponse` | `appliedVersion`, `appliedAt`, `lastAttemptAt`, `phase`, `safeDiagnostic`. |

Los IDs son numéricos; `dayOfWeek` es `MONDAY`–`SUNDAY`; horas locales y fechas utilizan tipos `java.time`; los instantes son ISO-8601 UTC. No se fija un patrón adicional de serialización para horas en estos DTO. `description` y timestamps de las entidades pueden ser nulos según el esquema. El diagnóstico inicial no tiene versión ni fechas aplicadas.

Ejemplo mínimo, con IDs ficticios:

```http
GET /api/catalogo/profesionales?categoriaId=1&nombre=ana&fecha=2026-11-26&hora=09:00
Authorization: Bearer <JWT de usuario autorizado>
```

Una búsqueda sin coincidencias responde `[]`; el detalle solo existe cuando el ID está almacenado localmente. Ningún DTO expone directamente una entidad JPA.

### Errores y vigencia

- **400:** IDs no positivos, errores de conversión/formato, `hora` sin `fecha` o criterios inválidos.
- **404:** profesional inexistente en detalle o consulta de horarios.
- **401:** JWT ausente, inválido, vencido o incompatible con firma/claims requeridos.
- **403:** JWT válido sin el scope requerido; otras rutas/métodos también se deniegan según las cadenas de seguridad.

[ProfesionalExceptionHandler](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/handler/ProfesionalExceptionHandler.java) utiliza `ProblemDetail` para ausencia de profesional y argumentos inválidos; hereda el manejo MVC de conversiones. No añade un `code` funcional propio. Los rechazos de seguridad no tienen garantizado ese mismo cuerpo. No existe una traducción específica de fallos de persistencia a 502/503/504 en estos controladores; no se documentan esos códigos como contrato implementado.

Las búsquedas leen la copia local aunque falle la sincronización. Un 200 acredita la consulta, no la frescura del catálogo. Una base sin sincronizar puede devolver `[]`; la versión y el diagnóstico deben verificarse por separado con el permiso correspondiente.

## Contrato utilizado por Turnos

Turnos consume detalle y horarios para validar nuevas operaciones y generar candidatos de disponibilidad. Envía el JWT de usuario validado con `iss=svc-turnos`, audiencia que incluya `svc-catalogo-api` y `catalogo:read`. Catálogo no recibe el JWT técnico de cátedra para autorizar estas consultas, ni consulta tablas de Turnos.

Un profesional deshabilitado o con categoría deshabilitada sigue siendo consultable por ID. Los horarios devueltos no se filtran por vigencia: la lista incluye `enabled` y sus reglas. Turnos interpreta esos datos y combina la agenda con ocupaciones externas; no debe confundir este contrato con una garantía de reserva.

## Snapshot externo de cátedra

Contrato diferente de las APIs anteriores, definido por `INTEGRATION_REFERENCE-v2` §7:

`GET /api/synchronization/snapshot`, autenticado con el JWT **técnico** del proyecto. Devuelve `snapshotVersion`, `generatedAt`, `professionalCategories`, `professionals`, `weeklySchedules`, incluyendo entidades habilitadas y deshabilitadas. Las colecciones contienen los campos del catálogo descritos arriba; la categoría llega separada, no anidada en cada profesional.

[CatalogoRestClientAdapter](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapter.java) obtiene el snapshot y `CatalogoSnapshotMapper` convierte los DTO externos a dominio. La versión y las tres colecciones son obligatorias; `generatedAt` no se usa como versión ni como timestamp de aplicación. Los errores REST se propagan al flujo de sincronización, sin transformarse en un snapshot vacío. Catálogo no implementa automáticamente el registro/login técnico de cátedra.
