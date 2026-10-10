# Arquitectura de Minecraft2 — recuperación del Corte 2

## 1. Sistema y base del Corte 1

Minecraft2 es un juego voxel de un jugador, ejecutado en un proceso JVM. El Corte 1 incorporó mundos/chunks/bloques, generación finita, jugador, movimiento y colisión, interacción por raycast, vista LibGDX y persistencia JSON. Factory construye bloques; World publica BlockChange mediante Observer; WorldManager conserva el mundo activo mediante Singleton.

En el Corte 2 se incorporaron enemigos con FSM/A*, oleadas y estado de partida. La preparación de recuperación está en `4220c4051cf306cabfaaaa79ea8e4c90c868b631`. En esa base GameInput construía/coordinaba PlayerMovementService, PlayerPhysics, CollisionResolver y Stamina, y VoxelGame entregaba un callback a GameSession. Por tanto, la separación declarada en los diagramas no describía por completo la ruta de controles.

La versión integrada conserva el cambio de Thomas, que traslada esa coordinación a aplicación; conserva las reglas de dominio, velocidades, JSON v1 y prioridad de interacción. No agrega funcionalidad de negocio.

## 2. Reto y trazabilidad

El reto oral fue incorporar un enemigo. Esto exige mantener conducta y navegación independientes del render, coordinar su avance con el jugador y congelar ambos al pausar/morir. El coste de actualizar múltiples enemigos afecta el rendimiento; la frontera de entrada afecta mantenibilidad y testabilidad.

