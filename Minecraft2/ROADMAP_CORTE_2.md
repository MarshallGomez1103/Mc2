# Roadmap maestro — Corte 2

Estado: preparación para revisión. No se han implementado las funcionalidades del Corte 2.
Horizonte orientativo: dos semanas desde la aprobación del equipo; no es una promesa de fechas.

## Estado de los cortes

- **CORTE 1 — TERMINADO:** MVP de consola + ventana voxel, CRUD JSON, cuatro chunks,
  generación inicial, jugador, cámara, movimiento, gravedad, salto, colisiones,
  interacción, Factory, Singleton, Observer y texturas de bloques ON/OFF.
- **CORTE 2 — ACTUAL:** preparación y desarrollo incremental de los tres retos siguientes.
- **CORTE 3 — TODAVÍA NO DEFINIDO:** pendiente de definición. No se diseña en este roadmap.

## Objetivo del Corte 2

1. Generación procedural mejorada: regiones PLAINS, DESERT y MOUNTAINS, más
   VILLAGE/estructura sencilla y determinista.
2. Enemy AI: Zombie sin heredar de Player, FSM + A*, repath, hordas, NORMAL/VERY_HARD,
   enemigos ON/OFF y texturas de enemigos ON/OFF.
3. Calidad: pruebas unitarias e integración, TDD específicamente en ZombieStateMachine,
   mutation testing con objetivo aproximado de 80% en scope seleccionado y mediciones
   BASELINE, PEAK, STRESS y ENDURANCE/RESISTANCE.

Fuera del Corte 2: multiplayer, agente LLM, survival completo, crafting, inventario
complejo, mundo infinito/streaming, ECS, behavior trees, machine learning, sistemas
distribuidos y dependencias empresariales. FOREST es opcional y no bloquea aceptación.
Una aldea es una estructura, no un valor de BiomeType.

## Baseline y límites de esta preparación

Base: main, commit 5501b02e183cf81950430eba11828908009d1caf.
Git estaba limpio; git pull --ff-only indicó Already up to date.
El 16 de septiembre de 2026, mvn clean test ejecutó 41 pruebas:
41 exitosas, 0 fallos, 0 errores, 0 ignoradas.
Entorno: Linux amd64, OpenJDK 26.0.2, Maven 3.9.16; compilación release 17.

Esta preparación añade documentación y tres enums sin comportamiento:
ZombieState, BiomeType y Difficulty (en domain.enemy para no hacer depender dominio
de application). No incluye GameSettings, FSM, A*, zombies, hordas, rendering nuevo
ni modificaciones de persistencia. El gate posterior se registra en testing/unit-integration.md.

## Plan de dos semanas y dependencias

Los IDs permiten reanudar sin perder el alcance. Checklist ejecutable: [TODO.md](TODO.md).

| ID | Momento orientativo | Resultado verificable | Dependencias |
| --- | --- | --- | --- |
| C2-00 | Antes de desarrollar | Revisar esta base y autorizar su commit; gate de 41 pruebas | Revisión humana |
| C2-01 | Inicio, días 1–2 | Acordar caminabilidad, temporización, dificultad y defaults; crear ramas desde base aprobada | C2-00 |
| C2-02 | Semana 1 | BiomeResolver puro, determinista y regional | C2-01 |
| C2-03 | Semana 1 | RED → GREEN → REFACTOR real de FSM | C2-01; independiente de C2-02 |
| C2-04 | Semana 1 | Configuración y planificación determinista de oleadas | C2-01; sin esperar renderer |
| C2-05 | Semana 1 | A* 2.5D con casos de rutas y caminabilidad de World actual | Contrato de C2-01 |
| C2-06 | Semana 1–2 | Capas/alturas por bioma y estructura mínima con pruebas | C2-02 |
| C2-07 | Semana 2 | EnemyUpdateService, seguimiento, repath y conexión de hordas | C2-03, C2-04, C2-05 |
| C2-08 | Semana 2 | Rendering mínimo y menú, integración serializada | C2-07 |
| C2-09 | Semana 2 | Integración, regresión funcional y PIT en scope puro | C2-06, C2-07; gate antes del visual |
| C2-10 | Semana 2 | Harness y resultados reales de cuatro escenarios | IA + navegación integradas |
| C2-11 | Cierre | Documentación, UML y README contrastados, gate final | C2-08, C2-09, C2-10 |

