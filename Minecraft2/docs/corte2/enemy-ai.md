# Enemy AI — preparación

Estado: planificado; solo ZombieState y Difficulty existen como vocabulario.
No hay Zombie, FSM, A*, EnemyUpdateService o rendering de enemigos funcionales.
Plan secuenciado para el agente de Jasub: [PLAN_AGENTE_JASUB.md](PLAN_AGENTE_JASUB.md).

## Alcance y separación

FSM = decide qué hacer. A* = decide por dónde desplazarse.
Zombie conserva estado lógico sin Player como superclase, teclado, cámara, renderer
ni persistencia. Los enemigos no se guardarán en JSON: son estado de sesión.

## FSM y TDD

Estados acordados: IDLE, CHASE, ATTACK, DEAD. SPAWN no será estado sin conducta real.
Transiciones conceptuales: detección → CHASE; alcance → ATTACK; salir de alcance →
CHASE; pérdida de objetivo → IDLE; health <= 0 → DEAD; DEAD es absorbente.
Prioridades, límites inclusivos y pérdida del objetivo se fijarán antes de RED.
No implementar ni evaluar transiciones desde LibGDX.
Evidencia: [TDD](testing/tdd-zombie-fsm.md).

## A* 2.5D

Nodo conceptual: worldX, groundY, worldZ. Lectura del World actual:
superficie sólida, espacio para cuerpo, desnivel admisible y chunk existente.
No depender únicamente de SimpleTerrainGenerator.surfaceHeightAt.
Origen/destino sin apoyo o chunk ausente no son rutas caminables.

Implementación futura: open/closed set, g, h, f, parent y reconstrucción.
Heurística coherente con vecinos/costes elegidos. Nada de búsqueda voxel 3D completa.
Casos: recto, obstáculo, rodear pared, destino imposible, origen=destino y desnivel inválido.
Un path no sustituye colisiones ni seguimiento físico.

## Repath

Recalcular por desplazamiento suficiente del objetivo, ruta terminada, invalidación
o intervalo explícito. No por enemigo × frame. Definir comportamiento sin ruta
para evitar reintento continuo. Instrumentar conteo y tiempo A* para carga.

## Conexiones y responsabilidades

Estudiante 2: FSM, navegación, Zombie, EnemyUpdateService, integración visual final.
Estudiante 3: settings/dificultad y hordas; acordar registro y lifecycle antes de conectar.
World ofrece getPlayer, findChunk y getBlock; colisión actual es especializada en Player.
BlockChange puede informar invalidación; Chunk.addBlock no emite ese evento.
No modificar VoxelGame antes de FSM/A* verdes y ventana de integración acordada.
`World` ya indexa chunks; puede contener 4, 100 o 256 chunks. `VoxelGame` mantiene
mallas cercanas, radio J/K y FPS. Preservar este ciclo y liberar recursos de enemigos.
`PlayerLife.die(ENEMY)` muestra «MORISTE» y permite reaparición con R.

El clic izquierdo ya elimina bloques. Al añadir melee, primero elegir el objetivo
visible más cercano dentro de alcance: si es un zombi, golpear; si no, conservar
la eliminación de bloques. La muerte del zombi tendrá una explosión **visual**
breve, sin explosión física, daño de área ni modificación de terreno.

## TODO y evidencias

- [ ] Reglas de percepción, daño/cooldown, muerte y actualización temporal.
- [ ] Contrato de caminabilidad y movimiento.
- [ ] Tests unitarios e integración World actual + rutas modificadas.
- [ ] Render simple con texturas ON/OFF; sin assets complejos obligatorios.
- [ ] Demostrar spawn, chase, rodeo, attack, death, oleadas y ambas dificultades.
- [ ] Registrar comandos/resultados y limitaciones reales. No hay resultados nuevos aún.
