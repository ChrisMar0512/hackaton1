# Informe de Entrega: Tuckersoft Branch Engine (Hackathon DBP)

## 1. Resumen de Calificación y Autotests

Ejecución oficial contra la batería de pruebas en `autotests/`:

```
  ──────────────────────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   motor: http://localhost:8080        corrida: MUBVAHGP
  ──────────────────────────────────────────────────────────────

   ★★★★★   5 / 5   Cinco estrellas.

   ✔  ★1  SEGURIDAD    65 comprobaciones
   ✔  ★2  NODOS        37 comprobaciones
   ✔  ★3  PARTIDAS     40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA   41 comprobaciones

   Las cinco estrellas. Bandersnatch sale para Navidad.
  ──────────────────────────────────────────────────────────────

Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 2. Explicación del Flujo Asíncrono Implementado

Para cumplir con el desacoplamiento de alta concurrencia y no bloquear la respuesta HTTP de las decisiones del jugador:

1. **Retorno Inmediato (HTTP 201 Created):**
   - El endpoint `POST /api/v1/decisions` procesa la lógica transaccional de negocio en `DecisionService`: valida la partida, clasifica el texto (`rawInput`), actualiza los atributos del `Playthrough` y persiste la `Decision` con estado inicial `REGISTRADA`.
   - Inmediatamente después de guardar, publica un evento de dominio `DecisionCommittedEvent` a través de Spring `ApplicationEventPublisher`.
   - El endpoint responde al cliente en menos de 1500 ms (típicamente ~20-50 ms) devolviendo el DTO `DecisionResponse`.

2. **Garantía Transaccional (`@TransactionalEventListener`):**
   - El componente `BranchNotificationListener` escucha el evento utilizando `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
   - Esto asegura que el listener solo se active cuando la transacción principal de PostgreSQL ha hecho `COMMIT`, evitando condiciones de carrera o lecturas fantasma de decisiones no confirmadas.

3. **Pool de Hilos Dedicado y Transacción Independiente:**
   - Se configuró un `ThreadPoolTaskExecutor` en `AsyncConfig` con el prefijo `"branch-worker-"`, `corePoolSize = 2`, `maxPoolSize = 4` y `queueCapacity = 50`.
   - El listener está anotado con `@Async("branchExecutor")` y `@Transactional(propagation = Propagation.REQUIRES_NEW)`. La propagación `REQUIRES_NEW` es indispensable para que los cambios de estado (`PROCESANDO` -> `ESTABILIZADA` o `ERROR`) y la creación de la fila de auditoría `RealityLog` se persistan en su propia transacción desacoplada.

4. **Envío de Correo y Auditoría (`RealityLog`):**
   - El listener construye el cuerpo del **Informe de Realidad** respetando todas las etiquetas y campos del guion de 1984 y lo envía mediante `JavaMailSender` con asunto `"[TUCKERSOFT] <branchType> en <playerTag> | Impacto <impactLevel>"`.
   - Si el envío es exitoso, la decisión pasa a `ESTABILIZADA` y se registra un `RealityLog` con `logStatus = SENT` y `sentAt`.
   - Si se simula un fallo mediante `X-Bandersnatch-Simulate: MAIL_FAILURE` o falla la conexión SMTP, el error es atrapado, la decisión se marca como `ERROR` y se registra un `RealityLog` con `logStatus = FAILED` y su `errorMessage`, manteniendo el sistema disponible y sin afectar las transacciones anteriores.
   - Se emite el log obligatorio en consola:
     `[BRANCH-LOG] Decision ID: <id> | Player: <playerTag> | ... | Thread: branch-worker-X | Status: <status>`

---

## 3. Pruebas Unitarias Propias

Se implementaron 5 pruebas unitarias con Mockito en `DecisionServiceTest` que corren sin depender de base de datos ni red:
1. Precedencia de reglas de clasificación (`"Stefan destruye la camara"` -> `RUPTURA_CUARTA_PARED`).
2. Entrada sin letras clasifica como `ENTRADA_CORRUPTA` y la partida no se modifica.
3. Impacto `CRITICO` con límites estrictos (`lucidity` topada en 0, `controlLevel` topado en 100).
4. Final por `controlLevel >= 100` (`ENDING_PAC_SYMBOL`) priorizado sobre `lucidity <= 0`.
5. Invocación de `publishEvent()`: 1 vez en decisión válida y 0 veces en `ENTRADA_CORRUPTA`.

---

## 4. Estado de la Implementación
- **Completitud:** 100% de los requerimientos y checkpoints implementados y aprobados con 5/5 estrellas.