Orden técnico de integración: contratos → biome foundation → Zombie/FSM → A* →
HordeManager/settings → Enemy/World → rendering → menú → integración → PIT →
load tests → documentación final. El trabajo puede ser paralelo, la integración
sobre hotspots no.

## Ownership temporal

| Zona/archivo | Owner |
| --- | --- |
| src/domain/world/biome/, src/domain/world/generation/, test/domain/world/ | Estudiante 1 |
| SimpleTerrainGenerator.java | Estudiante 1 |
| src/domain/enemy/ salvo Difficulty.java; test/domain/enemy/ salvo dificultad | Estudiante 2 |
| application/EnemyUpdateService.java | Estudiante 2 |
| Integración de enemies en VoxelGame.java | Estudiante 2, en ventana acordada |
| application/HordeManager.java | Estudiante 3 |
| application/GameSettings.java y domain/enemy/Difficulty.java | Estudiante 3 |
| MainMenu.java y GameWindow.java | Estudiante 3 |
| pom.xml | Estudiante 3 |
| docs/corte2/testing/ y harness de carga | Estudiante 3 |
| test/application/ y pruebas de dificultad | Estudiante 3 |
| docs/corte2/testing/tdd-zombie-fsm.md | Estudiante 2 escribe evidencia; coordina con Estudiante 3 |
| docs/corte2/terrain-generation.md | Estudiante 1 |
| docs/corte2/enemy-ai.md | Estudiante 2 |
| README.md | Solo integración final tras esta preparación |
| World.java | Cambio pequeño coordinado, sin ediciones simultáneas |
| VoxelGame.java | Hotspot controlado; sin integración concurrente |
| docs/uml.md | Solo integración final |
| ROADMAP_CORTE_2.md, TODO.md y arquitectura-corte2.md | Actualizaciones coordinadas |

Ownership no impide leer. Significa no modificar simultáneamente sin avisar y acordar
el cambio. Ser owner de una zona de tests no autoriza editar tests de otro integrante.
Preferir nuevas clases de prueba a modificar fixtures compartidos.

## Estrategia de ramas

Las ramas salen **después del commit base aprobado**, no del main anterior:

```text
main (base Corte 2 revisada e integrada)
├── feature/c2-terrain
├── feature/c2-enemy-ai
└── feature/c2-quality-hordes
```

En esta preparación no se crean ramas, commits, push, merges ni PR.
El commit base a main requiere aprobación posterior.

Trabajo futuro: commits pequeños por comportamiento. Ejecutar mvn clean test antes
y después de cada cambio coherente; documentar conscientemente cualquier cambio
al baseline, sin ocultar regresiones. Antes de push, repetir el gate. Antes de PR:

```bash
git fetch origin
git rebase origin/main
mvn clean test
```

Rebase solo en una rama personal/no publicada o cuando el equipo lo haya coordinado.
No reescribir historia de una rama compartida ni usar git push --force.
RED de TDD puede tener evidencia/commit local deliberadamente rojo; el gate de
integración y publicación exige GREEN. No presentar un commit RED como gate aprobado.

## Criterios de aceptación final, todavía pendientes

- Misma seed → mismo resultado, biomas regionales y estructura mínima.
- Zombis visibles, FSM observable, A*, evasión de obstáculos simples, ataque y muerte.
- Hordas, ambas dificultades y opciones enemies/textures ON/OFF.
- Enemies OFF: no spawn ni actualización de enemigos.
- Unitarias, integración y evidencia auténtica de TDD.
- PIT con scope definido, conteos y score cercano/superior al 80%, sin tests artificiales.
- Cuatro escenarios de carga con hardware/JVM, tiempos, llamadas A* y memoria.
- Regresión del menú, mundos, interacción, jugador, texturas y JSON del Corte 1.

## Handoff

Completado: baseline verificado y vocabulario/documentación de preparación.
Activo: revisión humana del diff; no hay commit base todavía.
Siguiente: aprobar contratos pendientes, autorizar commit base y luego ramificación.
Detalles: [arquitectura](docs/corte2/arquitectura-corte2.md),
[IA](docs/corte2/enemy-ai.md), [terreno](docs/corte2/terrain-generation.md).
