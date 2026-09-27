# Integración Minecraft 2 — evidencia vigente 2026-09-26

> Evidencia de la primera integración. Los ajustes posteriores y su validación están en
> [feedback-20260926.md](feedback-20260926.md); ese documento y el README describen el estado actual.

## Alcance y estado

El usuario autorizó ejecutar el plan completo en una sesión con subagentes.
Los stops por bloque del adjunto se sustituyeron por esa autorización directa;
se conservaron respaldo, baseline, pruebas, revisión y el límite de no commit/push.
Tres subagentes tuvieron archivos separados: zombies; estamina/input;
menús/ventana/GPU. El integrador fue owner de VoxelGame/sesión/HUD/persistencia/tests/docs.

**IMPLEMENTADO:** R1–R10. **PROBADO AUTOMÁTICAMENTE:** 238 pruebas JUnit y smoke
OpenGL programado. **PROBADO MANUALMENTE:** inspección de capturas del renderer/UI,
no playtest humano con ratón/teclado durante una sesión de diversión.
**PENDIENTE:** playtest humano prolongado, AMD físico y endurance gráfico prolongado.
El quality gate automático pasó; no se presenta como aceptación humana final del equipo.

## Preservación y Git

- HEAD inicial/final: `0e60f94f126c91e3afa7b39a4d4be96321d39f9d` (verificar head.txt).
- Rama main local, un commit por delante de origin/main antes de esta sesión.
- Había siete archivos modificados: README, TODO, arquitectura-corte2, enemy-ai,
  GameWindow, VoxelGame y ZombieRenderer. Se conservaron sus contenidos reales.
- Snapshot completo de fuentes/test/docs/config en `/home/marshall/Documents/Codex/2026-09-26/hola-muchacho-c-mo-vamos-te/work/return-point-20260926`.
- `local.patch`, `git-status.txt`, `head.txt` y manifiesto SHA256 de worlds en el respaldo.
- 14 archivos de mundos (aprox. 470 MiB) copiados y comprobados byte a byte mediante SHA256.
- Verificación posterior: los mismos 14 archivos originales permanecen idénticos.
- Jaimito.json incompleto se preservó; no se reparó ni se eliminó silenciosamente.
- No fetch/merge/commit/push/reset/clean de Git, ni historial reescrito. `mvn clean`
  se usó sólo para el directorio de build target como exige el plan.

## Baseline real y gate final

| Ejecución | Resultado |
| --- | --- |
| Baseline `mvn clean test` | 200 ejecutadas, 0 fallos/errores/ignoradas |
| Baseline package | BUILD SUCCESS; arranque OpenGL y captura |
| Primer gate integrado | 236/236 |
| Gate final `mvn clean test` | **238/238**, 0 fallos/errores/ignoradas |
| Package final | BUILD SUCCESS, JAR sombreado generado |
| Cinco harness existentes manualtest | Pasaron bloques, movimiento, física, colisión e interacción/patrones; son programas automáticos headless |
| Diff | `git diff --check` sin errores |
| Capas | Ningún import de LibGDX/application/presentation/persistence desde src/domain |

Se mantienen los 200 casos; las actualizaciones de fixtures existentes se explican:
DifficultyTest conserva contraste NORMAL/VERY_HARD ajustando distancia de 15 a 26
para nuevos rangos; EnemyUpdateServiceTest fija parámetros históricos para probar
su contrato original. No se redujeron assertions ni se borraron pruebas.

Entorno local: Linux x86_64, Intel Core i7-13650HX, 20 CPUs lógicas, RAM aprox.
47,776 MiB, OpenJDK Homebrew 26.0.2; compilación objetivo release17.
LibGDX1.12.1/LWJGL3/Surefire3.2.5 sin nuevas dependencias.

## Implementación y arquitectura

- ZombieRenderer/ZombieAtlas: cabeza, torso, brazos y piernas, UV por cara/parte,
  frente estable +Z, orientación/marcha por desplazamiento. Cuerpo lógico sin cambios.
- ZombieSeparation: AABB local determinista con validación de terreno, muertos no
  bloquean; spawn ocupado rechazado, horda a distancia planar >=2.5 del jugador.
