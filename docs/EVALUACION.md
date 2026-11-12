<!-- evaluacion:inicio -->
# Evaluación — prog2-portal-rocio/svc-catalogo

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-12.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **76/100** |
| Tipo de repositorio | `backend` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 7,582 |
| Archivos de código | 120 |
| Clases | 126 |
| Código dentro de clases (%POO) | 84% |
| Funciones fuera de clases | 0 |
| Duplicación (puerta) | 8.5% |
| Duplicación lógica | 8.9% |
| Duplicación de boilerplate | 7.4% |
| Cobertura | 98% |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 6/11 |
| Destacar (`exigente`) | **no cumple** — 6/11 |

**Para aprobar, falta cumplir:** SOLID; Duplicación

**Para destacar, falta cumplir:** SOLID; Duplicación

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Pilar Rocío Portal Romano | 1 | 0 | 0 | 0 | 14 | 0% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 2 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | 100 |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | N/A |
| **D4** Integración y contrato (10%) | 10% | 80 |
| **D5** Seguridad y robustez (10%) | 10% | 100 |
| **D6** Pruebas y cobertura (20%) | 20% | 100 |
| **D7** Proceso y entrega (15%) | 15% | 100 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **2/100**
- **Para investigar la causa:**
    - ¿Se pensó el modelo de clases antes de codificar o se agrupó después?
    - ¿Qué clases concentran muchas responsabilidades a la vez?
    - ¿Los colaboradores llegan como parámetros o se crean por dentro?

### D2. Arquitectura hexagonal — backend (15%)

- **Qué mide:** Separación en capas (dominio, puertos de entrada/salida, adaptadores) y dirección de las dependencias del backend.
- **Por qué importa:** Aislar el dominio de la infraestructura permite cambiar la base de datos, el framework o el transporte sin tocar las reglas de negocio.
- **Tu resultado:** **100/100**

### D4. Integración y contrato (10%)

- **Qué mide:** Cumplimiento del contrato de integración de la cátedra (formato de errores, paginación, autenticación, endpoints esperados).
- **Por qué importa:** Los servicios deben poder integrarse entre sí y con la app sin sorpresas; el contrato es lo que hacen cumplir las pruebas de integración.
- **Tu resultado:** **80/100**

### D5. Seguridad y robustez (10%)

- **Qué mide:** Credenciales embebidas, consultas SQL por concatenación, y presencia de config de entorno; aislamiento de secretos.
- **Por qué importa:** Un secreto en el repositorio o una consulta construida por concatenación son riesgos concretos, no hipotéticos.
- **Tu resultado:** **100/100**

### D6. Pruebas y cobertura (20%)

- **Qué mide:** Cantidad y resultado de pruebas y cobertura de línea (JaCoCo/Kover, Surefire/Failsafe) cuando hay build.
- **Por qué importa:** Las pruebas son la red de seguridad para refactorizar e integrar; la cobertura acota las zonas sin verificar.
- **Tu resultado:** **100/100**

### D7. Proceso y entrega (15%)

- **Qué mide:** Artefactos verificables de proceso/entrega: `compose`, `.env.example`, CI (`.github/workflows/`), `CODEOWNERS` y wrapper de build (`mvnw`/`gradlew`). **No** puntúa documentación.
- **Por qué importa:** Un proyecto que compila, se levanta y se valida solo es más fácil de revisar y de integrar en equipo.
- **Tu resultado:** **100/100**

## 5. Señales detectadas

El análisis encontró las siguientes señales (archivo, sin número de línea a propósito):

