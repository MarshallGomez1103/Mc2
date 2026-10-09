# ADR-001 — Estilo arquitectónico para la recuperación del Corte 2

| Campo | Valor |
| --- | --- |
| Estado | **Propuesto**. Pendiente de confirmación del equipo en el punto de control (REC-E1). |
| Fecha | 2026-10-08 |
| Autor | Ethian Daniel White Ortiz |
| Revisan | Elioth Thomas Gomez Morales, Jasub Sastre |
| Corrección | REC-02 del [plan de recuperación](../recuperacion-c2/plan-equipo.md) |
| Código evaluado | `main` @ `4220c40` (el código fuente es igual al de `4d33d6a`, cierre del Corte 2) |

## 1. Contexto

Minecraft2 es una aplicación de escritorio Java 17 que se ejecuta en un solo proceso, con LibGDX/LWJGL3 para ventana, entrada y OpenGL. Guarda los mundos como JSON v1 en disco local. El detalle está en los [diagramas de contexto y contenedores](../diagramas/recuperacion-c2/README.md).

### Reto que dirige la decisión

El reto comunicado oralmente al equipo fue **incorporar un enemigo**. Para comparar estilos lo descomponemos en tres exigencias. Ninguna de ellas es un reto docente nuevo:

1. **Conducta.** La FSM (`ZombieStateMachine`), la percepción, A\* (`AStarPathfinder`, `NavigationGrid`), la física del zombi y la política kamikaze deben poder probarse sin ventana ni GPU.
2. **Coordinación por tick.** Vida del jugador, oleadas (`HordeManager`), enemigos (`EnemyUpdateService`), pistola y movimiento del jugador deben avanzar con **un único reloj** y respetar los mismos estados RUNNING/PAUSED/DEAD.
3. **Coste de actualización.** Con hasta decenas de zombis activos, la IA por tick debe caber en el presupuesto del fotograma. El SLO y la medición están en [carga.md](../recuperacion-c2/carga.md).

### Estado actual medido en el código

Inventario de los `import` de `src/`:

| Paquete | Depende de | Observación |
| --- | --- | --- |
| `domain.*` | `domain.*`, `patterns.observer` | Sin LibGDX, aplicación, presentación ni persistencia. Persiste el ciclo `domain.world` ↔ `domain.player`. |
| `application` | `domain.*`, `patterns.factory`, `patterns.singleton`, `persistence.WorldStorage` | Sin LibGDX. Depende de la **interfaz** de almacenamiento, que vive en `persistence`. |
| `persistence` | `domain.*`, `patterns.factory` | JSON v1 con escritura temporal y movimiento atómico. |
| `presentation.game` | `application`, `domain.*`, LibGDX, `bootstrap.GpuPreference` | `GameInput` construye y coordina `PlayerMovementService`, `PlayerPhysics`, `CollisionResolver` y `Stamina`. |
| `bootstrap` | `application`, `persistence`, `presentation` | Raíz de composición. |

El problema que motiva este ADR es REC-04. `VoxelGame.render` llama a `GameSession.advance(delta, () -> input.update(player, world, delta))`. La sesión fija el reloj, pero la coordinación del jugador (leer el teclado, mover, aplicar gravedad, resolver colisiones y gastar estamina) ocurre **dentro de presentación**, mediante un callback. Esta deuda ya estaba en el Corte 1: en `5501b02`, `GameInput` creaba `PlayerMovementService`, `PlayerPhysics` y `CollisionResolver`. El Corte 2 le añadió `Stamina`, combate y pistola.

## 2. Criterios de decisión

Cada criterio sale del reto o de las restricciones de la recuperación:

| # | Criterio | Pregunta concreta |
| --- | --- | --- |
| K1 | Aislamiento de la conducta del enemigo | ¿FSM, A\* y oleadas se prueban sin LibGDX ni disco? |
| K2 | Coordinación del tick | ¿Hay una sola entrada que avance jugador, vida, hordas y enemigos con la misma semántica de pausa, muerte y delta? |
| K3 | Testabilidad de controles | ¿Sprint, salto y estamina se prueban sin teclado ni ventana? |
| K4 | Coste de cambio | ¿Cuántas capas y archivos toca un cambio típico: nueva regla de oleada, otro tipo de enemigo u otro dispositivo de entrada? |
| K5 | Proporcionalidad | ¿El estilo cabe en 2 días y 3 personas sin reescribir paquetes estables ni cambiar JSON v1? |
| K6 | Coste en tiempo de ejecución | ¿Añade trabajo por tick dentro del presupuesto de 16,67 ms? |
| K7 | Riesgo de regresión e integración | ¿Cuántos archivos de otros dueños se tocan y cuántas pruebas hay que migrar? |

