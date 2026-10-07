# Enemy AI — implementación del Corte 2

Estado 2026-09-26: IA/hordas integradas; modelo seis partes, separación, balance,
spawn seguro y golpe bloqueado por paredes implementados. Suite de la primera integración: 238/238; la etapa final registró 313.
Evidencia nueva: [upgrade zombie](testing/zombie-upgrade.md) e
[integración](testing/integration-20260926.md). El reporte base de Jasub se conserva
como historia: [REPORTE_ENEMY_AI_JASUB.md](REPORTE_ENEMY_AI_JASUB.md).

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
| ZombieRenderer | presentation.game | skin pixel-art opcional y efecto visual al morir |

## Decisiones cerradas (antes pendientes en arquitectura-corte2.md)

- **Cuerpo del zombi:** 0.6 × 1.8 × 0.6, igual que Player; ocupa dos bloques de aire.
- **groundY vs pies:** `groundY` es el bloque sólido de apoyo; los pies están en `groundY + 1`.
- **Vecinos:** 8 direcciones; diagonales exigen apoyo y espacio libre en ambas columnas laterales.
- **Desnivel:** subir máximo 1, bajar máximo 3 (`NavigationGrid.MAX_CLIMB/MAX_DROP`).
- **Chunk ausente:** nunca es caminable; origen o destino fuera de chunks → sin ruta.
- **Sin ruta:** `Optional.empty()`; el servicio espera `noRouteRetrySeconds` (1 s) antes de reintentar.
- **Invalidación por bloques:** no hay caché; la rejilla lee el World en cada consulta y
  `ZombieMovement.follow` devuelve `false` si un bloque nuevo cierra el paso → cooldown sin ruta antes de repath.
- **Repath:** sin ruta, ruta terminada/bloqueada, jugador a más de 1.5 bloques del destino planificado o
  intervalo de 1.5 s. Nunca un A* por zombi y frame (probado: 120 frames → 1–3 búsquedas).
- **Percepción:** distancia euclidiana 3D sin línea de visión (limitación documentada).
- **Ataque:** alcance 1.6, segmento despejado de terreno, primer contacto inmediato, después cada 0.6 s NORMAL / 0.4 s VERY_HARD. Cada golpe resta 25 de 100 puntos de vida.
- **Salud:** 3 golpes de jugador (`ZombieMeleeService.DAMAGE = 1`).
- **Muerte:** DEAD absorbente; despawn a los 0.8 s; el renderer hincha y colorea el modelo mientras tanto.
  No rompe bloques, no hay daño de área.
- **Registro integrado:** HordeManager crea zombis por EnemyUpdateService.spawnAt y
  consulta la colección de sesión para cerrar una oleada cuando ya no quedan.
- **Oleadas:** NORMAL empieza a los 8 s con 2 zombis; VERY_HARD, a los 4 s con 4.
  El HUD muestra la cuenta regresiva y el estado actual.
- **Spawn provisional eliminado:** ya no se generan zombis con Z ni con la propiedad
  mc2.zombies; el spawn de la partida viene del plan de oleadas.

## Parámetros por defecto (`ZombieParameters.defaults()`)

| Parámetro | Valor | Parámetro | Valor |
| --- | --- | --- | --- |
| detectionRange | 24 | moveSpeed | 4.3 b/s (jugador 4.3; sprint 7.095) |
| attackRange | 1.6 | maxHealth | 3 |
| loseTargetRange | infinito (persiste objetivo) | attackCooldownSeconds | 0.6 |
| repathIntervalSeconds | 1.5 | repathDistance | 1.5 |
| noRouteRetrySeconds | 1.0 | | |

## Ampliación de jugabilidad

- ZombieSeparation aplica correcciones deterministas validadas por ZombieMovement.
- Spawn ocupado es rechazado; muertos no bloquean; HordeManager evita spawn
  menor a 2.5 bloques del jugador incluso al proyectar al borde.
