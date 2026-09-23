# Enemy AI — implementación del Corte 2

Estado: **implementado y probado en headless** (110 pruebas verdes); verificación visual parcial con
capturas automáticas; smoke manual con ratón pendiente del equipo. Plan seguido:
[PLAN_AGENTE_JASUB.md](PLAN_AGENTE_JASUB.md). Explicación didáctica: [WIKI_ENEMY_AI_JASUB.md](WIKI_ENEMY_AI_JASUB.md).
Reporte de trabajo: [REPORTE_ENEMY_AI_JASUB.md](REPORTE_ENEMY_AI_JASUB.md).

## Mapa de clases

| Clase | Capa | Responsabilidad |
| --- | --- | --- |
| `Zombie` | domain.enemy | posición continua, salud, `ZombieState`, temporizadores; no hereda de Player |
| `ZombiePerception` | domain.enemy | record de entrada de la FSM: salud, distancia al jugador, jugador vivo |
| `ZombieStateMachine` | domain.enemy | FSM pura IDLE/CHASE/ATTACK/DEAD (TDD: [testing/tdd-zombie-fsm.md](testing/tdd-zombie-fsm.md)) |
| `ZombieParameters` | domain.enemy | parámetros centralizados con `defaults()`; punto de enganche para `Difficulty` |
| `NavigationNode` | domain.enemy | celda 2.5D `(x, groundY, z)`; pies en `groundY + 1` |
| `NavigationGrid` | domain.enemy | caminabilidad sobre el World actual: apoyo, cuerpo, desnivel, chunk existente, vecinos 4 |
| `AStarPathfinder` | domain.enemy | A* con open/closed, g/h/f, parent, reconstrucción, presupuesto e instrumentación |
| `Path` | domain.enemy | waypoints con cursor |
| `ZombieMovement` | domain.enemy | seguimiento de waypoints sin atravesar bloques; auto-step de 1 bloque |
| `EnemyUpdateService` | application | coordina percepción → FSM → navegación/ataque/despawn por fotograma |
| `ZombieMeleeService` | application | golpe del jugador: rayo-caja, alcance 4, oclusión por bloques |
| `ZombieRenderer` | presentation.game | caja verde por zombi; «explosión» visual naranja al morir |

## Decisiones cerradas (antes pendientes en arquitectura-corte2.md)

- **Cuerpo del zombi:** 0.6 × 1.8 × 0.6, igual que Player; ocupa dos bloques de aire.
- **groundY vs pies:** `groundY` es el bloque sólido de apoyo; los pies están en `groundY + 1`.
- **Vecinos:** 4 direcciones (sin diagonales) para no cortar esquinas de paredes.
- **Desnivel:** subir máximo 1, bajar máximo 3 (`NavigationGrid.MAX_CLIMB/MAX_DROP`).
- **Chunk ausente:** nunca es caminable; origen o destino fuera de chunks → sin ruta.
- **Sin ruta:** `Optional.empty()`; el servicio espera `noRouteRetrySeconds` (1 s) antes de reintentar.
- **Invalidación por bloques:** no hay caché; la rejilla lee el World en cada consulta y
  `ZombieMovement.follow` devuelve `false` si un bloque nuevo cierra el paso → repath al siguiente frame.
- **Repath:** sin ruta, ruta terminada/bloqueada, jugador a más de 1.5 bloques del destino planificado o
  intervalo de 1.5 s. Nunca un A* por zombi y frame (probado: 120 frames → 1–3 búsquedas).
- **Percepción:** distancia euclidiana 3D sin línea de visión (limitación documentada).
- **Ataque:** alcance 1.6, cooldown 1 s (también antes del primer golpe), efecto = `PlayerLife.die(ENEMY)`.
- **Salud:** 3 golpes de jugador (`ZombieMeleeService.DAMAGE = 1`).
- **Muerte:** DEAD absorbente; despawn a los 0.8 s; el renderer hincha y colorea la caja mientras tanto.
  No rompe bloques, no hay daño de área.
- **Registro (contrato propuesto a Estudiante 3):** `EnemyUpdateService.register(Zombie)`,
  `spawnAt(x, z)`, `zombies()`, `setEnabled(boolean)`. HordeManager decide cuándo y cuántos;
  este servicio decide cómo se comportan.
- **Spawn provisional:** tecla **Z** genera un zombi 6 bloques delante; `-Dmc2.zombies=N` para capturas.
  Ambos se retiran cuando exista HordeManager.

## Parámetros por defecto (`ZombieParameters.defaults()`)

| Parámetro | Valor | Parámetro | Valor |
| --- | --- | --- | --- |
| detectionRange | 12 | moveSpeed | 2.6 b/s (jugador 4.3) |
| attackRange | 1.6 | maxHealth | 3 |
| loseTargetRange | 18 | attackCooldownSeconds | 1.0 |
| repathIntervalSeconds | 1.5 | repathDistance | 1.5 |
| noRouteRetrySeconds | 1.0 | | |

## Evidencia

- Headless: `mvn clean package` → 110 pruebas, 0 fallos (63 baseline + 47 nuevas).
- Visual: `printf '3\ntest1\n6\n0\n' | java -Dmc2.zombies=3 -Dmc2.screenshot=<png> -Dmc2.screenshot.frame=N -jar target/minecraft2-0.1.0-SNAPSHOT.jar`
  - [evidencias/enemy-ai-chase.png](evidencias/enemy-ai-chase.png) (frame 60): HUD «Zombis: 3», cajas verdes acercándose.
  - [evidencias/enemy-ai-death.png](evidencias/enemy-ai-death.png) (frame 240): «MORISTE» por ataque, jugador sigue en y=33.

## Limitaciones y pendientes

- Sin línea de visión: un zombi detecta a través de paredes (persigue rodeándolas).
- Sin gravedad continua: el zombi se apoya en el nodo; una caída mayor de 3 bloques se trata como no transitable.
- El respawn del jugador usa el último checkpoint en suelo, que puede estar junto al zombi; tras
  morir, el zombi vuelve a esperar el cooldown completo.
- Pendientes de verificación manual con ratón: golpe con clic izquierdo, explosión visual, R y J/K con
  zombis activos. Texturas de enemigos ON/OFF y dificultad pertenecen a Estudiante 3.