La [tabla obligatoria de seis columnas está en el README raíz](../../README.md#tabla-de-trazabilidad), que es su referencia única. Allí se enlazan decisiones, código, pruebas y resultados actuales con límites explícitos. Controles, caja negra de consola y carga headless de la IA tienen ejecuciones con resultados. La presencia de biomas, menús o nuevas funciones no se presenta como una lista de retos asignados.

## 3. Estilo y alternativas

Se implementa una aplicación por capas con frontera de aplicación explícita. La selección y comparación ampliada están en el [ADR-001 de Ethian](adr/ADR-001-estilo-recuperacion.md), contrastado con los controles implementados y las reglas de import. Ethian revisó el ADR contra el candidato final medido.

| Criterio derivado del reto | Capas con coordinación explícita | Puertos y adaptadores completos |
| --- | --- | --- |
| Prueba de conducta y controles sin render | Datos de entrada y coordinador Java puro; reutiliza servicios existentes | Casos de uso y puertos para cada frontera; mayor independencia si se amplían adaptadores |
| Cambio de dispositivo/interfaz | Un traductor forma PlayerFrameInput sin modificar reglas | Adaptador de entrada implementa puertos explícitos |
| Almacenamiento real y pruebas | WorldStorage ya separa el contrato de JSON; su interfaz vive en persistence | El contrato se movería hacia el núcleo y la composición se reorganizaría |
| Proporción y coste en un escritorio de un jugador | Cambio acotado: tres clases, conexión visual y pruebas; conserva estructura existente | Más interfaces, movimiento de paquetes y revisión de consumidores; no hay reto de múltiples proveedores/remotos confirmado |
| Rendimiento de IA | El cambio de frontera no reduce por sí solo búsquedas o expansiones A* | El aislamiento tampoco mejora por sí solo A*; ambos requieren medir y optimizar el cuello real |

La frontera de aplicación permite probar controles sin Gdx y limita el punto que coordina una partida. Se sacrifica simplicidad de cableado: nuevos contratos, un objeto de entrada por tick y adaptación de HUD/tests. No se declara arquitectura hexagonal completa: WorldStorage sigue en persistence, hay colaboración domain.world ↔ domain.player y permanecen apoyos en Factory/Observer/Singleton.

El ADR reúne contexto, opciones, decisión implementada y consecuencias positivas/negativas. No atribuye nanosegundos ni una mejora de rendimiento al traslado de capas sin medición.

## 4. Arquitectura inicial y evolucionada

Las [vistas UML previas](uml.md) se conservan como históricas, sin rotularlas como recuperación. Las [vistas C4 de Ethian](diagramas/recuperacion-c2/README.md) incluyen contexto, contenedores y componentes del Corte 1 (`5501b02`), cierre del Corte 2 (`4d33d6a`) y recuperación. La vista recuperada se ajustó a las llamadas reales de GameSession/PlayerControlService y a los hashes del candidato integrado.

Flujo actual de los controles, comprobado en el candidato integrado:

```mermaid
flowchart LR
    Device[Teclado y ratón] --> Input[GameInput: traduce dispositivos]
    Input --> Frame[PlayerFrameInput: datos inmutables]
    Frame --> Session[GameSession: estado y tick limitado]
    Session --> Control[PlayerControlService: coordina controles]
    Control --> Rules[Movimiento, física, colisión y Stamina]
    Control --> Interaction[Raycast, melee y pistola]
    Session --> Runtime[Vida, HordeManager y EnemyUpdateService]
    Runtime --> AI[FSM, navegación y reglas de dominio]
    Session --> State[PlayerControlState: instantánea]
    State --> View[HUD y efecto de cámara]
```

Reglas de dependencia:

- domain no importa application, presentation, persistence ni LibGDX. Su apoyo en patterns se conserva y se declara.
- application coordina casos de uso sin LibGDX ni presentation. WorldApplicationService usa la abstracción WorldStorage, no un render ni un menú.
- GameInput solo lee Input y produce PlayerFrameInput; no tiene mundo, física, colisiones ni estamina mutable.
- VoxelGame controla recursos y ciclo gráfico, entrega la entrada a GameSession y consulta el modelo para renderizar. Cámara, mallas, HUD y fullscreen permanecen en presentación.
- JsonWorldStorage concentra archivos; el códec concentra JSON. Bootstrap compone el arranque; GameSession compone sus servicios de sesión.
- La lectura visual del dominio no autoriza coordinación física desde presentación. El Observer inverso notifica mediante un contrato sin depender de la vista concreta.

Contrato disponible:

```java
GameSession session = new GameSession(world, settings);
session.advance(deltaSeconds, PlayerFrameInput.NEUTRAL);
PlayerControlState state = session.controls();
```

PlayerFrameInput contiene MovementInput, salto, mirada, acción primaria/secundaria, alternancia de arma y material opcional. `selectedType == null` conserva el material; AIR se rechaza. La neutral mantiene ausencia de acciones pero permite gravedad/vida/IA durante RUNNING.

GameSession descarta deltas no finitos, negativos o cero antes de usar la entrada; pausa y muerte descartan el tick. Un delta positivo se limita a 0,05 s para todos los componentes. PlayerControlService, como operación directa, rechaza deltas negativos/no finitos mediante excepción; esta diferencia conserva el contrato anterior. No existe el avance por Runnable.

Orden de control: mirar → mover/saltar/colisionar → alternar pistola → seleccionar material y desequipar → acción primaria/secundaria. Pistola equipada nunca pica un bloque al fallar o estar en cooldown; sin pistola, melee tiene prioridad y se pica únicamente si no hay enemigo alcanzable. El sprint consume según desplazamiento horizontal real; teclas opuestas y paredes no consumen. Respawn restaura estamina/flags y conserva el material; no levanta una pausa existente. El cierre de VoxelGame después de morir delega también en GameSession, sin invocar directamente la reaparición del dominio.

## 5. Estrategia de pruebas

Unitarias: reglas de dominio y adaptadores aislados sin disco/red/ventana. Incluyen equivalencias, límites y casos inválidos; los tests nuevos usan datos y un doble Input para aislar dispositivos. No se convierte toda la suite en “unitaria” por usar JUnit.

Integración: coordinadores de sesión/control/IA con componentes reales y mundo determinista en memoria; almacenamiento JSON en directorios temporales; menú/Observer con modelos reales. La infraestructura apropiada para este juego es el archivo JSON local, sin añadir HTTP/BD a un sistema que no los usa. GameFlowSmokeIT inicia el JAR en procesos separados y cubre tres escenarios públicos. LocalWorldPersistenceIT y WorldLifecycleIT comprueban reinicio/archivos inválidos con almacenamiento real; cada prueba descarga el Singleton. [Detalle](recuperacion-c2/integracion.md).

Gráfica: PlayerControlsVisualSmoke abre OpenGL real, automatiza la entrada y verifica controles y estado mientras VoxelGame renderiza cámara/HUD. No es un flujo público de consola ni una evaluación humana.

Carga: los scripts/protocolo de Ethian están versionados. Los escenarios de población fija miden EnemyUpdateService headless, sin HordeManager ni el tick completo de sesión; el escenario histórico ENDURANCE sí usa oleadas. Las corridas de baseline/estrés están en perf/results y su análisis en [carga.md](recuperacion-c2/carga.md#7-análisis-rec-e9), con throughput, p95, errores, población sostenida y búsquedas/expansiones A*. No se atribuye una mejora de velocidad al traslado de capas sin medición comparable.

Comandos por nivel y reportes: [README](../../README.md#ejecutar-pruebas-por-nivel) y [pruebas](pruebas.md).

## 6. Resultados y evidencia

La [verificación integrada](pruebas.md) ejecutó 199 pruebas Surefire y 148 Failsafe, sin fallos, errores ni omitidas. Las 60 clases de la base siguen ejecutándose tras mapear la migración de GameInputStaminaTest a PlayerControlStaminaTest y los cuatro renombrados de Jasub a IT; ningún runner comparte una clase con el otro.

JaCoCo reporta solo domain: unitarias 887/984 líneas (90,14 %) y 555/693 ramas (80,09 %); integración 917/984 líneas (93,19 %) y 553/693 ramas (79,80 %). Son reportes independientes; no se suman ni incluyen automáticamente procesos de JAR separados.

El smoke OpenGL automatizado pasó en Intel/Mesa Linux y en Windows 11 con AMD Radeon. Los CSV, casos, cobertura, captura y fuentes del candidato final están en [evidencias/final](recuperacion-c2/evidencias/final/manifest.json); la etapa de integración anterior se conserva en [evidencias/integracion](recuperacion-c2/evidencias/integracion/manifest.json). Pruebas y build se repitieron en una copia sin target previo. Los resultados históricos de septiembre no se sustituyen por estos ni se presentan como mediciones nuevas de carga.

PIT conserva su selección original y se comprobó nuevamente; [resultado de mutación](pruebas.md#mutación). Los tres escenarios públicos de caja negra pasaron. Carga de recuperación: la primera medición (`d180eb9`) ya cumplía SLO-CARGA-01 con 80 zombis (p95 ≤ 9,857 ms) y se detuvo en 160. El perfilado con JFR demostró que el 51 % del tiempo de IA a 160 zombis estaba en `World.findChunk`: la clave del índice de chunks tenía `hashCode = x ^ z` y el `HashMap` degradaba a árboles. Se corrigió en dominio con una regresión, sin tocar otras capas, y se repitió la carga (`c2e03e7`). Con 80 zombis el p95 fue 1,759, 1,926 y 1,679 ms, con 0 ticks fallidos y población sostenida. Con 160 queda en 13,0–13,3 ms y la parada pasa a 320 (mediana 50,341 ms). La conducta simulada es idéntica: mismas búsquedas A*, expansiones y muertes. Ahora limitan las lecturas de bloques de A* y la separación cuadrática. [Resultados y análisis](recuperacion-c2/carga.md#6-resultados).

## 7. Límites y tercer corte

- Mundo finito completo en memoria, un jugador y estado global de WorldManager; no hay streaming ni arquitectura distribuida.
- Coordinación concentrada en GameSession/PlayerControlService; la separación hace comprobables las responsabilidades pero no elimina todos los acoplamientos de dominio.
- JSON v1 conserva el mundo/jugador; vida, arma, enemigos, oleadas, restos y estamina son estado de sesión.
- La carga headless de IA no garantiza FPS gráficos; la prueba visual automatizada tampoco acredita jugabilidad prolongada, equilibrio o SUS.
- Carga medida en un solo equipo Windows, con población fija sin HordeManager ni controles; el coste por tick de PlayerFrameInput sigue sin medirse. ADR, vistas, flujo público, fronteras y carga están incorporados y contrastados con el código.
- Rendimiento pendiente: cachear la caminabilidad durante A\*, escalonar o compartir rutas e indexar los zombis por celda para la separación. Cada cambio exige repetir baseline y estrés.
- Tercer corte: el enunciado anuncia que esta batería de pruebas se automatizará en un pipeline de CI/CD con prácticas DevSecOps. Los comandos por nivel del README y los scripts de `perf/` son el punto de partida; los gates y umbrales se definirán con la consigna de ese corte. No se presupone un umbral de cobertura del 80 %.