- NavigationGrid comprueba techo en las dos columnas de un escalón; ZombieMovement
  recorre centros exactos por subpasos .1 para conservar velocidad real y evitar tunnelling.
- Ruta bloqueada espera cooldown en lugar de replanificar al siguiente frame.
- Ataque enemigo requiere segmento despejado; pared interpuesta impide matar al jugador.
- NORMAL4.5/VERY_HARD5.2 frente a caminar4.3/sprint7.095; detección24/28 cubre anillo.
  Caps normales6/12 conservados. El escenario20 es de QA, no nuevo cap permanente.
- Stamina pura100, consumo20/s, regeneración14/s, unlock20%; gasto sólo al movimiento
  horizontal aplicado. Quieto/pared no consumen. Bob<=.035/FOV<=4 grados.
- GameSession RUNNING/PAUSED/DEAD coordina vida/enemigos/hordas. Pausa omite input,
  física, stamina, FSM, A*, ataques y timers; Stage continúa activo.
- GameHud concentra barra/texto/overlays/proyección; VoxelGame se redujo respecto al
  loop monolítico previo y delega coordinación de runtime y presentación del HUD.
- GraphicalGame concentra pantallas y lifecycle, CRUD pasa siempre por WorldApplicationService.
- Opciones enemies/textura zombie son inmediatas. Dificultad/bloques son preferencias
  de próxima partida, con runtime activo separado y aviso visible.
- ESC abre pausa, F alterna fullscreen conservando ventana/cursor. Resize de cámara,
  HUD y Stage. F no interfiere con escribir nombres en TextField.
- Clic Continuar se difiere y omite un frame de input. Salida/eliminación se confirman;
  iniciar una vista fallida libera recursos parciales y permite volver al menú.
- Factory/WorldManager Singleton/Observer/FSM/A* y JSON1 se preservan.

## Smoke gráfico real

El harness `GameFlowSmoke.java` del paquete de entrega instala entradas programadas,
usa callbacks Scene2D reales y un contexto OpenGL real. No es un usuario humano.
Resultado final: `FLOW_SMOKE_SUCCESS`, proceso exit0, 224 frames, JAR final.

Comprobó:

- crear por UI/listar/jugar;
- sprint121frames: energía59.6667 y desplazamiento real sobre arena temporal;
- ESC pausa, cursor liberado y posición/estamina/horda congeladas;
- clic UI no golpea ni cambia bloques;
- opciones enemies/texturas inmediatas, dificultad/bloques pendientes;
- continuar recaptura cursor, F ida/vuelta;
- tamaños640×480 y1280×720, cámara y Stage concordantes;
- guardar/salir/fresh-service + shell/cargar conserva bloque WOOD;
- pérdida de foco pausa, cancelar salida y cancelar borrado conservan estado;
- confirmar borrado elimina únicamente el mundo temporal creado por el harness.

La arena plana es una preparación de QA en un mundo temporal; no demuestra por sí
sola el balance de estructuras/biomas. El reinicio de shell usa la misma JVM/contexto;
la persistencia entre JVM distintas se comprobó separadamente abajo.
Las primeras dos ejecuciones del harness fallaron por instrumentación (backend
reemplazaba el proxy de input por frame, y lista recreada cambiaba selección tras
Cancelar), se corrigió el harness y se repitió exitosamente. No se ocultaron fallos de producto.
Capturas revisadas: menú, partida, barra, pausa, opciones y resize; zombie frente,
espalda, lateral, superior, marcha y texturas OFF.

## Persistencia: reproducción y resultado

La lista vacía reportada no se reprodujo en el estado local: el JAR final lanzado
 desde /tmp lista los 14 mundos originales. Crear ya persiste estado inicial;
Guardar conserva modificaciones posteriores. La UI ahora muestra ruta absoluta y
errores de archivo sin convertirlos en una lista vacía.

- JUnit LocalWorldPersistenceTest: crear/listar, editar/guardar/nuevo servicio/cargar,
  archivo inválido visible y cargarlo conserva mundo actual, salir sin guardar.
