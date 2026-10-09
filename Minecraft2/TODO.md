# Recuperación del Corte 2 — checklist activo

Plan y reparto por archivos: [plan del equipo](docs/recuperacion-c2/plan-equipo.md).
Meta interna: completar correcciones y solicitar revisión el 8 de octubre de 2026.

Esta sección registra la recuperación. Las secciones que siguen conservan el historial anterior. Marcar `[x]` solo después de revisar implementación, pruebas y evidencia real.

## Base común

- [x] Estilo implementado y reglas/contrato comprobados; revisión conjunta del equipo pendiente.
- [ ] Actualizar cada clon a su rama de recuperación publicada y conservar cambios locales.
- [x] Aportes identificados e integrados en orden: Thomas, jasub/final-a y Ethian.

## Thomas — arquitectura e integración

- [x] REC-T1: trasladar coordinación de movimiento/física/estamina a aplicación sin cambiar reglas.
- [x] REC-T2: conectar GameInput/GameSession y adaptar VoxelGame, HUD, cámara y respawn.
- [x] REC-T3: conservar regresiones y probar pausa, muerte, energía, tiempos inválidos y límites.
- [x] REC-T4: configurar JaCoCo y clasificar tests heredados/nuevos; separar unitarias e integración sin omitir ni duplicar pruebas.
- [x] REC-T5: ejecutar empaquetado e integración con `mvn verify`, conservando PIT.
- [ ] REC-T6: integrar Jasub y Ethian; tabla del README raíz, arquitectura con siete apartados, reportes publicados y ejecución limpia.

Evidencia de REC-T1–T5: [reporte actual](docs/pruebas.md), [índice](docs/recuperacion-c2/correcciones.md) y arnés gráfico de controles. La integración, tabla/arquitectura, flujo público y comprobación desde copia limpia están disponibles. REC-T6 permanece abierto únicamente para incorporar el resultado/análisis de carga y cerrar la revisión del candidato final. No se da por terminada la recuperación conjunta.

## Jasub — pruebas y flujo público

- [x] REC-J1: clasificar pruebas existentes y añadir casos propios con AAA, equivalencia, límites y dobles cuando apliquen.
- [x] REC-J2: sesión con componentes reales comprobada por GameSessionTest y CombatSessionRegressionTest; no se duplican casos para crear otro nombre de clase.
- [x] REC-J3: persistencia temporal/aislamiento comprobados por LocalWorldPersistenceIT, WorldLifecycleIT y SaveEdgeCasesIT.
- [x] REC-J4: GameFlowSmokeIT versionado y ejecutado mediante consola pública del JAR.
- [x] REC-J5: segundo escenario público de cancelación/eliminación o archivo inválido.
- [x] REC-J6: comprobación reproducible de fronteras arquitectónicas.
- [x] REC-J7: reporte con comandos, evidencia, resultados y límites de automatización.

Evidencia de integración: [reporte](docs/recuperacion-c2/integracion.md) e [inventario/casos](docs/pruebas.md). La ampliación de caja negra, fronteras, stub de error y regresiones de métricas se completó durante la integración; no se atribuye a Jasub todo el trabajo del candidato.

## Ethian — estilo, modelado y carga

- [x] REC-E1: comparación de al menos dos estilos y ADR con ganancias/costes.
- [x] REC-E2: diagramas de contexto, contenedor y componentes coherentes con código.
- [x] REC-E3: arquitectura inicial y recuperada con referencias de versión reales.
- [x] REC-E4: scripts reproducibles de baseline y estrés en perf/.
- [x] REC-E5: throughput real, tasa de errores y métricas headless bien definidas.
- [x] REC-E6: SLO definido antes de medir y población objetivo declarada.
- [ ] REC-E7: corridas acotadas autorizadas, comparables y con parada definida.
- [ ] REC-E8: CSV, consola y manifiesto revisados/versionados en perf/results/, tras medir el mismo candidato integrado.
- [ ] REC-E9: análisis de cuello de botella, relación con arquitectura y límites del harness.

Solo quedan REC-E7–E9 de carga y el cierre del candidato. La revisión conjunta del ADR/diagramas y la actualización de cada clon siguen pendientes: no se registran acciones humanas como realizadas por ejecutar Maven.

## Cierre del equipo

