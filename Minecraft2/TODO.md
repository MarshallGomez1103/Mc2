# TODO — Minecraft 2

Checklist único: `[ ]` pendiente, `[x]` terminada con evidencia.
Mapa maestro y ownership: [ROADMAP_CORTE_2.md](ROADMAP_CORTE_2.md).

# CORTE 1 — TERMINADO

Se conserva el historial del MVP. Huecos de automatización pasan a calidad del Corte 2,
sin reabrir el alcance funcional del Corte 1.

## Decisiones compartidas antes de programar

- [x] Completar los nombres de los tres integrantes en `README.md`.
- [x] Acordar ancho, alto y profundidad de cada chunk: `16 × 64 × 16`.
- [x] Definir cómo una posición del mundo se convierte en coordenada de chunk.
- [x] Acordar el esquema JSON definitivo para mundo, chunks y bloques.
- [x] Elegir la tecnología gráfica mínima compatible con Java e IntelliJ: LibGDX + LWJGL3.
- [x] Establecer criterios de terminado y casos de prueba del MVP.

Las decisiones, límites, esquema y pruebas acordados están en [docs/decisiones-compartidas.md](docs/decisiones-compartidas.md).

## Estudiante 1 — mundo y generación

- [x] Implementar la generación pseudoaleatoria sencilla.
- [x] Crear uno o pocos chunks usando `BlockFactory`.
- [x] Distribuir césped, tierra, piedra, arena, grava, madera y hojas.
- [x] Definir límites y validaciones de `Chunk`.
- [x] Resolver el chunk correspondiente a una `Position`.
- [x] Probar creación, consulta, colocación y eliminación de bloques.

## Estudiante 2 — jugador e interacción

- [x] Definir orientación y estado mínimo adicional del jugador.
- [x] Implementar adelante, atrás, izquierda y derecha.
- [x] Implementar salto y gravedad.
- [x] Añadir colisiones básicas con bloques sólidos.
- [x] Implementar selección de bloque mediante clic (raycast en `PlayerInteractionService`, ya conectado al clic real del ratón desde `presentation.game.GameInput`).
- [x] Conectar colocar y eliminar con `World.placeBlock` y `World.removeBlock`.
- [x] Suscribir un componente visual a `BlockChange`: `VoxelGame` es observador y reconstruye la malla del chunk afectado al colocar o eliminar un bloque.

## Estudiante 3 — presentación y persistencia

- [x] Diseñar la interfaz visual del menú principal sin lógica de negocio (`MainMenu` enruta y `ConsoleIO` concentra la entrada/salida; el menú gráfico con LibGDX corresponde a la integración del equipo).
- [x] Conectar las pantallas de crear, listar, cargar, guardar y eliminar.
- [x] Completar la escritura JSON de chunks y bloques en `JsonWorldStorage` (`WorldJsonCodec.write`).
- [x] Completar la reconstrucción del mundo desde JSON (`WorldJsonCodec.read`, con `JsonParser` propio).
- [x] Manejar archivos dañados, nombres duplicados y errores de lectura/escritura (`InvalidWorldFileException`, escritura atómica; CT-09 cubre 26 documentos inválidos en `persistence.InvalidWorldFileTest`).
- [x] Añadir confirmación antes de eliminar un mundo (solo una respuesta afirmativa explícita borra).
- [x] Probar el ciclo crear → guardar → cerrar → cargar → eliminar (`application.WorldLifecycleTest` y [docs/evidencias/ct-11-flujo-mvp.md](docs/evidencias/ct-11-flujo-mvp.md)).

## Integración del equipo

- [x] Renderizar uno o pocos chunks (`presentation.game`, cuadrícula 2×2 con descarte de caras ocultas: se dibuja el 6,6 % de la geometría). Evidencia en [docs/evidencias/ct-10-render.md](docs/evidencias/ct-10-render.md).
- [x] Asignar una representación visual a los ocho tipos de bloque (`BlockAppearance`, con sombreado por orientación de cara; el aire no se dibuja).
- [x] Conectar teclado y ratón con los casos de uso correspondientes (`GameInput`: WASD, salto, mirar, y clic para colocar y eliminar).
- [x] Separar archivos de pantallas: CRUD pasa por WorldApplicationService.
  Aclaración: MainMenu sí importa World; la vista consulta dominio y GameInput coordina física.
