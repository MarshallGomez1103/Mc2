# Contratos de trabajo — Minecraft 2, Corte 2

Especificación compartida para los tres estudiantes y sus agentes de IA.
Define responsabilidades y contratos conceptuales; no afirma que las capacidades
planificadas ya funcionen ni congela firmas Java todavía no acordadas.
Complementa [TODO.md](../../TODO.md), [ROADMAP_CORTE_2.md](../../ROADMAP_CORTE_2.md)
y [los acuerdos de arquitectura](arquitectura-corte2.md).

## 1. Reglas generales

- El Corte 1 está terminado y debe preservarse: menú, mundos, jugador, interacción,
  rendering, texturas y persistencia existentes.
- El Corte 2 tiene tres frentes: generación procedural, Enemy AI y hordas/configuración/calidad.
- Los tres trabajan sobre el mismo repositorio y la misma base main; las ramas de
  trabajo siguen el roadmap. No integrar ni editar hotspots simultáneamente.
- Cada estudiante limita su agente a su responsabilidad y backlog actual.
- Antes de trabajar: revisar `git status` y hacer pull en la rama correspondiente
  (`git pull --ff-only`). Si hay cambios locales o divergencia que impidan actualizar,
  detenerse y coordinar; no resetear, descartar ni guardar cambios ajenos automáticamente.
- Ejecutar `mvn clean test` antes y después de cambios coherentes. Las 41 pruebas
  existentes deben preservarse; cualquier modificación consciente requiere explicación.
- No hacer refactors transversales, renombrar paquetes o cambiar Maven/LibGDX.
- No modificar funcionalidad de otro estudiante sin coordinación explícita.
- Los zombies son estado de sesión: no persistirlos ni modificar WorldJsonCodec para ellos.
- No implementar mundo infinito/streaming ni funcionalidades de Corte 3, todavía no definido.
- Mantener KISS/YAGNI. No Behavior Tree, ML, LLM, ECS, multiplayer ni survival completo.

**HOTSPOTS COMPARTIDOS:** `World.java`, `VoxelGame.java`, `MainMenu.java`, `pom.xml`
y `README.md`. Antes de modificar cualquiera, coordinar alcance, owner y ventana
de edición con el equipo. Ownership permite leer, no autoriza ediciones concurrentes.

## 2. Estudiante 1 — Generación procedural

### Objetivo y responsabilidades

Transformar la generación actual en generación determinista con regiones de biomas
reconocibles. Responsable de BiomeType, BiomeResolver, PLAINS, DESERT, MOUNTAINS,
VILLAGE/estructura sencilla, determinismo mediante seed, pruebas, integración con
SimpleTerrainGenerator y documentación de generación.

Zona principal: `src/domain/world/biome/`, `src/domain/world/generation/`,
`test/domain/world/` y `docs/corte2/terrain-generation.md`.

### Contrato BiomeResolver

Entrada: `seed + worldX + worldZ`. Salida: `BiomeType`.

- Mismas entradas producen el mismo bioma; no depender del orden de consulta.
- Producir regiones, no ruido aleatorio bloque por bloque.
- No renderizar, conocer zombies, acceder a UI o guardar archivos.
- Coordinar la escala para demostrar regiones dentro del mundo finito existente.

### Contrato de generación

`seed + coordenadas + biome` produce conceptualmente `altura + BlockType`.
La altura se consulta por columna X/Z; el material también depende de Y.
Conservar determinismo en coordenadas arbitrarias, incluidas negativas.

| Capacidad | Regla mínima |
| --- | --- |
| PLAINS | Relieve moderado/suave, GRASS/DIRT/STONE y árboles ocasionales |
| DESERT | Arena predominante, relativamente plano, árboles normales ausentes |
| MOUNTAINS | Alturas mayores, variación espacial coherente; bloques en `0..Chunk.HEIGHT-1` |
| VILLAGE | MVP pequeño y determinista; no recrear Minecraft; ubicación separada de plantilla |

VILLAGE es estructura, no un bioma. Puede limitarse inicialmente al interior de un
chunk, documentando la limitación y evitando cortes en fronteras. Sin física de arena.

### Prioridad

1. BiomeResolver.
2. Tests BiomeResolver.
3. PLAINS.
4. DESERT.
5. MOUNTAINS.
6. Tests de generación, determinismo y chunks vecinos.
7. Village.
8. Integración/documentación.

### Fuera de responsabilidad

No implementar Zombie, FSM, A*, HordeManager, Difficulty, mutation testing global
ni rendering de enemigos. Coordinar bugs compartidos, no corregir otros subsistemas por iniciativa propia.

## 3. Estudiante 2 — Enemy AI

### Objetivo y responsabilidades