- [ ] Revisar cada observación de la retroalimentación y cada requisito obligatorio de la rúbrica.
- [x] `mvn clean verify` aprobado; resultados actuales separados por nivel y cobertura de dominio.
- [x] Flujo público reproducible y revisión gráfica real de controles afectados.
- [x] ADR y diagramas corresponden al candidato final integrado.
- [x] Tabla obligatoria en README raíz e índice directo de correcciones para revisión.
- [x] Archivos, evidencias e índice del candidato integrado revisados antes del commit; repetir al incorporar la carga.
- [ ] Publicación aprobada y verificada; solicitud de revisión enviada con enlaces de versión.

---

> Registro histórico del trabajo del Corte 2, cerrado. Las tareas y conteos de cada etapa no describen pendientes del Corte 3.

# TODO — Minecraft 2

Checklist único: `[ ]` pendiente, `[x]` terminada con evidencia.
Mapa maestro y ownership: [ROADMAP_CORTE_2.md](ROADMAP_CORTE_2.md).

## Integración de jugabilidad — 2026-09-26

- [x] R1: modelo zombie de seis partes, UV y textura ON/OFF; capturas OpenGL.
- [x] R2: estamina pura, sprint, cámara y barra; pruebas unitarias y smoke gráfico.
- [x] R3: GameSession y GameHud extraídos; UI, reglas y render separados.
- [x] R4: F fullscreen/ventana; probado junto a resize 640×480 y 1280×720.
- [x] R5/R6: renderer efectivo y preferencia próximo arranque; Intel y NVIDIA probadas.
- [x] R7: crear/listar/guardar/reiniciar/cargar y archivos inválidos; JSON compatible.
- [x] R8: menú gráfico, ESC pausa, opciones y confirmaciones de salida/eliminación.
- [x] R9: separación física local; spawn ocupado, pared/corredor y 20 zombies probados.
- [x] R10: velocidad real mayor que caminar y menor que sprint; spawn seguro y detección.
- [x] Suite de la primera integración: 238/238; build, PIT seleccionado y cinco escenarios de carga.
- [x] Preservación: diff previo, copia fuente y 14 mundos con SHA256.
- [ ] Playtest humano prolongado: diversión, hordas, bob, melee y refugio en estructuras.
- [ ] Prueba física AMD y sistemas Windows/macOS; endurance gráfico prolongado.

Evidencia y límites: [integración](docs/corte2/testing/integration-20260926.md).
Las secciones siguientes conservan el historial de trabajo por estudiante;
sus conteos y opciones de consola son históricos si contradicen esta sección.

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

# CORTE 2 — CERRADO

Preparación aprobada; frentes del Corte 2 en desarrollo. Estado de cada tarea abajo.
Baseline previo: mvn clean test, 41 exitosas, 0 fallos/errores/ignoradas.

## Preparación común — C2-00

- [x] Verificar Git, actualizar main y registrar baseline.
- [x] Crear roadmap, ownership y documentos iniciales sin resultados inventados.
- [x] Crear vocabulario mínimo ZombieState, BiomeType y Difficulty.
- [x] Revisión humana y aprobación de la base de Corte 2.
- [x] Crear ramas de trabajo individuales desde la base aprobada.

## Estudiante 1 — Generación procedural

- [x] BiomeType: PLAINS, DESERT, MOUNTAINS; solo vocabulario.
- [x] C2-02: BiomeResolver regional determinista por seed + coordenadas.
- [x] Escala regional 18: demostrable con seed 17 en el mundo finito 2×2; no garantizada para toda seed.
- [x] C2-06: PLAINS suave, GRASS/DIRT/STONE y árboles ocasionales.
- [x] C2-06: DESERT plano relativo, SAND predominante y sin árboles.
- [x] C2-06: MOUNTAINS con altura espacialmente coherente y límites válidos.
- [x] C2-06: VILLAGE/estructura sencilla; ubicación y plantilla separadas, contenida en chunk.
- [x] Determinismo por seed, negativos y orden independiente de generación.
- [x] Tests unitarios de generación, capas y límites.
- [x] Tests de integración entre chunks vecinos, límites de estructura y JSON existente.
- [x] Documentación de generación en docs/corte2/terrain-generation.md.
- [x] Revisión visual de biomas/relieve y spawn en mundos medianos/grandes.
- [x] Captura visual del overlay MORISTE con instancia temporal bajo el vacío.
- [x] Revisión visual de estructura con seed 17 y cámara temporal frente a la casa.
- [x] Añadir huecos de ventanas a la casa y probar la plantilla.
- [x] Shift para correr con prueba de velocidad y controles documentados.
- [x] Impedir colocar un bloque dentro del jugador; recuperar posiciones incrustadas.
- [x] Invalidar mallas vecinas al editar un bloque en borde de chunk.
- [x] Listar mundos desde ruta estable aunque el JAR se ejecute fuera del proyecto.
- [ ] Verificación interactiva de J/K/R con teclado real.

