# Pruebas unitarias e integración — preparación

## Baseline confirmado

16 de septiembre de 2026; main 5501b02e183cf81950430eba11828908009d1caf.
Comando previo a los cambios: mvn clean test.
Resultado: BUILD SUCCESS; 41 ejecutadas, 41 exitosas, 0 fallos, 0 errores, 0 ignoradas.
Linux amd64, OpenJDK 26.0.2, Maven 3.9.16, release objetivo 17.

| Suite existente | Casos ejecutados |
| --- | --- |
| application.WorldLifecycleTest | 1 |
| persistence.InvalidWorldFileTest | 26 |
| persistence.JsonWorldStorageTest | 6 |
| persistence.WorldJsonCodecTest | 5 |
| presentation.game.GameWindowTextureModeTest | 1 |
| presentation.game.VoxelGameObserverTest | 2 |

JUnit 5.10.2 y Surefire 3.2.5. Ubicación real: test/, no src/test/java.
Los main de src/manualtest no son parte de los 41 casos ni los ejecuta Surefire.
Las pruebas del Observer/mode gráfico no ejecutan OpenGL ni comprueban meshes.

## Gate de preparación

Posterior a la preparación, el 16 de septiembre de 2026 a las 20:53:54 (America/Bogota):
`mvn clean test` terminó BUILD SUCCESS, con 41 exitosas, 0 fallos, 0 errores,
0 ignoradas. Compiló 42 fuentes principales (39 existentes + 3 enums) y las mismas
7 fuentes de pruebas. Duración Maven observada: 1,791 s; no es benchmark de carga.
Los archivos de preparación aún no tienen commit: evidencia del working tree.
No se modifican pruebas existentes ni lógica del Corte 1.

## Unitarias futuras

- ZombieStateMachineTest: transiciones, prioridad, límites y estado absorbente.
- AStarPathfinderTest: rutas, bloqueo, imposibilidad y desniveles.
- BiomeResolverTest/TerrainGenerationTest: determinismo y regiones/alturas/capas.
- HordeManagerTest: oleadas y tiempos explícitos.
- DifficultyConfigTest: parámetros centralizados y validación.
- Huecos baratos del Corte 1: bordes Chunk, todos los tipos Factory y misma seed.

No tests artificiales de getters/setters ni assertions solo para coverage.

## Integración futura

- Enemy + World: navegación sobre bloques actuales y obstáculos editados.
- Generación + chunks vecinos: resultado independiente del orden y sin cortes indebidos.
- World + Observer: bloqueo/destrucción de nodo invalida o fuerza repath.
- Persistencia: regresión del snapshot existente; zombies no se guardan.

Owners siguen las zonas del roadmap; nombres/API finales pendientes de acuerdo.

## Evidencia pendiente

- [ ] Ejecutar suites nuevas y conservar reportes Surefire.
- [ ] Documentar comandos, conteos, fixtures y frontera probada.
- [ ] Regresión manual de menú, jugador, interacción, guardar/cargar y texturas.
- [ ] No cambiar baseline para ocultar fallos.
