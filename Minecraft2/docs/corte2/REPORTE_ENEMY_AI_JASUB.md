# Reporte de trabajo — Enemy AI (Estudiante 2, Jasub)

Fecha: 2026-09-21. Base: `master` en `6202362`. Trabajo en working tree, **sin commit** (pendiente
de revisión y aprobación de Jasub; el plan pedía rama `feature/c2-jasub`, que no existe localmente).

## Resumen

Se implementó el frente completo de Enemy AI del Corte 2: zombi de sesión, FSM con TDD real,
navegación 2.5D con A*, seguimiento de ruta con repath inteligente, servicio de actualización,
golpe cuerpo a cuerpo, muerte del jugador por zombi, render mínimo y explosión visual.
Suite: **110 pruebas, 0 fallos** (63 baseline + 47 nuevas). Dos capturas automáticas de evidencia.

## Archivos

### Nuevos — dominio (`src/domain/enemy/`)

| Archivo | Líneas aprox. | Contenido |
| --- | --- | --- |
| `Zombie.java` | 110 | entidad de sesión: posición, salud, estado, temporizadores |
| `ZombiePerception.java` | 15 | record de entrada de la FSM con validación |
| `ZombieStateMachine.java` | 70 | FSM pura IDLE/CHASE/ATTACK/DEAD (TDD) |
| `ZombieParameters.java` | 45 | parámetros centralizados + `defaults()` |
| `NavigationNode.java` | 25 | celda `(x, groundY, z)` |
| `NavigationGrid.java` | 120 | caminabilidad sobre World actual, vecinos 4, subir 1 / bajar 3 |
| `Path.java` | 45 | waypoints con cursor |
| `AStarPathfinder.java` | 130 | A* con open/closed, g/h/f, parent, presupuesto, instrumentación |
| `ZombieMovement.java` | 70 | seguimiento de waypoints, auto-step, detección de bloqueo |

### Nuevos — aplicación y presentación

| Archivo | Contenido |
| --- | --- |
| `src/application/EnemyUpdateService.java` | coordina percepción → FSM → chase/attack/dead, repath, despawn, ON/OFF, registro/spawn |
| `src/application/ZombieMeleeService.java` | golpe del jugador: rayo-caja, alcance 4, oclusión por bloques, daño 1 |
| `src/presentation/game/ZombieRenderer.java` | caja verde por zombi, explosión naranja escalada al morir, `dispose` |

### Modificados

| Archivo | Cambio | Nota de coordinación |
| --- | --- | --- |
| `src/presentation/game/GameInput.java` | constructor recibe `ZombieMeleeService` y `EnemyUpdateService`; clic izquierdo golpea zombi en alcance antes de romper bloque | zona de Estudiante 2 |
| `src/presentation/game/VoxelGame.java` | crea servicios y renderer; `update` de IA con tope 0.05 s; dibuja zombis en el mismo batch; HUD «Zombis: N»; tecla Z y `-Dmc2.zombies=N` provisionales; `dispose` del renderer | **hotspot compartido**: el plan autorizaba integrar en ventana coordinada; avisar al equipo antes de integrar |

Constructores públicos de `VoxelGame` intactos; `VoxelGameObserverTest` y `GameWindow` no cambian.
No se tocaron `World.java`, `MainMenu.java`, `pom.xml`, `README.md`, JSON ni generación.

### Nuevos — pruebas

`test/domain/enemy/`: `TestWorlds` (fixture), `ZombieStateMachineTest` (13), `ZombieTest` (4),
`NavigationGridTest` (6), `AStarPathfinderTest` (8), `ZombieMovementTest` (3).
`test/application/`: `EnemyUpdateServiceTest` (9), `ZombieMeleeServiceTest` (4).

### Nuevos — documentación

`docs/corte2/WIKI_ENEMY_AI_JASUB.md` (guía de estudio en 8 secciones), este reporte,
`docs/corte2/enemy-ai.md` reescrito, `docs/corte2/testing/tdd-zombie-fsm.md` con evidencia real,
`docs/corte2/evidencias/enemy-ai-chase.png` y `enemy-ai-death.png`, `TODO.md` actualizado.

## Cronología de lo que se hizo

1. Lectura de TODO, plan, contratos, arquitectura y código base (World, Chunk, Player, PlayerLife,
   CollisionResolver, PlayerInteractionService, GameInput, VoxelGame).