- Probe real separado: JVM1 crea (classpath tipo IDE); JVM2 carga/edita/guarda usando
  JAR desde proyecto; JVM3 desde /tmp lista/carga y verifica WOOD + yaw17.
- Las pruebas de escritura usan mc2.worlds.dir apuntando a una carpeta temporal.
- Resolución de ruta sin override desde clases y JAR coincide con worlds del proyecto.
- WorldDirectoryTest cubre layouts IDE/JAR y override; no se abrió IntelliJ como UI.
- JSON versión1 sin cambios para zombies/stamina/settings; ningún autosave oculto.
- Archivo inválido Jaimito conserva sus bytes originales y aparece en la lista.

## GPU: prueba efectiva, no sólo hardware instalado

| Preferencia | GL_VENDOR | GL_RENDERER | GL_VERSION |
| --- | --- | --- | --- |
| AUTO | Intel | Mesa Intel(R) Graphics (RPL-S) | 4.6 Compatibility Mesa26.2.2 |
| DEDICATED | NVIDIA Corporation | NVIDIA GeForce RTX4060 Laptop GPU/PCIe/SSE2 | 4.6.0 NVIDIA615.71.09 |

Backend LWJGL3. Arranques independientes exit0 y captura real. La prueba dedicada
usó override -Dmc2.gpu=DEDICATED; no cambió drivers ni la preferencia persistida.
La preferencia guardada por UI se aplica al próximo inicio, antes del contexto;
NVIDIA usa PRIME, Mesa usa DRI_PRIME, fallbackAUTO si solicitud no soportada o
inicio hijo falla. Renderer efectivo siempre visible; AUTO no equivale a GPUOFF.
AMD no físicamente probado. Fuera de Linux multigpu se remite la selección al SO.
JDK26 emitió avisos LWJGL/Unsafe; las pruebas ejecutadas pasaron.

## PIT final: scope definido y conteos reales

PIT1.30.0/JUnit5plugin1.2.3, mutadores por defecto, 85 tests examinados.
Scope: siete clases históricas más Stamina y GameSession. No incluye presentación,
separación completa ni persistencia; no es coverage global del videojuego.

| Clase | Generated | Killed | Timeout | Survived | No coverage |
| --- | --- | --- | --- | --- | --- |
| application.GameSession | 25 | 15 | 0 | 4 | 6 |
| application.GameSettings | 14 | 14 | 0 | 0 | 0 |
| application.HordeManager | 85 | 71 | 1 | 13 | 0 |
| domain.enemy.AStarPathfinder | 31 | 23 | 0 | 7 | 1 |
| domain.enemy.Difficulty | 5 | 5 | 0 | 0 | 0 |
| domain.enemy.WaveRules | 22 | 22 | 0 | 0 | 0 |
| domain.enemy.ZombieStateMachine | 29 | 26 | 0 | 3 | 0 |
| domain.player.Stamina | 37 | 30 | 0 | 4 | 3 |
| domain.world.biome.BiomeResolver | 8 | 6 | 0 | 2 | 0 |
| TOTAL | 256 | 212 | 1 | 33 | 10 |

Score PIT=(212+1)/256=**83.20%**; sin timeout=82.81%.
273/287 líneas cubiertas en clases mutadas (95%); no confundirlo con mutation score.
Supervivientes/no coverage se conservan en CSV/HTML. Entre ellos hay decisiones de
secuencias de spawn, límites/costes A*, umbrales de biome y ramas de sesión/stamina.
No se etiquetan todos como equivalentes ni se promete cobertura exhaustiva.

```bash
mvn clean test
mvn org.pitest:pitest-maven:mutationCoverage -DtargetClasses=domain.enemy.ZombieStateMachine,domain.enemy.AStarPathfinder,domain.enemy.WaveRules,domain.enemy.Difficulty,domain.world.biome.BiomeResolver,application.HordeManager,application.GameSettings,domain.player.Stamina,application.GameSession
```

## Carga final nueva

