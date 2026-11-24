# Seguridad, configuración y operación

## Autenticación entrante

[JwtConfig](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtConfig.java) carga únicamente una pública RSA X.509 de al menos 2048 bits. Valida firma **RS256**, issuer configurado (predeterminado `svc-turnos`), audiencia configurada (`svc-catalogo-api`), sujeto no blanco y expiración obligatoria. `JwtTimestampValidator(Duration.ZERO)` comprueba expiración y `nbf` cuando corresponde, sin tolerancia temporal adicional. Los relojes de los servicios deben estar sincronizados.

Catálogo no emite JWT, no almacena contraseñas ni utiliza la privada RSA de Turnos. La duración y emisión de los tokens pertenecen al emisor. La API no verifica contra una tabla de usuarios de Turnos: confía en la firma y claims validados.

[SecurityConfig](../src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/SecurityConfig.java) exige `SCOPE_catalogo:read` para las tres consultas de profesionales y `SCOPE_catalogo:sync:read` para el diagnóstico. Todas las demás rutas/métodos se deniegan, también fuera de `/api/**`, mediante una segunda cadena. Usa sesión stateless y desactiva formulario, HTTP Basic y caché de requests. CSRF no se desactiva globalmente. No hay política CORS explícita configurada; un cliente web de otro origen requiere revisar ese aspecto, sin asumir acceso habilitado.

El scope de diagnóstico no debe concederse a usuarios comunes para eludir la autorización. Un 401 no distingue ausencia de datos; un 403 tampoco demuestra que un ID exista.

## Identidad técnica y fuentes externas

El JWT de usuario que llega desde Turnos es independiente del JWT técnico configurado en `CATEDRA_API_TOKEN` para obtener snapshots. El segundo se envía como Bearer al REST de cátedra; no se devuelve al cliente ni a KMP. El servicio no registra, autentica ni renueva automáticamente la cuenta técnica: ese procedimiento y los valores de integración proceden de `INTEGRATION_REFERENCE-v2` §5.

Redis utiliza usuario/contraseña ACL asignados; el código solo lee `catedra:sync:*`, no manipula holds ni escribe estado propio en Redis. Kafka usa brokers, consumer group y topic asignados; no reutiliza automáticamente el JWT HTTP como credencial del broker. No se configuran SASL/TLS explícitos en este repositorio. HTTP local/privado no proporciona cifrado de transporte; no existe un proxy HTTPS incorporado.

## Configuración

Fuentes: [application.properties](../src/main/resources/application.properties), [.env.example](../.env.example) y [compose.yaml](../compose.yaml). Compose interpreta `.env`; Java ejecutado directamente no lo carga automáticamente. No imprimir la configuración completa de Compose ni secretos para diagnosticar.

| Variable / propiedad | Uso y valor predeterminado |
| --- | --- |
| `CATALOGO_DB_NAME`, `CATALOGO_DB_USER`, `CATALOGO_DB_PASSWORD` | Identidad y acceso PostgreSQL; completar los tres en Compose. Java tiene `catalogo_db` como nombre predeterminado, no usuario/contraseña predeterminados. |
| `POSTGRES_PORT` | Puerto del host 5432; PostgreSQL usa 5432 dentro de Docker. Turnos publica normalmente su base en 5433, sin necesidad de cambiar Catálogo. |
| `CATALOGO_HTTP_PORT` | Host 8081, loopback. Compose fija `SERVER_PORT=8080` dentro del contenedor. |
| `CATALOGO_JWT_PUBLIC_KEY_FILE` | Ruta absoluta a pública RSA existente del host, sin prefijo `file:`; montaje Compose de solo lectura. |
| `CATALOGO_JWT_PUBLIC_KEY_LOCATION` | Recurso Spring para Java en el host, por ejemplo `file:/ruta/externa/turnos-public.pem`; Compose lo fija a `file:/run/keys/turnos-public.pem`. |
| `CATALOGO_JWT_ISSUER`, `CATALOGO_JWT_AUDIENCE` | `svc-turnos` / `svc-catalogo-api`. Deben coincidir con los tokens legítimos del emisor. |
| `CATEDRA_API_BASE_URL`, `CATEDRA_API_TOKEN` | Sin URL/token por defecto. JWT técnico sin prefijo `Bearer`; requeridos al ejecutar snapshot. |
| `REDIS_HOST`, `REDIS_PORT` | `localhost` / 6379; reemplazar por la dirección externa accesible desde el proceso. |
| `REDIS_USERNAME`, `REDIS_PASSWORD` | Vacíos por defecto; configurar credenciales asignadas. |
| `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_GROUP_ID` | `localhost:9092` / `svc-catalogo-local`; sustituir por integración asignada. |
| `KAFKA_CATALOGO_TOPIC` | `catalogo-no-configurado`; no representa un topic real asignado. |
| `KAFKA_LISTENER_AUTO_STARTUP` | `false`; controla `spring.kafka.listener.auto-startup`, no la reconciliación de arranque. |
| `CATALOGO_RECONCILIACIONINICIO_ENABLED` | Compose: `true`. Corresponde a `catalogo.reconciliacion-inicio.enabled`, activado también si la propiedad falta. Es independiente del listener. |