- [x] Verificar que `WorldManager` siga siendo el único Singleton: es la única clase con instancia global; los demás constructores privados son de clases de utilidad estáticas.
- [x] Verificar que no se hayan introducido patrones no permitidos: las únicas menciones a DAO, Command o Strategy son comentarios que explican por qué **no** se usan.
- [x] Revisar dirección entre capas: dominio no importa capas superiores.
  Existe ciclo interno domain.world ↔ domain.player; no se afirma ausencia total de ciclos.
- [ ] Histórico trasladado a Corte 2: ampliar JUnit de dominio/patrones.
  Persistencia/flujo ya automatizados; manualtest no cubre exhaustivamente CT-02 a CT-07.
- [ ] Actualizar README y documentación después de cada decisión de diseño.

## Restricciones históricas del Corte 1

Contexto histórico: en Corte 2 sí se autorizan enemigos, FSM y A*.
Se mantienen sin red, base de datos, nuevos singletons o frameworks innecesarios.

- [ ] No implementar multijugador, red, crafting, enemigos o inventario avanzado.
- [ ] No añadir base de datos.
- [ ] No introducir DAO, Strategy, Builder, Prototype, CQRS ni Event Sourcing.
- [ ] No convertir nuevas clases en Singleton.
- [ ] No agregar frameworks sin una necesidad acordada por el equipo.

# CORTE 2 — ACTUAL

Preparación para revisión; ninguna funcionalidad nueva implementada.
Baseline previo: mvn clean test, 41 exitosas, 0 fallos/errores/ignoradas.

## Preparación común — C2-00

- [x] Verificar Git, actualizar main y registrar baseline.
- [x] Crear roadmap, ownership y documentos iniciales sin resultados inventados.
- [x] Crear vocabulario mínimo ZombieState, BiomeType y Difficulty.
- [ ] Revisión humana y autorización del commit base; sin commit/push/PR en esta fase.
- [ ] Crear ramas después del commit base aprobado.

## Estudiante 1 — Generación procedural

- [x] BiomeType: PLAINS, DESERT, MOUNTAINS; solo vocabulario.
- [ ] C2-02: BiomeResolver regional determinista por seed + coordenadas.
- [ ] Acordar escala de regiones demostrable dentro del mundo finito.
- [ ] C2-06: PLAINS suave, GRASS/DIRT/STONE y árboles ocasionales.
- [ ] C2-06: DESERT plano relativo, SAND predominante y sin árboles.
- [ ] C2-06: MOUNTAINS con altura espacialmente coherente y límites válidos.
- [ ] C2-06: VILLAGE/estructuras sencillas; separar ubicación y plantilla.
- [ ] Determinismo por seed, negativos y orden independiente de generación.
- [ ] Tests unitarios de generación, capas y límites.
- [ ] Tests de integración entre chunks vecinos y límites de estructuras.
- [ ] Documentación de generación en docs/corte2/terrain-generation.md.

## Estudiante 2 — Enemy AI

FSM = decide qué hacer. A* = decide por dónde desplazarse. Son conceptos diferentes.

- [x] ZombieState: IDLE, CHASE, ATTACK, DEAD; solo vocabulario.
- [ ] Zombie sin heredar de Player, sin LibGDX y como estado de sesión.
- [ ] Acordar API pura de ZombieStateMachine antes de escribir comportamiento.
- [ ] C2-03 TDD RED: pruebas primero y evidencia de fallo real pertinente.
- [ ] TDD GREEN: mínimo comportamiento y evidencia verde.
- [ ] TDD REFACTOR: simplificar conservando suite verde y registrar diff.
- [ ] NavigationNode: X/Z + groundY, significado acordado.
- [ ] NavigationGrid: World actual, apoyo, cuerpo, desnivel y chunk existente.
- [ ] C2-05 AStarPathfinder: open/closed, g/h/f, parent y reconstrucción.
- [ ] Path y movimiento siguiendo waypoint sin atravesar obstáculos.
- [ ] Detección del jugador e IDLE.
- [ ] CHASE con ruta A*, no búsqueda por frame.
- [ ] ATTACK con alcance, cooldown y efecto mínimo acordados.
- [ ] DEAD por salud <= 0 y retirada según regla acordada.
- [ ] Repath por objetivo movido, ruta terminada/inválida o intervalo; evitar busy retry.
- [ ] C2-07 integración EnemyUpdateService: FSM, percepción, navegación y movimiento.
- [ ] C2-08 integración visual mínima y VoxelGame en ventana coordinada.
- [ ] Tests unitarios FSM: transiciones, límites y prioridades.
- [ ] Tests unitarios A*: recto, obstáculo/rodeo, imposible, origen=destino y desnivel.
- [ ] Tests de integración Enemy + World con bloques modificados.
- [ ] Documentación IA y evidencia TDD en docs/corte2/.