**SRP/responsabilidad** (59)
- clase 'CatalogoRedisAdapter' grande (LOC=218, métodos=20, campos=9) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- clase 'CatalogoRedisAdapter' con baja cohesión (7%) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- método 'CatalogoRedisAdapter.obtenerCambios' de 47 líneas — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- método 'CatalogoRedisAdapter.obtenerCambios' con complejidad 7 — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- método 'CatalogoKafkaConsumerAdapter.convertirNotificacion' de 62 líneas — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapter.java`
- método 'CatalogoKafkaConsumerAdapter.convertirNotificacion' con complejidad 9 — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapter.java`
- clase 'HorarioSemanalEntity' grande (LOC=21, métodos=0, campos=9) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/HorarioSemanalEntity.java`
- clase 'HorarioSemanalEntity' sin métodos (9 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/HorarioSemanalEntity.java`
- clase 'ProfesionalEntity' sin métodos (7 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/ProfesionalEntity.java`
- clase 'CategoriaEntity' sin métodos (6 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/CategoriaEntity.java`
- clase 'EventoCatalogoProcesadoEntity' sin métodos (3 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/EventoCatalogoProcesadoEntity.java`
- clase 'EstadoSincronizacionEntity' sin métodos (6 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/entity/EstadoSincronizacionEntity.java`
- clase 'ProfesionalController' con baja cohesión (5%) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalController.java`
- clase 'ProcesarNotificacionCatalogoService' con baja cohesión (10%) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoService.java`
- método 'SincronizarCatalogoIncrementalUseCase.sincronizarCatalogoIncremental' de 70 líneas — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCase.java`
- método 'SincronizarCatalogoIncrementalUseCase.sincronizarCatalogoIncremental' con complejidad 9 — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCase.java`
- clase 'HorarioSemanal' grande (LOC=66, métodos=11, campos=9) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanal.java`
- clase 'HorarioSemanal' con baja cohesión (15%) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanal.java`
- clase 'Categoria' con baja cohesión (0%) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/Categoria.java`
- clase 'EstadoSincronizacion' sin métodos (5 campos): solo datos — `src/main/java/com/example/svccatalogo/catalogo/domain/model/EstadoSincronizacion.java`
- clase 'Profesional' con baja cohesión (0%) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/Profesional.java`
- método 'CatalogoRedisRealIntegrationTest.debeConsultarCatalogoDesdeRedisReal' de 33 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisRealIntegrationTest.java`
- clase 'CatalogoRedisAdapterTest' grande (LOC=251, métodos=17, campos=4) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapterTest.java`
- método 'CatalogoRedisAdapterTest.obtenerCambios' de 43 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapterTest.java`
- método 'CatalogoRedisAdapterTest.jsonInvalidos' de 40 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapterTest.java`
- clase 'JwtSecurityHttpTest' grande (LOC=281, métodos=37, campos=7) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtSecurityHttpTest.java`
- clase 'JwtSecurityHttpTest' con baja cohesión (39%) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtSecurityHttpTest.java`
- clase 'JwtTestContextInitializer' con baja cohesión (13%) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtTestContextInitializer.java`
- método 'CatalogoKafkaConsumerAdapterTest.mensajesInvalidos' de 54 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- clase 'CatalogoKafkaConsumerIntegrationTest' grande (LOC=155, métodos=16, campos=9) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerIntegrationTest.java`
- clase 'CatalogoKafkaConsumerIntegrationTest' con baja cohesión (30%) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerIntegrationTest.java`
- método 'CatalogoKafkaConsumerIntegrationTest.configurarIncrementos' con complejidad 6 — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerIntegrationTest.java`
- clase 'CatalogoSnapshotPersistenceAdapterTest' grande (LOC=293, métodos=9, campos=7) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.aplicaSnapshotCompletoYActualizaVersion' de 47 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.revierteCambiosSiFallaLaSincronizacion' de 43 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.reemplazaLosDatosDeUnSnapshotAnterior' de 44 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.ignoraSnapshotsConVersionIgualOAnterior' de 38 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.aplicaSnapshotVacioYEliminaDatosAnteriores' de 37 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- método 'CatalogoSnapshotPersistenceAdapterTest.conservaLaVersionMasRecienteConSnapshotsConcurrentes' de 45 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- clase 'CatalogoIncrementalPersistenceAdapterTest' grande (LOC=224, métodos=12, campos=9) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoIncrementalPersistenceAdapterTest.java`
- clase 'ProfesionalPersistenceAdapterTest' grande (LOC=316, métodos=36, campos=6) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- clase 'ProfesionalPersistenceAdapterTest' con baja cohesión (13%) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- método 'ProfesionalPersistenceAdapterTest.guardaYRecuperaUnProfesionalConSuCategoria' de 31 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- método 'ProfesionalPersistenceAdapterTest.of' con 6 parámetros — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- método 'HorarioSemanalPersistenceAdapterTest.guardaYRecuperaUnHorarioSemanal' de 36 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/HorarioSemanalPersistenceAdapterTest.java`
- método 'CatalogoSnapshotMapperTest.debeConvertirSnapshotCompleto' de 40 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoSnapshotMapperTest.java`
- método 'CatalogoRestClientRealTest.debeObtenerSnapshotDesdeLaCatedra' de 33 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientRealTest.java`
- método 'CatalogoRestClientAdapterTest.debeEnviarGetConJwtYObtenerSnapshot' de 32 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapterTest.java`
- método 'CatalogoRestClientAdapterTest.debeDeserializarSnapshotCompleto' de 71 líneas — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapterTest.java`
- clase 'ProfesionalControllerTest' grande (LOC=145, métodos=13, campos=3) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalControllerTest.java`
- clase 'ProfesionalControllerTest' con baja cohesión (19%) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalControllerTest.java`
- método 'SincronizarCatalogoIntegrationTest.debeObtenerYGuardarSnapshotEnPostgreSQL' de 31 líneas — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIntegrationTest.java`
- clase 'ProcesarNotificacionCatalogoServiceTest' grande (LOC=110, métodos=10, campos=5) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoServiceTest.java`
- método 'AplicarSnapshotCatalogoUseCaseTest.delegaSnapshotConDatosAlPuertoDePersistencia' de 46 líneas — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/AplicarSnapshotCatalogoUseCaseTest.java`
- clase 'ProcesarNotificacionCatalogoIntegrationTest' grande (LOC=464, métodos=37, campos=12) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- clase 'ProcesarNotificacionCatalogoIntegrationTest' con baja cohesión (49%) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- método 'ProcesarNotificacionCatalogoIntegrationTest.arranqueYEventosDuplicadosConcurrentesCompartenBloqueoHastaElCommit' de 37 líneas — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- método 'ProcesarNotificacionCatalogoIntegrationTest.reconciliacionEsperaAlEventoKafkaQueYaEstaSincronizando' de 31 líneas — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- clase 'SincronizarCatalogoIncrementalUseCaseTest' grande (LOC=224, métodos=12, campos=5) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCaseTest.java`

**DIP/concretos** (85)
- clase 'CatalogoRedisAdapter' muy acoplada (CBO=26) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- clase 'CatalogoRedisAdapter' instancia 17 concretos (CBO=26) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapter.java`
- clase 'ReconciliacionCatalogoInicioAdapter' muy acoplada (CBO=10) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/startup/ReconciliacionCatalogoInicioAdapter.java`
- clase 'JwtConfig' muy acoplada (CBO=22) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtConfig.java`
- clase 'JwtConfig' instancia 4 concretos (CBO=22) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtConfig.java`
- clase 'SecurityConfig' muy acoplada (CBO=12) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/security/SecurityConfig.java`
- clase 'CatalogoKafkaConsumerAdapter' muy acoplada (CBO=19) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapter.java`
- clase 'CatalogoKafkaConsumerAdapter' instancia 11 concretos (CBO=19) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapter.java`
- clase 'ProfesionalJpaRepository' muy acoplada (CBO=17) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/repository/ProfesionalJpaRepository.java`
- clase 'EventoCatalogoProcesadoJpaRepository' muy acoplada (CBO=9) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/repository/EventoCatalogoProcesadoJpaRepository.java`
- clase 'CatalogoSnapshotPersistenceAdapter' muy acoplada (CBO=23) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapter.java`
- clase 'CatalogoSnapshotPersistenceAdapter' instancia 3 concretos (CBO=23) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapter.java`
- clase 'CatalogoIncrementalPersistenceAdapter' muy acoplada (CBO=20) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoIncrementalPersistenceAdapter.java`
- clase 'CatalogoIncrementalPersistenceAdapter' instancia 5 concretos (CBO=20) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoIncrementalPersistenceAdapter.java`
- clase 'EstadoSincronizacionPersistenceAdapter' muy acoplada (CBO=8) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/EstadoSincronizacionPersistenceAdapter.java`
- clase 'CatalogoSnapshotMapper' muy acoplada (CBO=12) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoSnapshotMapper.java`
- clase 'CatalogoSnapshotMapper' instancia 4 concretos (CBO=12) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoSnapshotMapper.java`
- clase 'CatalogoRestClientAdapter' muy acoplada (CBO=13) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapter.java`
- clase 'CatalogoRestClientAdapter' instancia 5 concretos (CBO=13) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapter.java`
- clase 'ProfesionalController' muy acoplada (CBO=24) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalController.java`
- clase 'ProfesionalController' instancia 6 concretos (CBO=24) — `src/main/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalController.java`
- clase 'ProcesarNotificacionCatalogoService' muy acoplada (CBO=12) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoService.java`
- clase 'ConsultarHorariosProfesionalUseCase' muy acoplada (CBO=8) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarHorariosProfesionalUseCase.java`
- clase 'ConsultarProfesionalUseCase' muy acoplada (CBO=10) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarProfesionalUseCase.java`
- clase 'ConsultarProfesionalUseCase' instancia 4 concretos (CBO=10) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarProfesionalUseCase.java`
- clase 'AplicarSnapshotCatalogoUseCase' muy acoplada (CBO=9) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/AplicarSnapshotCatalogoUseCase.java`
- clase 'SincronizarCatalogoIncrementalUseCase' muy acoplada (CBO=19) — `src/main/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCase.java`
- clase 'HorarioSemanal' muy acoplada (CBO=9) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanal.java`
- clase 'HorarioSemanal' instancia 5 concretos (CBO=9) — `src/main/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanal.java`
- clase 'CatalogoRedisRealIntegrationTest' muy acoplada (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisRealIntegrationTest.java`
- clase 'CatalogoRedisRealIntegrationTest' instancia 4 concretos (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisRealIntegrationTest.java`
- clase 'CatalogoRedisAdapterTest' muy acoplada (CBO=19) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/redis/CatalogoRedisAdapterTest.java`
- clase 'ReconciliacionCatalogoInicioIntegrationTest' muy acoplada (CBO=13) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/startup/ReconciliacionCatalogoInicioIntegrationTest.java`
- clase 'ReconciliacionCatalogoInicioIntegrationTest' instancia 3 concretos (CBO=13) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/startup/ReconciliacionCatalogoInicioIntegrationTest.java`
- clase 'JwtSecurityHttpTest' muy acoplada (CBO=54) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtSecurityHttpTest.java`
- clase 'JwtSecurityHttpTest' instancia 17 concretos (CBO=54) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtSecurityHttpTest.java`
- clase 'JwtTestContextInitializer' muy acoplada (CBO=18) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtTestContextInitializer.java`
- clase 'JwtTestContextInitializer' instancia 3 concretos (CBO=18) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtTestContextInitializer.java`
- clase 'JwtConfigTest' muy acoplada (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/security/JwtConfigTest.java`
- clase 'CatalogoKafkaConsumerAdapterTest' muy acoplada (CBO=18) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- clase 'CatalogoKafkaConsumerAdapterTest' instancia 4 concretos (CBO=18) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- clase 'CatalogoKafkaConsumerIntegrationTest' muy acoplada (CBO=38) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerIntegrationTest.java`
- clase 'CatalogoKafkaConsumerIntegrationTest' instancia 8 concretos (CBO=38) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerIntegrationTest.java`
- clase 'HorarioSemanalMapperTest' muy acoplada (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/mapper/HorarioSemanalMapperTest.java`
- clase 'CatalogoSnapshotPersistenceAdapterTest' muy acoplada (CBO=38) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- clase 'CatalogoSnapshotPersistenceAdapterTest' instancia 20 concretos (CBO=38) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoSnapshotPersistenceAdapterTest.java`
- clase 'CatalogoIncrementalPersistenceAdapterTest' muy acoplada (CBO=29) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoIncrementalPersistenceAdapterTest.java`
- clase 'CatalogoIncrementalPersistenceAdapterTest' instancia 6 concretos (CBO=29) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/CatalogoIncrementalPersistenceAdapterTest.java`
- clase 'ProfesionalPersistenceAdapterTest' muy acoplada (CBO=71) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- clase 'ProfesionalPersistenceAdapterTest' instancia 22 concretos (CBO=71) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/ProfesionalPersistenceAdapterTest.java`
- clase 'EstadoSincronizacionPersistenceAdapterTest' muy acoplada (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/EstadoSincronizacionPersistenceAdapterTest.java`
- clase 'HorarioSemanalPersistenceAdapterTest' muy acoplada (CBO=16) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/HorarioSemanalPersistenceAdapterTest.java`
- clase 'HorarioSemanalPersistenceAdapterTest' instancia 7 concretos (CBO=16) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/persistence/adapter/HorarioSemanalPersistenceAdapterTest.java`
- clase 'CatalogoSnapshotMapperTest' muy acoplada (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoSnapshotMapperTest.java`
- clase 'CatalogoSnapshotMapperTest' instancia 7 concretos (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoSnapshotMapperTest.java`
- clase 'CatalogoRestClientRealTest' muy acoplada (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientRealTest.java`
- clase 'CatalogoRestClientAdapterTest' muy acoplada (CBO=27) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapterTest.java`
- clase 'CatalogoRestClientAdapterTest' instancia 12 concretos (CBO=27) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/client/CatalogoRestClientAdapterTest.java`
- clase 'EstadoSincronizacionControllerTest' muy acoplada (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/EstadoSincronizacionControllerTest.java`
- clase 'EstadoSincronizacionControllerTest' instancia 3 concretos (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/EstadoSincronizacionControllerTest.java`
- clase 'ProfesionalControllerTest' muy acoplada (CBO=28) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalControllerTest.java`
- clase 'ProfesionalControllerTest' instancia 14 concretos (CBO=28) — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/web/controller/ProfesionalControllerTest.java`
- clase 'SincronizarCatalogoIntegrationTest' muy acoplada (CBO=20) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIntegrationTest.java`
- clase 'SincronizarCatalogoIntegrationTest' instancia 3 concretos (CBO=20) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIntegrationTest.java`
- clase 'SincronizarCatalogoUseCaseTest' muy acoplada (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoUseCaseTest.java`
- clase 'SincronizarCatalogoUseCaseTest' instancia 4 concretos (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoUseCaseTest.java`
- clase 'ConsultarProfesionalUseCaseTest' muy acoplada (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarProfesionalUseCaseTest.java`
- clase 'ConsultarProfesionalUseCaseTest' instancia 4 concretos (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarProfesionalUseCaseTest.java`
- clase 'ConsultarEstadoSincronizacionUseCaseTest' muy acoplada (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarEstadoSincronizacionUseCaseTest.java`
- clase 'ConsultarEstadoSincronizacionUseCaseTest' instancia 4 concretos (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarEstadoSincronizacionUseCaseTest.java`
- clase 'ReconciliarCatalogoUseCaseTest' muy acoplada (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ReconciliarCatalogoUseCaseTest.java`
- clase 'ReconciliarCatalogoUseCaseTest' instancia 3 concretos (CBO=8) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ReconciliarCatalogoUseCaseTest.java`
- clase 'ConsultarHorariosProfesionalUseCaseTest' muy acoplada (CBO=13) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarHorariosProfesionalUseCaseTest.java`
- clase 'ConsultarHorariosProfesionalUseCaseTest' instancia 5 concretos (CBO=13) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ConsultarHorariosProfesionalUseCaseTest.java`
- clase 'ProcesarNotificacionCatalogoServiceTest' muy acoplada (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoServiceTest.java`
- clase 'ProcesarNotificacionCatalogoServiceTest' instancia 11 concretos (CBO=9) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoServiceTest.java`
- clase 'AplicarSnapshotCatalogoUseCaseTest' muy acoplada (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/AplicarSnapshotCatalogoUseCaseTest.java`
- clase 'AplicarSnapshotCatalogoUseCaseTest' instancia 4 concretos (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/AplicarSnapshotCatalogoUseCaseTest.java`
- clase 'ProcesarNotificacionCatalogoIntegrationTest' muy acoplada (CBO=64) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- clase 'ProcesarNotificacionCatalogoIntegrationTest' instancia 31 concretos (CBO=64) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`
- clase 'SincronizarCatalogoRealIntegrationTest' muy acoplada (CBO=11) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoRealIntegrationTest.java`
- clase 'SincronizarCatalogoIncrementalUseCaseTest' muy acoplada (CBO=10) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCaseTest.java`
- clase 'SincronizarCatalogoIncrementalUseCaseTest' instancia 3 concretos (CBO=10) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/SincronizarCatalogoIncrementalUseCaseTest.java`
- clase 'BuscarProfesionalesUseCaseTest' muy acoplada (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/BuscarProfesionalesUseCaseTest.java`
- clase 'BuscarProfesionalesUseCaseTest' instancia 13 concretos (CBO=15) — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/BuscarProfesionalesUseCaseTest.java`

**LSP/overrides** (1)
- 'ProcesarNotificacionCatalogoIntegrationTest.debeRevertirIncrementosSiRedisNoAlcanzaVersionNotificada' vacío o 'no implementado' — `src/test/java/com/example/svccatalogo/catalogo/application/usecases/ProcesarNotificacionCatalogoIntegrationTest.java`

**duplicación** (6)
- duplicación lógica: 8.5% · boilerplate: 7.4%
- bloque de 8 líneas repetido 7 veces en src/test/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanalTest.java:19, src/test/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanalTest.java:20, src/test/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanalTest.java:21 — `src/test/java/com/example/svccatalogo/catalogo/domain/model/HorarioSemanalTest.java`
- bloque de 8 líneas repetido 5 veces en src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:148, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:155, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:162 — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- bloque de 8 líneas repetido 5 veces en src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:150, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:157, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:164 — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- bloque de 8 líneas repetido 5 veces en src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:152, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:159, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:166 — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`
- bloque de 8 líneas repetido 5 veces en src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:149, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:156, src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java:163 — `src/test/java/com/example/svccatalogo/catalogo/infrastructure/kafka/CatalogoKafkaConsumerAdapterTest.java`

## 6. Evolución por commit

Métricas calculadas sobre el estado de `main` en **cada commit** (20 commits con trabajo), del primero al último. La calidad usa el perfil `exigente`; el aporte de código lo mide RepoSense. El índice puede **bajar** al crecer el proyecto: se acumulan señales.

```mermaid
xychart-beta
    title "Índice de calidad a lo largo de los commits"
    x-axis ["dde650c", "166fe85", "9f95fbf", "8288b7a", "98aec9b", "e8296a9", "36bc83c", "2f9c036", "2e04637", "f556159", "98585e8", "d95d83c", "710b4d7", "2df7127", "28739c1", "c47a2f7", "ef20794", "3f10d18", "e4d80b7", "7f15d77"]
    y-axis "Índice" 0 --> 100
    line [0, 15, 35, 40, 37, 47, 54, 57, 59, 59, 59, 59, 58, 58, 58, 58, 61, 61, 65, 65]
```

| # | Fecha | Commit | Autor | LOC | Índice | %POO | Duplic. | Sueltas |
|---|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2026-09-07 | `dde650c` | Pilar Rocío Portal Romano | 0 | 0 | 0% | 0.0% | 0 |
| 2 | 2026-09-10 | `166fe85` | Ing. Daniel Quinteros | 0 | 15 | 0% | 0.0% | 0 |
| 3 | 2026-10-12 | `9f95fbf` | Pilar Rocío Portal Romano | 18 | 35 | 56% | 0.0% | 0 |
| 4 | 2026-10-16 | `8288b7a` | Pilar Rocío Portal Romano | 18 | 40 | 56% | 0.0% | 0 |
| 5 | 2026-10-17 | `98aec9b` | Ing. Daniel Quinteros | 313 | 37 | 90% | 0.0% | 0 |
| 6 | 2026-10-20 | `e8296a9` | Pilar Rocío Portal Romano | 1,658 | 47 | 80% | 4.5% | 0 |
| 7 | 2026-10-23 | `36bc83c` | Pilar Rocío Portal Romano | 1,842 | 54 | 81% | 8.5% | 0 |
| 8 | 2026-10-25 | `2f9c036` | Pilar Rocío Portal Romano | 2,414 | 57 | 84% | 12.6% | 0 |
| 9 | 2026-10-26 | `2e04637` | Pilar Rocío Portal Romano | 2,684 | 59 | 83% | 12.6% | 0 |
| 10 | 2026-10-28 | `f556159` | Pilar Rocío Portal Romano | 2,858 | 59 | 83% | 11.9% | 0 |
| 11 | 2026-10-30 | `98585e8` | Pilar Rocío Portal Romano | 3,257 | 59 | 83% | 10.3% | 0 |
| 12 | 2026-11-01 | `d95d83c` | Pilar Rocío Portal Romano | 4,314 | 59 | 84% | 9.8% | 0 |
| 13 | 2026-11-07 | `710b4d7` | Pilar Rocío Portal Romano | 5,255 | 58 | 85% | 10.9% | 0 |
| 14 | 2026-11-08 | `2df7127` | Pilar Rocío Portal Romano | 5,388 | 58 | 85% | 10.2% | 0 |
| 15 | 2026-11-08 | `28739c1` | Pilar Rocío Portal Romano | 5,608 | 58 | 85% | 9.8% | 0 |
| 16 | 2026-11-08 | `c47a2f7` | Pilar Rocío Portal Romano | 6,046 | 58 | 84% | 9.2% | 0 |
| 17 | 2026-11-09 | `ef20794` | Pilar Rocío Portal Romano | 6,334 | 61 | 84% | 9.5% | 0 |
| 18 | 2026-11-09 | `3f10d18` | Pilar Rocío Portal Romano | 6,658 | 61 | 84% | 9.3% | 0 |
| 19 | 2026-11-09 | `e4d80b7` | Pilar Rocío Portal Romano | 7,094 | 65 | 84% | 8.7% | 0 |
| 20 | 2026-11-10 | `7f15d77` | Pilar Rocío Portal Romano | 7,582 | 65 | 84% | 8.5% | 0 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