Redis tiene `spring.data.redis.connect-timeout=5s` y `timeout=5s`. El cliente REST fija 5 s de conexión y 10 s de lectura en código; no se declaran variables de entorno propias para esos timeouts. `catalogo.sincronizacion.transaction-timeout-seconds` tiene valor predeterminado 60 en las anotaciones transaccionales; no dispone de una variable dedicada declarada en Compose. Un ajuste adicional requiere configuración Spring externa, no asumir que `.env` se reenvía íntegramente al contenedor.

## Docker y arranque

Los comandos principales se mantienen en el [README](../README.md). El [Dockerfile](../Dockerfile) usa construcción multietapa con JDK 25/Maven Wrapper y ejecución con JRE 25, bases Temurin fijadas por digest, usuario `catalogo` UID/GID 10001 y `curl` para la sonda. No ejecuta pruebas durante el build ni incorpora la pública mediante COPY: Compose la monta externamente. [.dockerignore](../.dockerignore) limita el contexto de construcción.

El archivo público debe ser legible por UID 10001 y sus directorios atravesables. No montar la privada ni ampliar permisos sobre ella para resolver problemas de Catálogo. Si el archivo no existe, `create_host_path=false` evita crear un directorio por error; si el PEM es inválido o ilegible, Spring no inicia correctamente.

Compose conecta Spring a `jdbc:postgresql://postgres:5432/<base configurada>`. El backend depende del healthcheck `pg_isready`, Flyway aplica V1/V2 y Hibernate valida. Se conserva `catalogo_postgres_data:/var/lib/postgresql`, correspondiente a PostgreSQL 18. Mantener el directorio/nombre de proyecto y las credenciales del volumen existente; cambiar `.env` no modifica automáticamente el usuario/contraseña de una base inicializada.

Solo `svc-catalogo` se conecta a `backends-integracion` además de `default`; PostgreSQL permanece en `default`. La red externa debe existir antes de iniciar. Turnos se conecta a `http://svc-catalogo:8080`; Ubuntu utiliza `http://127.0.0.1:8081` con el puerto predeterminado. `localhost` dentro del contenedor es Catálogo, no Ubuntu, Turnos ni cátedra.

El Compose no inicia Redis ni Kafka locales. ZeroTier en Ubuntu no demuestra que el bridge Docker tenga las mismas rutas, DNS y permisos de forwarding. Para Kafka deben alcanzarse también los brokers anunciados por sus listeners, no solo el bootstrap.

La reconciliación se ejecuta en segundo plano con activación independiente de Kafka. Desactivarla puede ser útil para arrancar una prueba puramente local, pero no produce un catálogo sincronizado. Con una copia previa, el servicio puede consultar datos aun cuando no se confirme su vigencia. Una base vacía seguirá sin datos hasta completar una sincronización.

## Java en el host

Con JDK 25, variables de PostgreSQL y pública `*_LOCATION` proporcionadas por entorno o IDE, el servicio puede ejecutarse en el puerto 8080 predeterminado de Spring Boot:

```bash
java -version
./mvnw spring-boot:run
```

Detener con `Ctrl+C`. Para levantar únicamente la base usando el Compose existente:

```bash
docker compose up -d postgres
```

El archivo completo de Compose requiere la variable de montaje público aunque solo se seleccione PostgreSQL. Preparar la red compartida conforme al README. No ejecutar simultáneamente Java y Docker sobre el mismo puerto HTTP. Las variables de Compose como `CATALOGO_HTTP_PORT` no configuran por sí solas el puerto del Java ejecutado en Ubuntu.

## Diagnóstico y limitaciones

| Síntoma | Comprobación segura |
| --- | --- |
| Compose no valida o no encuentra la red | `docker compose config --quiet`; comprobar variables obligatorias y existencia de `backends-integracion`, sin imprimir sus secretos. |
| Spring falla al cargar JWT | Ruta, formato X.509, tamaño RSA y permisos de lectura. Nunca inspeccionar/publicar la privada de Turnos. |
| PostgreSQL/Flyway falla | Disponibilidad, credenciales del volumen y logs de arranque; no borrar volúmenes ni editar migraciones aplicadas. |
| HTTP responde 401 | La sonda es correcta si no envía JWT. Con token, revisar pública, issuer, audiencia, expiración y reloj. |
| HTTP responde 403 | Scope requerido o ruta/método no permitido; no conceder permisos diagnósticos a usuarios comunes. |
| Búsqueda vacía | Revisar filtros y versión aplicada; no equivale necesariamente a sincronización completa de un catálogo vacío. |
| Diagnóstico ERROR | Puede existir copia válida anterior. Revisar logs seguros, REST técnico y Redis antes de concluir vigencia. |
| Listener detenido | Corregir la causa y reiniciar de forma controlada; no hay reinicio automático ni DLT. |

La sonda Docker realiza el GET de profesionales sin token y exige 401. No es un health público ni certifica frescura, éxito de reconciliación o salud de Redis/Kafka. El diagnóstico protegido proporciona versión y resultado persistidos, no una comprobación en vivo de dependencias.

Los logs de arranque y respuestas de terceros deben revisarse antes de compartirse. El diagnóstico propio guarda textos fijos y los mensajes del arranque/ejecutor diagnóstico muestran clases de excepción, sin copiar causas externas. No se afirma que todo log generado por librerías o toda traza externa esté saneado automáticamente.

Para aplicar cambios de entorno de Compose, utilizar `docker compose up -d`; `restart` no recarga variables. La activación del listener puede consumir notificaciones pendientes y modificar la copia local. Estas operaciones no deben confundirse con una simple validación de configuración.