## Estudiante 3 — Hordas, configuración y calidad

- [ ] C2-04 GameSettings: campos/defaults y política de sesión acordados.
- [ ] Enemigos ON/OFF: OFF impide spawn y actualización.
- [ ] Enemy textures ON/OFF independiente de texturas del mundo.
- [x] Difficulty: NORMAL y VERY_HARD como vocabulario, sin parámetros.
- [ ] NORMAL con parámetros centralizados y probados.
- [ ] VERY_HARD con mismos algoritmos y parámetros distintos probados.
- [ ] HordeManager separado de movimiento individual.
- [ ] Oleadas: inicio, cantidad, intervalo, final y siguiente oleada deterministas.
- [ ] Acordar creación/registro de zombies y colección de sesión.
- [ ] C2-08 configuración desde menú; MainMenu y GameWindow owned.
- [ ] Pruebas unitarias de HordeManager con tiempo explícito.
- [ ] Pruebas unitarias de Difficulty/configuración, no getters triviales.
- [ ] Huecos baratos Corte 1: límites Chunk, todos los tipos Factory y misma seed.
- [ ] C2-09 configuración PIT en pom.xml y scope pequeño de lógica pura.
- [ ] Mutation testing: generated/killed/survived/no coverage/score reales.
- [ ] Análisis de mutantes sobrevivientes y tests de comportamiento, sin maquillar scope.
- [ ] C2-10 load test harness reproducible, headless cuando aplique.
- [ ] BASELINE: referencia finita con NORMAL.
- [ ] PEAK: carga alta y repaths concurrentes medidos.
- [ ] STRESS: aumentar hasta degradación observada, sin prometer 100 enemigos.
- [ ] ENDURANCE/RESISTANCE: ciclo prolongado spawn/update/death/oleadas.
- [ ] Documentación de resultados, hardware/JVM, duración y limitaciones.

## Dependencias de integración

| Puede avanzar independientemente | Dependencia posterior |
| --- | --- |
| Estudiante 1: BiomeResolver | Acordar escala; luego terreno/estructuras y pruebas vecinas |
| Estudiante 2: FSM pura y TDD | Percepción/EnemyUpdateService; no requiere renderer |
| Estudiante 3: GameSettings y scheduling HordeManager | Parámetros y registro para spawn real |
| A* sobre fixtures | Consulta de caminabilidad acordada del World actual |
| EnemyUpdateService | Zombie + FSM + navegación + movimiento |
| HordeManager con spawn real | Creación/registro de zombies y lifecycle de colección |
| Rendering | Zombie con posición y actualización funcional |
| Load testing de IA | EnemyUpdateService y A* integrados e instrumentados |

No crear veinte interfaces vacías: acordar APIs pendientes antes del trabajo dependiente.
No editar archivos de otro owner sin aviso. POM solo Estudiante 3.

## Integración y cierre — C2-09 a C2-11

- [ ] Pruebas nuevas verdes; 41 casos existentes preservados.
- [ ] Verificación visual spawn/chase/obstáculos/attack/death/oleadas/opciones.
- [ ] Regresión Corte 1: CRUD, jugar, movimiento, clics, guardar/cargar y texturas.
- [ ] PIT con objetivo aproximado 80% en scope justificado, no global.
- [ ] Cuatro escenarios medidos; no optimizar antes de medir.
- [ ] README y docs/uml.md actualizados solo en integración final.
- [ ] Quality gate aprobado antes de declarar Corte 2 terminado.

Decisiones fijas: zombies no persistidos; JSON sin cambios para enemigos; FSM + A*
sin ML/LLM/Behavior Tree; 2.5D desde World actual; sin streaming.
Documentación de pruebas canónica: docs/corte2/testing/, sin duplicar docs/testing/.

# CORTE 3 — TODAVÍA NO DEFINIDO

Pendiente de definición.