2. Baseline: `mvn clean test` → 63/63.
3. TDD FSM: test + stub → RED (8 fallos de aserción) → GREEN (13/13) → REFACTOR (13/13, diff guardado).
4. `Zombie` + `ZombieParameters` + `ZombieTest` → suite 80/80.
5. Navegación: `NavigationNode/Grid`, `Path`, `AStarPathfinder`, `ZombieMovement` + tests.
   Un test rojo real (escalón): la caja tocaba la columna vecina antes de que el centro entrara;
   se corrigió con auto-step sobre todas las columnas pisadas → 97/97.
6. `EnemyUpdateService` + `ZombieMeleeService` + tests de integración → 110/110.
7. Integración visual en `GameInput`/`VoxelGame`/`ZombieRenderer`; `mvn clean package` OK.
8. Capturas automáticas con el menú alimentado por stdin.
9. Documentación y TODO.

## Comandos ejecutados y resultados reales

```text
mvn -q clean test                                   → 63 run, 0 fail   (baseline)
mvn -Dtest=ZombieStateMachineTest test              → 13 run, 8 fail   (RED)
mvn -Dtest=ZombieStateMachineTest test              → 13 run, 0 fail   (GREEN)
mvn -q test                                         → 80 run, 0 fail   (REFACTOR + ZombieTest)
mvn -q test                                         → 97 run, 1 fail   (escalón; corregido)
mvn -q test                                         → 97 run, 0 fail
mvn -q test                                         → 110 run, 0 fail
mvn -q clean package                                → BUILD OK, jar 16.5 MB, 110 run, 0 fail
git diff --check                                    → sin problemas de espacios
printf '3\ntest1\n6\n0\n' | java -Dmc2.zombies=3 -Dmc2.screenshot=...chase.png -Dmc2.screenshot.frame=60 -jar ...
printf '3\ntest1\n6\n0\n' | java -Dmc2.zombies=3 -Dmc2.screenshot=...death.png -Dmc2.screenshot.frame=240 -jar ...
```

Capturas: a 1 s se ven las cajas verdes acercándose y el HUD «Zombis: 3» (FPS 13 en arranque por
construcción de mallas; 59–61 después). A 4 s, «MORISTE» con el jugador todavía en y=33: muerte por
ataque, no por caída.

## Decisiones tomadas (documentadas en enemy-ai.md)

Cuerpo 0.6×1.8; `groundY` = bloque de apoyo, pies en `groundY+1`; vecinos 4; subir 1 / bajar 3;
chunk ausente no caminable; sin ruta = `Optional.empty()` + espera 1 s; repath por ruta
nula/terminada/bloqueada, jugador a >1.5 del destino o cada 1.5 s; percepción por distancia 3D;
ataque 1.6 con cooldown 1 s → `PlayerLife.die(ENEMY)`; 3 golpes de salud; despawn 0.8 s;
contrato de registro `register/spawnAt/zombies/setEnabled` propuesto a Estudiante 3.

## Qué NO se probó o queda pendiente

- **Manual con ratón**: golpe con clic izquierdo sobre zombi, explosión visual, eliminación de bloques
  con zombi fuera de alcance, R con zombis activos, J/K durante persecución. La lógica de ambos caminos
  del clic está cubierta por `ZombieMeleeServiceTest`, pero no el cableado LibGDX.
- **Rodeo visual** de paredes en la ventana (probado solo headless).
- Mundo **Grande** con zombis (solo `test1`, Mediano).
- Línea de visión: no implementada; los zombis detectan a través de paredes.
- Texturas de enemigos ON/OFF, dificultad y HordeManager: Estudiante 3.

## Siguientes pasos sugeridos

1. Jasub revisa el diff y decide crear `feature/c2-jasub` y commitear en pasos pequeños
   (dominio+TDD, navegación, servicio, integración visual, docs).
2. Avisar al equipo del cambio en `VoxelGame.java` (hotspot) y de la firma nueva de `GameInput`.
3. Smoke manual del equipo con la lista de arriba; registrar resultado en `enemy-ai.md`.
4. Acordar con Ethian: `ZombieParameters` por `Difficulty`, quién llama a `register/spawnAt`,
   y retirar la tecla Z y `mc2.zombies` cuando exista HordeManager.
5. Marcar en `arquitectura-corte2.md` los acuerdos ya cerrados (edición del owner de ese doc).