## 3. Opciones consideradas

### Opción 0 — Statu quo: capas con coordinación del jugador en presentación

Es la referencia de comparación, no una candidata. Se cumple K1. K2 y K3 fallan: la física y la estamina del jugador avanzan desde un `Runnable` de la vista, y probar el sprint exige construir `GameInput`, que depende de `Gdx.input`. Es la observación REC-04.

### Opción A — Capas con frontera de aplicación explícita

Se mantienen `domain` → `application` → `presentation` / `persistence` / `bootstrap`, y se completa la frontera de entrada según el contrato de la sección 3 del plan:

- `GameInput` solo traduce dispositivos a un dato inmutable, `application.PlayerFrameInput`.
- `application.PlayerControlService` coordina movimiento, física, colisión, estamina e interacción. Reutiliza los servicios de dominio sin cambiar sus reglas.
- `GameSession.advance(double, PlayerFrameInput)` pasa a ser la única entrada de avance. La vista consulta `GameSession.controls()` para el HUD y la cámara.
- `WorldStorage` permanece en `persistence`. La dependencia `application → persistence.WorldStorage` se **declara**, no se oculta.

### Opción B — Puertos y adaptadores (hexagonal)

El núcleo, dominio y aplicación, define todos sus puertos. Los adaptadores viven fuera:

- Puertos de entrada: `PlayerControlUseCase`, `GameLoopUseCase` y `WorldCatalogUseCase`, como interfaces en `application.port.in`.
- Puertos de salida: `WorldRepository` (trasladado desde `persistence.WorldStorage` a `application.port.out`), `Clock` y `RenderNotifier` (sustituiría el Observer que hoy usa `VoxelGame`).
- Adaptadores: LibGDX de entrada y de render, consola, JSON y composición en `bootstrap`.

## 4. Comparación

| Criterio | Opción A: capas con frontera explícita | Opción B: puertos y adaptadores |
| --- | --- | --- |
| K1 Enemigo | **Cumple igual que hoy.** FSM, A\*, física y oleadas ya están en `domain.enemy` y se prueban con mundos en memoria. | **Cumple igual.** No añade nada a la conducta: el enemigo no tiene dependencias externas que invertir. |
| K2 Tick | **Cumple.** `GameSession.advance(delta, PlayerFrameInput)` es la única entrada. Pausa, muerte, delta inválido y límite de 0,05 s se aplican igual al jugador y a la IA. | **Cumple igual**, mediante `GameLoopUseCase`. El puerto de entrada tendría una sola implementación (`GameSession`). |
| K3 Controles | **Cumple.** `PlayerControlService` recibe datos y se prueba sin `Gdx`. | **Cumple igual.** La diferencia es nominal: interfaz más implementación. |
| K4 Cambio | Una regla de oleada cambia `WaveRules` o `Difficulty`: 1 capa. Un enemigo nuevo afecta a dominio + `EnemyUpdateService` + render: 3 capas, igual que hoy. Un dispositivo nuevo, como un mando, solo exige otro traductor que produzca `PlayerFrameInput`. | Iguales en dominio. Un dispositivo nuevo implementa un adaptador del mismo puerto. Una persistencia distinta de JSON se aísla mejor, pero **no hay requisito** de segunda persistencia. |
| K5 Proporción | **Alta.** Unas 3 clases nuevas y cambios en `GameSession`, `GameInput`, `VoxelGame` y `GameHud`, todos asignados a Thomas. | **Baja.** Mover `WorldStorage` toca `persistence`, `application`, `bootstrap` y pruebas de Jasub y Thomas. Hay que crear 4 a 6 interfaces con una sola implementación y cambiar el Observer de `World`, un paquete estable del dominio. |
| K6 Tiempo | Construye un `PlayerFrameInput` por tick: un objeto pequeño, del orden de nanosegundos frente al presupuesto de 16,67 ms. Lo domina A\* (ver carga). | Igual, más una llamada por interfaz. Despreciable. |
| K7 Riesgo | Concentrado en los archivos de Thomas. JSON v1, dominio, IA y generación no cambian. | Cambios repartidos entre los tres dueños, con más conflictos de merge y más pruebas que migrar en 2 días. |