- NavigationGrid exige techo libre en ambos lados de un escalón: caso RED reproducido.
- ZombieMovement.consume distancia continua por frame: velocidad 4.3 efectiva probada.
- NORMAL detecta24/velocidad4.3; VERY_HARD detecta28/velocidad4.7; ambos conservan el objetivo detectado mientras viva.
- ZombieRenderer + ZombieAtlas: seis partes, UV por cara/parte, marcha y texturas inmediatas.
- Golpe enemigo consulta terreno; una pared alta bloquea daño y fuerza buscar camino
  alrededor. El intervalo entre golpes se conserva al salir y volver al alcance.
  La percepción por distancia sigue atravesando paredes.
- Pruebas de 20 zombies coincidentes/convergentes, spawn, corredor, pared y semillas.

## Evidencia histórica

- IA base de Jasub: 110 pruebas, 0 fallos en su revisión; las capturas
  [enemy-ai-chase.png](evidencias/enemy-ai-chase.png) y
  [enemy-ai-death.png](evidencias/enemy-ai-death.png) son de esa etapa.
- Integración local: `mvn clean package` → 200 pruebas, 0 fallos. Se hizo una
  comprobación visual de juego en VERY_HARD: aparecieron cuatro zombis, el HUD
  informó la oleada y su contador, y los zombis atacaron al jugador.
- La instrucción histórica con `-Dmc2.zombies` corresponde al spawn temporal
  de depuración y ya no funciona: las apariciones ahora siguen las reglas de
  oleadas seleccionadas en el menú.

## Limitaciones y pendientes

- Sin línea de visión: un zombi detecta a través de paredes (persigue rodeándolas).
- Sin gravedad continua: el zombi se apoya en el nodo; una caída mayor de 3 bloques se trata como no transitable.
- El respawn del jugador usa el último checkpoint en suelo, que puede estar junto al zombi; tras
  morir, el zombi vuelve a esperar el cooldown completo.
- Pendientes de verificación manual completa: golpe con clic izquierdo,
  explosión visual, R y J/K con zombis activos. Las texturas de enemigos
  ON/OFF, la dificultad y las oleadas ya están conectadas al juego.

## Combate y caídas actuales

ZombiePhysics usa Player.GRAVITY=-20 y Player.JUMP_SPEED=8, con colisión vertical
continua y 1 segundo entre saltos. ZombieMovement valida el cuerpo horizontal sin
subirlo de golpe. La caída se integra en subpasos; no atraviesa pisos ni usa cabezas
como plataformas. KamikazePolicy recibe RNG inyectable: una muestra por aterrizaje
profundo >=8,30%reptante/70%desarme. El reproductor del mundo inicializa la RNG de
la sesión desde su seed; las pruebas controlan ambos resultados y el límite0.30.

KamikazeDescent busca un borde abierto cerca del jugador enterrado: máximo6 intentos
con250expansiones de A* por aproximación. La ruta normal mantiene2000expansiones.
EnemyUpdateService conserva el borde escogido mientras lo recorre, bloquea la entrada
si el cuerpo anterior sigue cerca y separa entradas por0.65s. Una víctima10bloques
abajo no bloquea el borde para siempre. Un impacto sobre otra cabeza desarma al que
cae. Un zombi muerto desaparece después0.8s, sin dañar bloques del mundo.

Reptante: alto0.8, no salta, camina al55%, rejilla con1bloque de espacio libre. Golpe
kamikaze usa su causa propia. ZombieTargeting comparte entre puño y pistola el rayo
sobre la altura real; voxelDDA conservadora en esquinas bloquea terreno y chunks
ausentes. El efecto rojo dura0.18s; no comparte materiales de otro zombi.

## Ajuste final de entrega

La progresión actual es 4,8,16,32,64,128… en ambas dificultades; las apariciones fallidas se reintentan. Reptante al 28% de velocidad, 5 s de preparación al borde y entrada secuencial ≥1 s. `ZombieDebris` separa los 18 fragmentos persistentes de la IA y la pertenencia de oleadas. Detalle: [validación final](testing/final-horde-20260926.md). Las cantidades/límites de notas anteriores son históricos.