## Extensión coordinada — tamaños finitos, FPS y muerte por vacío

- [x] Elegir Pequeño 2×2, Mediano 10×10 o Grande 16×16 al crear mundo.
- [x] Indexar chunks en World y limitar búsqueda de spawn al centro.
- [x] Crear/liberar mallas cercanas con radio J/K y mostrar FPS en HUD.
- [x] Muerte de sesión por caída y entrada reutilizable para futura muerte por enemigo.
- [x] Tests de tamaños, índice, radio y vida; último gate 63/63.
- [x] Crear, guardar, cargar y abrir Grande con heap de 2 GB; límite de 1 GB documentado.
- [ ] Verificación manual de J/K, caída real y R en una sesión interactiva del equipo.

Detalle técnico y mediciones: [world-size-render-and-life.md](docs/corte2/world-size-render-and-life.md).

## Estudiante 2 — Enemy AI

FSM = decide qué hacer. A* = decide por dónde desplazarse. Son conceptos diferentes.
Estado: implementado y probado headless (110/110); smoke manual con ratón pendiente.
Detalle: [docs/corte2/enemy-ai.md](docs/corte2/enemy-ai.md), reporte en
[docs/corte2/REPORTE_ENEMY_AI_JASUB.md](docs/corte2/REPORTE_ENEMY_AI_JASUB.md).

- [x] ZombieState: IDLE, CHASE, ATTACK, DEAD; solo vocabulario.
- [x] Zombie sin heredar de Player, sin LibGDX y como estado de sesión (`domain.enemy.Zombie`; `ZombieTest` verifica ambas restricciones).
- [x] Acordar API pura de ZombieStateMachine antes de escribir comportamiento: `next(ZombieState, ZombiePerception)`; reglas fijadas por escrito en `docs/corte2/testing/tdd-zombie-fsm.md` antes de RED.
- [x] C2-03 TDD RED: pruebas primero y evidencia de fallo real pertinente (13 ejecutadas, 8 fallos de aserción con stub sin transiciones).
- [x] TDD GREEN: mínimo comportamiento y evidencia verde (13/13).
- [x] TDD REFACTOR: simplificar conservando suite verde y registrar diff (ternarios → `fromIdle/fromChase/fromAttack`).
- [x] NavigationNode: X/Z + groundY, significado acordado (`groundY` = bloque de apoyo; pies en `groundY + 1`).
- [x] NavigationGrid: World actual, apoyo, cuerpo, desnivel y chunk existente (sin caché; subir 1, bajar 3; vecinos 4).
- [x] C2-05 AStarPathfinder: open/closed, g/h/f, parent y reconstrucción (presupuesto 2 000 expansiones; sin ruta = `Optional.empty()`).
- [x] Path y movimiento siguiendo waypoint sin atravesar obstáculos (`ZombieMovement` con auto-step; bloqueo → repath).
- [x] Detección del jugador e IDLE (distancia 3D, 12 bloques; sin línea de visión, documentado).
- [x] CHASE con ruta A*, no búsqueda por frame (probado: 120 frames → 1–3 búsquedas).
- [x] ATTACK con alcance, cooldown y efecto mínimo acordados (1.6 bloques, 1 s incluido el primer golpe, `PlayerLife.die(ENEMY)`).
- [x] DEAD por salud <= 0 y retirada según regla acordada (despawn a 0.8 s; la colección no crece tras 10×20 muertes).
- [x] Repath por objetivo movido, ruta terminada/inválida o intervalo; evitar busy retry (1.5 bloques / 1.5 s / espera 1 s sin ruta).
- [x] C2-07 integración EnemyUpdateService: FSM, percepción, navegación y movimiento (`application.EnemyUpdateService`; ON/OFF y registro para HordeManager).
- [x] C2-08 integración visual mínima y VoxelGame en ventana coordinada (`ZombieRenderer`; capturas en `docs/corte2/evidencias/`; avisar al equipo del cambio en el hotspot).
- [x] Clic izquierdo sobre zombi en alcance: golpe cuerpo a cuerpo antes de borrar bloque; si no hay zombi, conservar eliminación de bloques (`ZombieMeleeService`, ambos caminos probados en JUnit; clic real pendiente de smoke manual).
- [ ] Muerte de zombi: efecto visual breve de «explosión» sin romper terreno ni producir daño de área; retirar recursos visuales al despawn. Implementado en `ZombieRenderer` (solo visual; instancias liberadas con `retainAll`); falta verlo en pantalla con un golpe real.
- [x] Ataque letal del zombi: conectar con `PlayerLife.die(ENEMY)` sin persistir vida de sesión (captura `enemy-ai-death.png`).
- [x] Tests unitarios FSM: transiciones, límites y prioridades (13).
- [x] Tests unitarios A*: recto, obstáculo/rodeo, imposible, origen=destino y desnivel (8).
- [x] Tests de integración Enemy + World con bloques modificados (`EnemyUpdateServiceTest`: pared construida durante la persecución, jugador encerrado).
- [x] Documentación IA y evidencia TDD en docs/corte2/ (`enemy-ai.md`, `testing/tdd-zombie-fsm.md`).
- [ ] Smoke manual con ratón: golpe, explosión, picar sin zombi, R y J/K durante oleadas. La integración retiró el spawn de prueba por Z y mc2.zombies.