**Resumen.** K1, K3 y K6 no distinguen entre A y B: el dominio del enemigo ya es puro y el coste por tick es despreciable en ambas. Las opciones se separan en **K5 y K7**, donde A es claramente mejor, y en la parte de K4 sobre una persistencia alternativa, donde B sería mejor ante un cambio que hoy no existe.

## 5. Decisión (propuesta)

Se elige la **Opción A: capas con una frontera de aplicación explícita**. Reglas de dependencia que deben poder comprobarse (REC-J6):

1. `domain` no importa `application`, `presentation`, `persistence`, `bootstrap` ni `com.badlogic`.
2. `application` no importa `presentation`, `bootstrap` ni `com.badlogic`. La única dependencia hacia `persistence` es la interfaz `WorldStorage`.
3. `presentation.game.GameInput` no importa `PlayerPhysics`, `CollisionResolver`, `PlayerMovementService` ni `Stamina`.
4. `GameSession.advance(double, PlayerFrameInput)` es la única entrada de avance del juego en la ruta normal.
5. La vista puede **leer** el modelo para dibujar (`World`, `Zombie`, `GameSession.controls()`), pero no coordina reglas.

El estilo no se presenta como hexagonal, porque hay puertos de salida que no son del núcleo.

## 6. Consecuencias

### Ganancias

- Los controles se prueban sin dispositivo: sprint, salto, estamina, pausa, muerte y respawn, con datos de entrada.
- Hay un solo avance de sesión para jugador, vida, hordas, enemigos y pistola. Se elimina la coordinación por callback.
- Las responsabilidades son verificables: las reglas 1 a 3 se comprueban de forma automática.
- `GameInput` queda acoplado solo a dispositivos. Cambiar o añadir un dispositivo no toca reglas.
- El coste es proporcional al plazo: no cambian JSON v1, dominio, IA ni generación.

### Sacrificios y riesgos aceptados

- Hay nuevas clases (`PlayerFrameInput`, `PlayerControlService` y `PlayerControlState`) y una conversión de datos por tick.
- HUD, cámara y pruebas de estamina se adaptan a `GameSession.controls()`. Las pruebas actuales de `GameInputStaminaTest` deben migrarse sin perder casos.
- **`application` sigue dependiendo de `persistence.WorldStorage`**: la inversión de dependencias está incompleta, porque la interfaz no pertenece al núcleo.
- Permanecen el ciclo `domain.world` ↔ `domain.player`, el Singleton `WorldManager` y los mundos finitos en memoria. Este ADR no los resuelve.
- `presentation.game.GraphicalGame` importa `bootstrap.GpuPreference`, una dependencia hacia la raíz de composición que se declara como límite conocido.
- El contrato de entrada se convierte en un punto de coordinación entre los tres frentes: cualquier cambio de firma se acuerda antes de editar consumidores.

### Cuándo revisar esta decisión

Se reevaluará la Opción B si aparece una **segunda implementación real** de un puerto: almacenamiento remoto, entrada de red o multijugador, o un segundo front-end que no sea LibGDX. En ese caso el primer paso sería trasladar `WorldStorage` a `application` como puerto de salida.

## 7. Evidencia y enlaces

- Diagramas: [contexto, contenedores y componentes](../diagramas/recuperacion-c2/README.md), con las vistas Corte 1, cierre Corte 2 y recuperación.
- Carga: [carga.md](../recuperacion-c2/carga.md), con SLO, protocolo y resultados.
- Implementación y pruebas de la frontera: las ramas `feature/c2-Thomas` (REC-T1 a T3) y `feature/c2-jasub` (REC-J2, REC-J6). Thomas añade los enlaces definitivos al integrar.