Zombies que deciden comportamiento mediante FSM y persiguen mediante A*.
Responsable de Zombie, ZombieState, ZombieStateMachine, TDD de FSM, NavigationNode,
NavigationGrid, AStarPathfinder, EnemyUpdateService, seguimiento de path, detección,
ataque, muerte, repath e integración visual mínima controlada.
Zombie no hereda de Player ni depende de teclado, cámara o persistencia.

Zona principal: `src/domain/enemy/` excepto Difficulty, `application/EnemyUpdateService.java`,
tests de IA en `test/domain/enemy/` y `docs/corte2/enemy-ai.md`.
Escribe evidencia TDD en `docs/corte2/testing/tdd-zombie-fsm.md`, coordinado con Estudiante 3.

### Contrato FSM

Entrada: `estado actual + condiciones observables`. Salida: `nuevo ZombieState`.
Estados iniciales: IDLE, CHASE, ATTACK, DEAD.

FSM = decide qué hacer. No renderiza, lee teclado, persiste ni ejecuta A* internamente.
Debe probarse con JUnit sin LibGDX. EnemyUpdateService coordina intención y navegación.
Conceptualmente: detección → CHASE; alcance de ataque → ATTACK; alejamiento → CHASE;
pérdida de objetivo → IDLE; salud <= 0 → DEAD; DEAD permanece DEAD.
Precisar límites, prioridades y pérdida del objetivo antes de escribir conducta.

TDD obligatorio: **RED → GREEN → REFACTOR**. Tests primero y evidencia real de las tres
etapas. Un error ambiental no prueba RED. Durante RED se admite el fallo deliberado
local; antes de integrar/publicar, toda la suite debe estar verde.

### Contrato A*

Entrada conceptual: `World actual + origen + destino`. Salida: `Path`.
Acordar cómo representar ausencia de ruta, sin simular éxito cuando el destino es imposible.

Navegación inicial 2.5D: `X/Z + altura caminable`. Consultar bloques ACTUALES:
apoyo sólido, espacio corporal, desnivel permitido y chunk existente.
No depender exclusivamente de `SimpleTerrainGenerator.surfaceHeightAt()`.
No convertir chunks ausentes en terreno caminable.
Definir groundY vs altura de pies, vecinos y subidas/caídas antes de integrar.

A* determina por dónde ir, no qué estado adoptar. Implementar búsqueda y reconstrucción
reales, sin LibGDX. Probar recto, obstáculo, rodeo, imposible, origen=destino y desnivel.
No recalcular para todos los zombies en todos los frames: política de repath por
cambio relevante del objetivo, fin/invalidez de ruta o intervalo. Evitar busy retry sin ruta.

### Fuera de responsabilidad

No implementar biomas, villages, configuración PIT, load testing general o persistencia
de zombies. Coordinar con Estudiante 3 el registro de enemigos y parámetros.

## 4. Estudiante 3 — Hordas, configuración y calidad

### Objetivo y responsabilidades

Controlar experiencia de enemigos e infraestructura académica de calidad.
Responsable de GameSettings, Difficulty, NORMAL/VERY_HARD, enemigos ON/OFF,
enemy textures ON/OFF, HordeManager, oleadas, integración mínima con menú,
tests de hordas/settings, PIT, mutation testing, load/performance y documentación de testing.

Zona principal: nuevos archivos propios de `application/`, pruebas correspondientes,
`docs/corte2/testing/` y `pom.xml`. Difficulty permanece en `domain/enemy/Difficulty.java`:
su ubicación no transfiere ownership a Estudiante 2. No editar tests ajenos sin acuerdo.

### Contrato GameSettings

Centralizar configuración del Corte 2. Dificultad modifica parámetros, no duplica IA
ni dispersa condicionales `if (VERY_HARD)`. Acordar defaults y lifecycle de sesión.
Enemies OFF impide spawn y actualización. Enemy textures OFF permite visual básico;
es una opción distinta de las texturas de bloques existentes.

### Contrato HordeManager

Entrada: `configuración + estado de oleada + enemigos activos + tiempo`.
Salida: decisiones de spawn/oleada: iniciar, cantidad, intervalo, terminar y avanzar.
Tiempo explícito cuando permita determinismo; acordar creación/registro con Estudiante 2.
No calcula paths, ejecuta FSM individual, renderiza o genera terreno.

### Calidad

Mutation testing: objetivo aproximado `>= 80% mutation score` en scope seleccionado
de lógica pura, no 80% global del videojuego. Registrar generated, killed, survived,
no coverage y score; analizar supervivientes con tests de comportamiento, sin maquillaje.

Load testing: BASELINE, PEAK, STRESS y ENDURANCE/RESISTANCE. Medir actualización IA,
A*, generación y memoria aproximada; frame time solo cuando haya prueba gráfica.
Registrar hardware/JVM, escenario, seed, duración y resultados reales. No inventar evidencia.