## Estudiante 3 — Hordas, configuración y calidad

- [x] C2-04 GameSettings: enemigos ON, texturas de enemigos ON y NORMAL por defecto; solo sesión, sin persistir (`application.GameSettings`, `GameSettingsTest`).
- [x] Enemigos ON/OFF: OFF impide spawn y actualización. Conectado desde GameSettings al loop de VoxelGame y HordeManager.
- [x] Enemy textures ON/OFF independiente de texturas del mundo; GameSettings llega a VoxelGame y ZombieRenderer aplica ZombieSkin o color plano.
- [x] Difficulty: NORMAL y VERY_HARD como vocabulario, sin parámetros.
- [x] NORMAL con parámetros centralizados y probados (`Difficulty` = `ZombieParameters.defaults()` + `WaveRules`).
- [x] VERY_HARD con mismos algoritmos y parámetros distintos probados (`DifficultyTest`: detecta, corre, mata y aguanta más).
- [x] HordeManager separado de movimiento individual (`application.HordeManager`; solo usa `spawnAt`/`zombies`/`isEnabled`).
- [x] Oleadas: inicio, cantidad, intervalo, final y siguiente oleada deterministas (seed del mundo, tiempo explícito).
- [x] Integración local de HordeManager con spawnAt, zombies e isEnabled de EnemyUpdateService; los zombis siguen en estado de sesión.
- [x] C2-08 configuración desde menú: opción 8 con submenú de enemigos, texturas y dificultad (`MainMenuSettingsTest`).
- [x] Pruebas unitarias de HordeManager con tiempo explícito (`HordeManagerTest`, 16 casos).
- [x] Pruebas unitarias de Difficulty/configuración, no getters triviales (`DifficultyTest`, `WaveRulesTest`, `GameSettingsTest`).
- [x] Huecos baratos Corte 1: límites Chunk, todos los tipos Factory y misma seed (`ChunkBoundsTest`, `BlockFactoryTest`, `SameSeedTest`).
- [x] C2-09 configuración PIT en pom.xml y scope pequeño de lógica pura (PIT 1.30.0, fuera del gate).
- [x] Mutation testing: 172 generated, 153 killed, 1 timed out, 17 survived, 1 no coverage; score 89.5 % ([mutation-testing.md](docs/corte2/testing/mutation-testing.md)).
- [x] Análisis de mutantes sobrevivientes y tests de comportamiento, sin maquillar scope (81 % → 89.5 %; supervivientes ajenos reportados).
- [x] C2-10 load test harness reproducible, headless (`test/loadtest/EnemyLoadHarness.java`).
- [x] BASELINE: referencia finita con NORMAL (3 zombis, p95 < 0.05 ms).
- [x] PEAK: carga alta y repaths concurrentes medidos (25 zombis, p95 3.7 ms, 268 A*/s).
- [x] STRESS: aumentar hasta degradación observada, sin prometer 100 enemigos (p95 > 16.7 ms con 320).
- [x] ENDURANCE/RESISTANCE: ciclo prolongado spawn/update/death/oleadas (30 min, 4 096 oleadas, heap estable, 0 errores).
- [x] Documentación de resultados, hardware/JVM, duración y limitaciones ([load-testing.md](docs/corte2/testing/load-testing.md)).

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