Ejecuciones finales secuenciales, posteriores al código definitivo, sin smoke
gráfico/PIT simultáneo. Escritorio normal; no entorno dedicado de benchmark.
Hubo una ejecución preliminar STRESS con unos3s de smoke simultáneo; se repitieron
los cinco escenarios finales y se usan únicamente los CSV finales de212745–212818.

Métrica: wall-clock HordeManager.update + EnemyUpdateService.update, no FPS/render.
Seed20260925, paso1/60s, warmup10s simulados; heap tras GC, resolución1MiB.

| Escenario | Población | Duración | Media ms | p95 ms | Heap MiB | Errores |
| --- | --- | --- | --- | --- | --- | --- |
| BASELINE | 3 | 5 × 60 s simulados | 0.0039–0.0069 | 0.0024–0.0089 | 4.0000–4.0000 | 0 |
| PEAK | 25 | 3 × 60 s simulados | 0.1607–0.1819 | 0.1088–0.1667 | 72.0000–73.0000 | 0 |
| FUNCTIONAL20 | 20 | 1 × 60 s simulados | 0.0348 | 0.0571 | 4.0000 | 0 |

STRESS, 30s simulados por escalón:

| Zombies | Media ms | p95 ms | Máximo ms | A*/s simulado |
| --- | --- | --- | --- | --- |
| 10 | 0.0713 | 0.0981 | 6.2816 | 24.97 |
| 20 | 0.1590 | 0.3093 | 15.9049 | 44.70 |
| 40 | 0.2422 | 0.2813 | 14.5644 | 84.20 |
| 80 | 0.3856 | 0.5945 | 27.3479 | 172.53 |
| 160 | 0.8634 | 0.9992 | 45.3659 | 357.03 |
| 320 | 2.7067 | 9.9833 | 100.4442 | 731.80 |
| 640 | 5.6444 | 19.4979 | 168.4152 | 1430.63 |

Degradación a640: p95=19.4979ms >16.67ms presupuesto60FPS.
No se garantiza ningún número de FPS ni render640. El p95 de20 en100chunks es
0.3093ms, y el escenario funcional20 mantiene20 activos en4chunks.

ENDURANCE: **1minuto real**, 61109s simulados, oleada1952,
23404 zombies generados, 11 activos al muestreo,
media0.0161ms/p950.0096ms/máximo10.5611ms,
heap72MiB, 3230 muertes del jugador, 0 errores.
Un único muestreo no prueba estabilidad prolongada, ausencia de fugas nativas/GPU
ni temperatura de una partida larga. Las métricas antiguas de Windows se conservan
como historia separada, no se comparan directamente con este hardware.

```bash
java -cp target/classes:target/test-classes loadtest.EnemyLoadHarness BASELINE
java -cp target/classes:target/test-classes loadtest.EnemyLoadHarness PEAK
java -cp target/classes:target/test-classes loadtest.EnemyLoadHarness STRESS
java -cp target/classes:target/test-classes loadtest.EnemyLoadHarness FUNCTIONAL20
java -cp target/classes:target/test-classes loadtest.EnemyLoadHarness ENDURANCE 1
```

## Artefactos y retorno

Logs, reportes Surefire/PIT, CSV y screenshots de esta sesión:
`/home/marshall/Documents/Codex/2026-09-26/hola-muchacho-c-mo-vamos-te/outputs/validation`.
El paquete de entrega incluye GameFlowSmoke.java portable y el informe de retorno.
El snapshot previo es el punto de retorno; contiene cambios locales anteriores,
no sólo HEAD. El script volver_al_estado_previo.py valida hashes antes de restaurar
fuente/docs/tests y puede recompilar. Los mundos originales se verificaron intactos;
el respaldo worlds se conserva por separado. Consultar instrucciones de la entrega.

## Pendientes humanos explícitos

- Sesión prolongada jugando: melee/clics reales, salto, R/J/K, borde, estructuras,
  hordas, ritmo de estamina y percepción del bob/FOV.
- Confirmar que el balance resulte divertido para el equipo; tests no deciden diversión.
- AMD físico y otros sistemas; prueba prolongada gráfica de recursos/temperatura.
- Revisión del diff y autorización de commit/push por el usuario/equipo.