### Fuera de responsabilidad

No implementar BiomeResolver, montañas, AStarPathfinder ni lógica interna de
ZombieStateMachine. Tests y cambios que afecten APIs ajenas requieren coordinación.

## 5. Contratos entre equipos

Estas flechas describen flujo/dependencia conceptual, no imports obligatorios ni permiso
para modificar código del otro. Las firmas y adaptación se acuerdan entre owners.

```text
BiomeResolver
      ↓
Terrain Generation
      ↓
World (bloques actuales)
      ↓
NavigationGrid
      ↓
A*
      ↓
Zombie (seguimiento del Path)

GameSettings
      ↓
HordeManager
      ↓
Zombie creation

Zombie
      ↓
EnemyUpdateService
      ↓
VoxelGame integration
```

BiomeResolver, FSM pura y scheduling/settings pueden avanzar independientemente.
A* necesita caminabilidad acordada; EnemyUpdateService necesita Zombie/FSM/navegación;
spawn real necesita registro; rendering necesita posición; carga de IA necesita integración
e instrumentación. FSM no llama A*: el servicio coordina ambos.

## 6. Archivos de alto riesgo

Rutas relativas a la raíz Maven `Minecraft2/`.

| Archivo | Riesgo | Regla |
| --- | --- | --- |
| `src/domain/world/World.java` | Agregado compartido por generación, navegación y persistencia | Cambio pequeño coordinado; no editar simultáneamente |
| `src/presentation/game/VoxelGame.java` | Loop, meshes y lifecycle; conflicto de integraciones | Estudiante 2 integra enemigos en ventana controlada acordada |
| `src/presentation/MainMenu.java` | Opciones y flujo del usuario | Estudiante 3; coordinar hotspot antes de editar |
| `src/domain/world/SimpleTerrainGenerator.java` | Altura/materiales consumidos por generación | Owner principal Estudiante 1; cualquier otro cambio requiere acuerdo |
| `pom.xml` | Build y dependencias de todos | Owner principal Estudiante 3; coordinar antes de cambiar |
| `README.md` | Estado y comandos compartidos | Solo integración final coordinada; no cambios concurrentes |

GameWindow pertenece a Estudiante 3; docs/uml.md se actualiza en integración final.
Ser owner no elimina obligación de coordinar un hotspot compartido.

## 7. Definition of Done individual

Una tarea no termina solo porque compila. Requiere:

- Código implementado dentro del alcance y contrato acordados.
- Tests correspondientes y comportamiento relevante comprobado.
- `mvn clean test` verde, sin romper Corte 1 ni esconder regresiones.
- TODO actualizado cuando corresponda, con coordinación para evitar conflictos.
- Documentación actualizada cuando corresponda, sin afirmar trabajo no probado.
- Commit pequeño y descriptivo, realizado por estudiante o con autorización explícita.

Sin autorización de commit, el agente entrega diff probado para revisión: no se
autoautoriza por esta Definition of Done. Un RED local documentado es etapa de TDD,
no una funcionalidad terminada ni un quality gate de integración aprobado.

## 8. PROTOCOLO OBLIGATORIO PARA AGENTES DE IA

**ANTES DE MODIFICAR:**

1. Leer TODO.md.
2. Leer ROADMAP_CORTE_2.md.
3. Leer este documento.
4. Identificar estudiante actual; si no se indicó, preguntar antes de editar.
5. Trabajar solamente su backlog autorizado y comprobar ownership/hotspots.
6. Revisar `git diff` antes de finalizar; incluir archivos nuevos en la revisión
   mediante `git status --short` y lectura directa (no aparecen en diff por defecto).

Leer también las instrucciones aplicables del repositorio y documentación específica
del frente. Revisar estado Git, actualizar con pull de manera segura y ejecutar baseline.
Ante contradicción, API pendiente o tarea ajena necesaria: reportar y coordinar,
no ampliar alcance automáticamente. Al finalizar ejecutar tests y `git diff --check`.

**PROHIBIDO PARA EL AGENTE:**

- Reestructurar todo el proyecto o cambiar arquitectura sin autorización.
- Implementar tareas de otro estudiante o editar hotspots sin coordinación.
- Hacer commit/push automáticamente.
- Eliminar tests o reducir assertions para lograr build verde.
- Inventar evidencia, resultados de PIT, carga o TDD.
- Declarar funcionalidades completas sin probarlas.
- Persistir zombies, implementar streaming o diseñar Corte 3.

Entrega mínima: archivos tocados, resultados reales, límites/no probado y siguiente
paso dentro del frente. Documentación no reemplaza implementación ni verificación.
