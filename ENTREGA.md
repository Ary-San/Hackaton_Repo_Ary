# ENTREGA — Tuckersoft Branch Engine

Proyecto Spring Boot 3.3.4 / Java 21 en la raíz de este repositorio, con PostgreSQL y
`autotests/` sin modificar.

---

## 1. Resumen de estrellas

Salida de `cd autotests && ./mvnw test` con la aplicación corriendo:

```
  ──────────────────────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   motor: http://localhost:8080        corrida: MUBWE64V
  ──────────────────────────────────────────────────────────────

   ★★★★★   5 / 5   Cinco estrellas.

   ✔  ★1  SEGURIDAD    65 comprobaciones
   ✔  ★2  NODOS        37 comprobaciones
   ✔  ★3  PARTIDAS     40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA   41 comprobaciones

   Las cinco estrellas. Bandersnatch sale para Navidad.
  ──────────────────────────────────────────────────────────────
```

`Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` — 284 comprobaciones en total.

Tests unitarios propios (`./mvnw test` desde la raíz, sin PostgreSQL ni red):
`Tests run: 18, Failures: 0, Errors: 0` — 10 de `DecisionServiceTest` y 8 de
`BranchClassifierTest`.

---

## 2. El flujo asíncrono

El objetivo es que el cliente reciba su **201 inmediato** y que el correo salga después,
en otro hilo y solo si PostgreSQL confirmó la transacción.

```
POST /api/v1/decisions
        │
        ▼
  DecisionService.registrar()          @Transactional
   1. Usuario del token (AuthenticatedUser), la partida tiene que ser suya y estar ACTIVA
   2. BranchClassifier.clasificar(rawInput) → handlerUnit y outcomeCode salen del enum
   3. ENTRADA_CORRUPTA → se guarda con status ERROR y se corta aquí: ni stats, ni evento
   4. Stats acotados 0–100, nodo destino, estado de la partida
   5. Se guardan Playthrough y Decision (status = REGISTRADA)
   6. eventPublisher.publishEvent(DecisionCommittedEvent)
        │
        ├──────────────────────────────► 201 al cliente (sin esperar al correo)
        │
   PostgreSQL hace COMMIT
        │
        ▼
  BranchNotificationListener.alCommit()      @Component aparte
  @Async("branchExecutor")
  @Transactional(propagation = REQUIRES_NEW)
  @TransactionalEventListener(phase = AFTER_COMMIT)
   7. decision.status → PROCESANDO
   8. RealityReportMailer.enviar() → JavaMailSender, envío real por SMTP
        ├── OK    → ESTABILIZADA + RealityLog SENT con sentAt
        └── Fallo → ERROR + RealityLog FAILED con errorMessage + log.error()
   9. [BRANCH-LOG] con el nombre del hilo
```

Tres decisiones de diseño que el enunciado pide explícitamente:

- **`@TransactionalEventListener(AFTER_COMMIT)` y no `@EventListener`.** Con
  `@EventListener` el evento se dispara antes del commit y el hilo asíncrono podría
  buscar una decisión que todavía no existe en la base.
- **El listener vive en `BranchNotificationListener`, un `@Component` distinto del
  service.** Spring no aplica `@Async` a llamadas internas del mismo bean: si esto
  estuviera dentro de `DecisionService`, el correo se enviaría en el hilo de la petición
  y el 201 tardaría segundos. `DecisionService` no inyecta `JavaMailSender` ni conoce al
  listener: solo publica el evento.
- **`REQUIRES_NEW`.** La transacción original ya se cerró cuando el listener arranca, y
  Spring rechaza un `@Transactional` normal sobre un `@TransactionalEventListener`.

El pool es un `ThreadPoolTaskExecutor` (`corePoolSize` 2, `maxPoolSize` 4,
`queueCapacity` 50, prefijo `branch-worker-`) declarado en `AsyncConfig` con
`@EnableAsync`. En el log se comprueba que el hilo es el correcto:

```
[branch-worker-1] [BRANCH-LOG] Decision ID: 35 | Player: QA-...-ASYNC | Branch: OBEDIENCIA
   | Impact: LEVE | Unit: Mesa de Guion | Node: ...-BUCLE -> ...-BUCLE
   | Thread: branch-worker-1 | Status: ESTABILIZADA
```

**Modo QA.** La cabecera `X-Bandersnatch-Simulate: MAIL_FAILURE` viaja como un campo
booleano del evento hasta el listener. `RealityReportMailer.enviar()` lanza entonces un
`MailSendException` real en el mismo punto donde reventaría un fallo de SMTP de verdad,
así que lo atrapa el mismo `catch` y se ejerce la rama `FAILED` del `RealityLog`. No se
escribe el log `FAILED` a mano. El código HTTP sigue siendo 201 y un valor desconocido
en la cabecera no cambia nada ni produce un 400.

---

## 3. Lo que no llegó a terminarse

Nada del alcance del enunciado quedó fuera: las cinco estrellas están en verde y los
cinco tests unitarios obligatorios están escritos (hay 18 en total).

Quedan dos cosas pendientes, ajenas al código:

1. **`equipo.json` sigue con los valores de ejemplo** (`G00` y tres
   `Nombre Apellido / CODIGO UTEC`). Hay que rellenarlo con el nombre del equipo y los
   tres integrantes con su código UTEC antes de la entrega; mientras tenga los valores
   de ejemplo el tablero del auditorio no publica la corrida.
2. **El `.env` no se commitea** (está en `.gitignore`). En el repositorio solo va
   `.env.example`, con placeholders y sin ningún secreto real.

### Detalle del entorno de desarrollo

En la máquina donde se probó había **dos servicios PostgreSQL locales de Windows**
ocupando los puertos 5432 y 5433, así que el contenedor se levantó mapeado al **5434** y
el `.env` local usa `DB_PORT=5434`. Es un detalle de máquina, no del proyecto: con el
puerto 5432 libre funciona con el comando `docker run` tal cual viene en el enunciado y
`DB_PORT=5432`.

---

## 4. Estructura

```
src/main/java/com/tuckersoft/branchengine/
├── BranchEngineApplication.java
├── config/       SecurityConfig · AsyncConfig · DataInitializer
├── security/     JwtService · JwtAuthenticationFilter · AppUserDetailsService
│                 RestAuthenticationEntryPoint · RestAccessDeniedHandler · AuthenticatedUser
├── domain/       User · StoryNode · Playthrough · Decision · RealityLog
│                 BranchType · ImpactLevel · PlaythroughStatus · DecisionStatus · LogStatus · Roles
├── repository/   los cinco repositorios (DecisionRepository con Specifications)
├── dto/          requests, responses, PageResponse y ErrorResponse
├── service/      AuthService · UserService · StoryNodeService · PlaythroughService
│                 DecisionService · BranchClassifier
├── event/        DecisionCommittedEvent
├── listener/     BranchNotificationListener · RealityReportMailer
├── controller/   Auth · User · StoryNode · Playthrough · Decision
└── exception/    ApiException y sus cinco hijas + GlobalExceptionHandler
```

Ningún controller devuelve una entidad JPA: todo sale por DTOs, y ningún response
incluye `password` en ninguna forma.
