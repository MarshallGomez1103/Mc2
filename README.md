# Minecraft2

Videojuego voxel de escritorio desarrollado en Java 17, Maven y LibGDX por Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz.

## Recuperación del Corte 2

El candidato común integra Thomas (`e730e9c`), Jasub (`b7c95a3`, `jasub/final-a`) y Ethian (`d186d46`). Controles, cobertura, flujo público, ADR y diagramas están incorporados y comprobados. Las corridas de carga de recuperación (baseline y estrés) y su análisis están incorporados: el SLO de 80 zombis se cumple en el equipo medido, y el perfilado identificó y corrigió el cuello de botella del índice de chunks. [Correcciones y estado](Minecraft2/docs/recuperacion-c2/correcciones.md).

| Integrante | Rama | Responsabilidad |
| --- | --- | --- |
| Thomas | `feature/c2-Thomas` | Controles en aplicación, regresiones, cobertura, build, documentación e integración de los tres frentes |
| Jasub | `jasub/final-a` | Integración, flujo público de caja negra y comprobación de fronteras |
| Ethian | `feature/c2-Ethian` | Comparación de estilos, ADR, contexto/componentes, carga y cierre de entrega |

El [plan compartido](Minecraft2/docs/recuperacion-c2/plan-equipo.md) y el [TODO](Minecraft2/TODO.md) delimitan los archivos de cada frente. La integración conserva el historial de las tres ramas y un respaldo de la base. Ethian completó la carga y la verificación técnica del candidato. Los criterios de cierre están en [pruebas](Minecraft2/docs/pruebas.md#verificación-del-candidato-de-entrega).

## Tabla de trazabilidad

El reto comunicado oralmente fue **incorporar un enemigo** (zombis que perciben, persiguen y atacan al jugador). No hay una lista escrita adicional de retos. Cada fila es un aspecto de ese mismo reto con su atributo de calidad, su decisión y su evidencia; no son retos nuevos.

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado |
| --- | --- | --- | --- | --- | --- |
| Incorporar un enemigo: actualizar muchos zombis por fotograma sin salirse del presupuesto de 60 FPS | Rendimiento | IA headless en dominio, coordinada por `EnemyUpdateService`: repath acotado (no hay A\* por tick) y A\* con presupuesto de expansiones. El índice de chunks de `World` usa una clave sin colisiones de hash, corregida tras perfilar | [EnemyUpdateService](Minecraft2/src/application/EnemyUpdateService.java), [AStarPathfinder](Minecraft2/src/domain/enemy/AStarPathfinder.java), [World](Minecraft2/src/domain/world/World.java), [perf/](Minecraft2/perf/README.md) | Carga headless baseline + estrés con SLO previo: p95 ≤ 16,67 ms, 0 ticks fallidos, 80 zombis ([protocolo y análisis](Minecraft2/docs/recuperacion-c2/carga.md), [CSV y manifiestos](Minecraft2/perf/results/README.md)); regresión [WorldChunkIndexTest](Minecraft2/test/domain/world/WorldChunkIndexTest.java) | **Cumple.** 80 zombis: p95 1,759 / 1,926 / 1,679 ms y 0 errores en las tres repeticiones; 160 zombis: ≤ 13,3 ms; parada en 320 (mediana 50,3 ms). JFR demostró el cuello de botella (51 % del tiempo en el índice de chunks). Su corrección bajó el p95 con 80 zombis de 7,0–9,9 a 1,7–1,9 ms. Límites: headless, sin FPS gráficos; ahora limitan las lecturas de bloques de A\* y la separación cuadrática |
| Incorporar un enemigo: ajustar dificultad y oleadas sin tocar la IA ni el render | Modificabilidad | Reglas puras en dominio: `WaveRules` (oleadas) y `Difficulty` → `ZombieParameters`. `HordeManager` solo programa oleadas en aplicación; `GameSettings` es configuración de sesión | [WaveRules](Minecraft2/src/domain/enemy/WaveRules.java), [Difficulty](Minecraft2/src/domain/enemy/Difficulty.java), [HordeManager](Minecraft2/src/application/HordeManager.java), [GameSettings](Minecraft2/src/application/GameSettings.java) | [WaveRulesTest](Minecraft2/test/domain/enemy/WaveRulesTest.java), [DifficultyTest](Minecraft2/test/application/DifficultyTest.java), [HordeManagerTest](Minecraft2/test/application/HordeManagerTest.java), [GameSettingsTest](Minecraft2/test/application/GameSettingsTest.java); mutación con PIT | **Cumple.** Pruebas aprobadas; VERY_HARD usa los mismos algoritmos con otros parámetros. PIT (ejecución conservada de `b5a8e7c`, selección sin cambios): mutantes eliminados en Difficulty 5/5, GameSettings 14/14, WaveRules 30/32 y HordeManager 79/94. Límite: un nivel de dificultad nuevo requiere añadirlo a `Difficulty` |
| Incorporar un enemigo: probar conducta del zombi y controles del jugador sin ventana ni GPU | Testabilidad | FSM, A\*, navegación y física del zombi en `domain.enemy`, sin LibGDX. `GameInput` solo traduce dispositivos a `PlayerFrameInput` inmutable, y `PlayerControlService` coordina movimiento, física, estamina y combate en aplicación | [ZombieStateMachine](Minecraft2/src/domain/enemy/ZombieStateMachine.java), [PlayerFrameInput](Minecraft2/src/application/PlayerFrameInput.java), [PlayerControlService](Minecraft2/src/application/PlayerControlService.java), [GameInput](Minecraft2/src/presentation/game/GameInput.java) | [FSM](Minecraft2/test/domain/enemy/ZombieStateMachineTest.java), [A\*](Minecraft2/test/domain/enemy/AStarPathfinderTest.java), [controles/combate](Minecraft2/test/application/PlayerControlServiceTest.java), [estamina](Minecraft2/test/application/PlayerControlStaminaTest.java), [fronteras](Minecraft2/test/architecture/ArchitectureBoundaryTest.java), [smoke OpenGL](Minecraft2/test/presentation/game/PlayerControlsVisualSmoke.java) | **Cumple.** **199 unitarias/adaptadores + 148 integración; 0 fallos, errores u omitidas.** Dominio con unitarias: líneas **887/984 (90,14 %)**, ramas **555/693 (80,09 %)**. FSM: 26/29 mutantes eliminados. Las reglas de import se comprueban automáticamente. El smoke OpenGL automatizado pasó en Linux/Intel y Windows/AMD; no es una prueba con usuarios. [Reporte](Minecraft2/docs/pruebas.md) |
| Incorporar un enemigo sin romper mundo, guardado ni estado de partida | Mantenibilidad | `GameSession.advance(delta, PlayerFrameInput)` es el único avance del tick: pausa y muerte congelan jugador, hordas e IA. Los enemigos son estado de sesión y JSON v1 no cambia. Persistencia detrás de `WorldStorage` | [GameSession](Minecraft2/src/application/GameSession.java), [WorldApplicationService](Minecraft2/src/application/WorldApplicationService.java), [JsonWorldStorage](Minecraft2/src/persistence/JsonWorldStorage.java) | [GameSessionTest](Minecraft2/test/application/GameSessionTest.java), [CombatSessionRegressionTest](Minecraft2/test/application/CombatSessionRegressionTest.java), [LocalWorldPersistenceIT](Minecraft2/test/application/LocalWorldPersistenceIT.java), [WorldLifecycleIT](Minecraft2/test/application/WorldLifecycleIT.java), [stub de almacenamiento](Minecraft2/test/application/RecoveryWorldStorageFailureTest.java), caja negra [GameFlowSmokeIT](Minecraft2/test/presentation/GameFlowSmokeIT.java) | **Cumple.** Regresiones de pausa, muerte, reaparición y combate aprobadas. Tres escenarios públicos del JAR por consola aprobados: crear, guardar, reabrir en otro proceso y eliminar; cancelar el borrado; JSON inválido sin sustituir el mundo. Límites: `application` depende de `persistence.WorldStorage` (inversión incompleta) y persiste el ciclo `domain.world` ↔ `domain.player` ([ADR](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md)) |

La separación en capas añade contratos, cableado y una conversión de entrada por tick. No acelera A\* por sí sola: la mejora de carga se debe a la corrección del índice de chunks, que la separación permitió localizar y cambiar en un solo archivo de dominio. Tampoco demuestra FPS gráficos bajo carga ni elimina los ciclos internos del dominio.

## Compilar y jugar

Requisitos: **JDK 17**, Maven y una sesión gráfica compatible con OpenGL para jugar. Verificar el JDK de Maven con `mvn -version`. Desde la raíz Git:

```bash
cd Minecraft2
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

El empaquetado ejecuta unitarias; la verificación completa requiere `verify`. [Controles, consola y guardados](Minecraft2/README.md).

## Ejecutar pruebas por nivel

Desde `Minecraft2/`:

```bash
# Unitarias y adaptadores aislados; sin la selección de integración
mvn clean test

# Integración; compila y empaqueta el JAR sin ejecutar unitarias
mvn clean verify -DskipUnitTests=true

# Verificación completa: unitarias, JAR, integración y cobertura separada
mvn clean verify

# Mutación con la selección existente de PIT
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Surefire genera `target/surefire-reports/`; Failsafe genera `target/failsafe-reports/`. JaCoCo genera `target/site/jacoco-domain-unit/` y `target/site/jacoco-domain-integration/`, con HTML, XML y CSV de `domain/**`. No se suman ambos porcentajes ni se configura un umbral atribuido a la rúbrica. Ejecutar un JAR separado no incorpora automáticamente su cobertura al reporte.

La clasificación conserva clases heredadas con sufijo `Test` en Failsafe cuando conectan componentes o archivos reales. Los nuevos sufijos `IT` también van a Failsafe. El [inventario y los resultados seleccionados](Minecraft2/docs/pruebas.md) permiten comprobar que ninguna clase quedó omitida o duplicada.

### Controles con OpenGL real

Comprobación automatizada, distinta del flujo público de consola y de una sesión humana. Desde `Minecraft2/`, en Linux/macOS con entorno gráfico:

```bash
mvn test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt
java -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" presentation.game.PlayerControlsVisualSmoke target/control-smoke.png
```

En PowerShell, después del comando Maven:

```powershell
$mc2Classpath = "target/classes;target/test-classes;$(Get-Content -Raw target/test-classpath.txt)"
java -cp $mc2Classpath presentation.game.PlayerControlsVisualSmoke target/control-smoke.png
```

La comprobación abre una ventana temporal, usa un mundo determinista en memoria y verifica controles, combate, pausa/muerte y R. Falla si una aserción falla o se cierra antes de terminar; no usa mundos personales. Pasó en Linux con Intel/Mesa y, con el comando de PowerShell, en Windows 11 con AMD Radeon (Ryzen 5 4500U). macOS no se ha comprobado.

### Carga y flujo público

GameFlowSmokeIT está integrado y se ejecuta con Failsafe mediante verify. Los scripts de baseline/estrés y el SLO están versionados, y las corridas de recuperación están [publicadas en perf/results](Minecraft2/perf/results/README.md) con [resultados y análisis](Minecraft2/docs/recuperacion-c2/carga.md#6-resultados). Desde Minecraft2/: `perf/run-load.sh baseline` y `perf/run-load.sh stress` (Windows: `perf/run-load.ps1`). El [protocolo](Minecraft2/docs/recuperacion-c2/carga.md) fija escenario, repeticiones, límite real y parada. La [evidencia de carga del 26 de septiembre](Minecraft2/docs/corte2/testing/load-testing.md) es histórica y no acredita la recuperación actual.

## Documentación y límites

- [Arquitectura canónica: evolución, reglas, comparación y pendientes](Minecraft2/docs/arquitectura.md).
- [Pruebas, cobertura y aportes de la recuperación](Minecraft2/docs/pruebas.md).
- [Índice de correcciones](Minecraft2/docs/recuperacion-c2/correcciones.md).
- [Wiki e índice de etapas](Wiki.md).
- [ADR y comparación de estilos](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md).
- [Contexto, contenedores y evolución de componentes](Minecraft2/docs/diagramas/recuperacion-c2/README.md). El [UML previo](Minecraft2/docs/uml.md) conserva sus versiones históricas.

Mundos finitos en memoria; enemigos, restos, estamina y arma son estado de sesión sin persistencia JSON. Se conserva JSON v1. La prueba gráfica automatizada no acredita equilibrio de jugabilidad, usabilidad ni rendimiento gráfico bajo carga. La carga de recuperación es headless y no acredita FPS gráficos; se midió en un solo equipo Windows.
