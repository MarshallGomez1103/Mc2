# Pruebas — candidato integrado de recuperación del Corte 2

## Versión y alcance

Se integraron Thomas (`e730e9c`), Jasub (`b7c95a3`, rama jasub/final-a) y Ethian (`d186d46`). El [manifiesto integrado](recuperacion-c2/evidencias/integracion/manifest.json) identifica la base, las fuentes exactas mediante hashes y el JAR usado por el flujo público.

Java Microsoft OpenJDK 17.0.20, Maven 3.9.16 y Linux. En una copia sin target previo pasaron `mvn -o -B clean test`, `mvn -o -B clean verify -DskipUnitTests=true` y `mvn -o -B clean verify`. Se usó la caché de dependencias existente; no se afirma una descarga o instalación desde cero.

Verificación del candidato de entrega: en un clon limpio de `d180eb9` (mismo árbol que `620f901`), con Temurin 17.0.20.1, Maven 3.9.11 y Windows 11, pasaron de nuevo `mvn -B clean test` (38 clases, 198 casos), `mvn -B clean verify -DskipUnitTests=true` (30 clases, 148 casos) y `mvn -B clean verify` (198 + 148), sin fallos, errores ni omitidas y sin clases compartidas entre runners. Los reportes JaCoCo recién generados coinciden con los de la tabla de cobertura. El manifiesto integrado identifica la instantánea anterior a la carga: desde entonces solo cambió `docs/recuperacion-c2/carga.md`, al añadir los resultados (§6–§7); la carga tiene sus propios [manifiestos](../perf/results/README.md).

## Resultados por nivel

| Ejecución | Runner | Clases | Casos | Fallos | Errores | Omitidas |
| --- | --- | --- | --- | --- | --- | --- |
| Unitarias y adaptadores/métricas aislados | Surefire | 38 | 198 | 0 | 0 | 0 |
| Componentes, archivos o proceso reales | Failsafe | 30 | 148 | 0 | 0 | 0 |
| Verificación conjunta | Selecciones disjuntas | 68 | 346 | 0 | 0 | 0 |

[Resultados por clase](recuperacion-c2/evidencias/integracion/pruebas.csv), [casos](recuperacion-c2/evidencias/integracion/casos.csv) e [inventario clasificado](recuperacion-c2/evidencias/integracion/clasificacion.csv). Las 60 clases de la base original siguen presentes, considerando la migración GameInputStaminaTest → PlayerControlStaminaTest y los cuatro renombrados de Jasub. No hay clases omitidas o compartidas por ambos runners.

Los conteos 238/313 son históricos de septiembre. Las [evidencias de Thomas](recuperacion-c2/evidencias/thomas/manifest.json) conservan la etapa e730e9c (335 casos); sus hashes y nombres corresponden a esa etapa, anterior a los renombrados. El resultado actual es 346, no una suma entre etapas.

## Clasificación y aportes

Surefire excluye `integration.test.patterns`; Failsafe incluye esa misma selección y los sufijos IT. Se conservan clases heredadas con nombre Test en integración cuando conectan componentes reales: el sufijo no decide por sí solo. El JAR se empaqueta en package, antes de integration-test; no hace falta ejecutar package por separado antes de verify.

Jasub añadió GameFlowSmokeIT y renombró EnemyUpdateServiceIT, LocalWorldPersistenceIT, SaveEdgeCasesIT y WorldLifecycleIT sin quitar aserciones. La integración conservó la configuración completa de cobertura/clasificación de Thomas; no se reemplazó por una selección basada únicamente en nombres.

| Aporte | Comprobación |
| --- | --- |
| Thomas: controles, frontera de sesión, entrada inmutable y dispositivo aislado | PlayerControlServiceTest, PlayerControlStaminaTest, GameSessionTest, PlayerFrameInputTest y GameInputMappingTest; mantiene las regresiones de combate |
| Jasub: flujo público y cuatro renombrados | Proceso JAR real y suite de aplicación/persistencia conservada |
| Ajuste de integración: flujo público completo | Tres casos: crear/listar/guardar/reabrir en otro proceso/eliminar; cancelación de borrado; JSON inválido sin sustituir el mundo actual |
| Ajuste de integración: fronteras arquitectónicas | ArchitectureBoundaryTest: dominio sin capas superiores/gráficos; aplicación usa solo WorldStorage de persistencia; GameInput sin coordinadores y GameSession sin callback Runnable |
| Ajuste de integración: fallo aislado | RecoveryWorldStorageFailureTest: stub de WorldStorage, error de leer/guardar propagado y mundo activo conservado; sin disco ni red |
| Ethian: métricas y protocolo; ajuste de población | EnemyLoadHarnessMetricsTest verifica latencias/errores/capacidad; EnemyLoadHarnessIT verifica que los cadáveres no reemplazan la población viva. Son comprobaciones breves, no corridas del protocolo de carga |

Se pasó de 313 a 346 casos, 33 adicionales; no se presenta trabajo heredado como nuevo ni se reconstruye TDD. Las nuevas pruebas usan AAA y nombres descriptivos. Los límites/prioridades de delta, pausa/muerte, sprint bloqueado, selección/arma y acciones simultáneas están en las pruebas de controles; el error de almacenamiento usa un doble explícito.

## Integración y caja negra

[Reporte de integración](recuperacion-c2/integracion.md). El almacenamiento real sigue siendo JSON local en directorios temporales; no se añaden HTTP, Docker o BD para imitar ejemplos de herramientas.

[GameFlowSmokeIT](../test/presentation/GameFlowSmokeIT.java) ejecuta exclusivamente el JAR por stdin/stdout, usa la JVM de las pruebas y AUTO para GPU, y arranca con cwd y mundos temporales. Cada proceso tiene un timeout de 30 s y salida redirigida a archivo para evitar un bloqueo al leer antes del timeout. Comprueba salida/código y efectos en disco. No llama a WorldApplicationService ni a clases de juego para producir los resultados públicos.

La persistencia de cambios, un servicio nuevo, el Singleton aislado y el archivo inválido se prueban en LocalWorldPersistenceIT y WorldLifecycleIT; los casos de altura/eliminación se conservan en SaveEdgeCasesIT. GameSessionTest y CombatSessionRegressionTest comprueban la sesión con componentes reales; no se crean pruebas duplicadas solo para que exista otro nombre de clase del plan.

## Cobertura del dominio

| Fuente | Líneas cubiertas/total | Líneas | Ramas cubiertas/total | Ramas |
| --- | --- | --- | --- | --- |
| Unitarias/adaptadores aislados | 885/982 | 90,12 % | 555/693 | 80,09 % |
| Integración | 915/982 | 93,18 % | 553/693 | 79,80 % |

[CSV unitarias](recuperacion-c2/evidencias/integracion/cobertura-dominio-unit.csv) y [CSV integración](recuperacion-c2/evidencias/integracion/cobertura-dominio-integration.csv). JaCoCo incluye todas las clases compiladas de domain y produce HTML/XML/CSV separados en target/site/jacoco-domain-unit y target/site/jacoco-domain-integration. No se suman porcentajes ni se atribuye un umbral a la rúbrica.

El proceso JAR de caja negra y el smoke OpenGL no añaden automáticamente cobertura a esos reportes: no están instrumentados con ese agente. Los casos skipped no se cuentan como aprobados; aquí todos los casos de las selecciones ejecutadas pasaron.

## Mutación

PIT conserva su selección de FSM, A*, WaveRules, Difficulty, BiomeResolver, HordeManager y GameSettings. Ejecución conservada de `b5a8e7c` (las clases y pruebas seleccionadas por PIT no cambiaron en este cierre): 215 mutantes, 185 KILLED, 29 SURVIVED, 1 NO_COVERAGE y 0 TIMED_OUT. Cobertura solo de clases mutadas: 235/238 líneas; 113 tests examinados y 1532 ejecuciones durante la mutación. Killed/generados: 86,05 %. No son 1532 pruebas distintas ni cobertura de todo el dominio. [CSV](recuperacion-c2/evidencias/integracion/mutaciones.csv).

## Comprobación gráfica

PlayerControlsVisualSmoke ejecutó el VoxelGame real con LWJGL/OpenGL, entrada automatizada y mundo determinista en memoria. Verificó sprint, salto, mirada, pausa, muerte, R, melee, minería, colocación, pistola y selección mientras se renderizaban HUD/cámara. También comprobó el cierre de la vista después de morir: GameSession restaura vida/posición y controles, conservando pausa y material. Salida 0 en Mesa Intel(R) Graphics (RPL-S), Linux. [Captura](recuperacion-c2/evidencias/integracion/control-integrated.png).

El [README](../../README.md#controles-con-opengl-real) documenta el comando. La captura no demuestra por sí sola todas las acciones: las comprueba el arnés. No es una sesión humana, SUS, flujo de consola ni medición de FPS bajo carga. El 10 de octubre de 2026 el mismo arnés pasó también con el comando de PowerShell del README en Windows 11, renderer AMD Radeon (Ryzen 5 4500U), sobre el candidato `d180eb9`: salida 0 y las mismas comprobaciones, incluido el cierre tras muerte. macOS no se ha comprobado.

## Carga de recuperación

Ethian ejecutó baseline y estrés conforme a [carga.md](recuperacion-c2/carga.md) sobre el commit `d180eb9`, cuyo árbol es idéntico al del candidato integrado `620f901`. Mismo JDK 17 (Temurin 17.0.20.1), heap de 1 GB y equipo (Ryzen 5 4500U, Windows 11); manifiestos sin cambios locales de fuentes, pruebas, scripts, POM ni protocolo. En ese mismo commit `mvn clean verify` dio 198 + 148 casos, sin fallos, errores ni omitidas.

| Escenario | Niveles | p95 por tick de IA | Errores | Población |
| --- | --- | --- | --- | --- |
| REC_BASELINE | 3 zombis | mediana 0,185 ms | 0 ticks fallidos | sostenida |
| REC_STRESS | 20, 40, 80, 160 | 80: 9,857 / 9,829 / 7,048 ms; 160: mediana 59,893 ms (parada) | 0 ticks fallidos | sostenida en todos los niveles |

SLO-CARGA-01 se cumple en el equipo medido: las tres repeticiones de 80 zombis quedan bajo 16,67 ms, sin fallos ni déficit. Throughput, búsquedas/expansiones A\*, máximos, heap, análisis y límites están en [carga.md §6–§7](recuperacion-c2/carga.md#6-resultados). [CSV, consola y manifiestos](../perf/results/README.md). La carga es headless: no mide FPS gráficos ni la ruta de controles del jugador. Las comprobaciones breves del harness (EnemyLoadHarnessMetricsTest, EnemyLoadHarnessIT) siguen siendo pruebas de las métricas, no corridas del protocolo.

Los tres integrantes deben poder explicar la solución. Los reportes de septiembre siguen como históricos y no se comparan con estas corridas.

## Verificación del candidato de entrega

La validación corresponde al commit que se entrega. Si cambian fuentes, POM, scripts o protocolo después de medir, las corridas afectadas deben repetirse; un commit posterior que solo incorpora resultados debe identificar el commit medido.

1. Ejecutar con JDK 17, desde una copia limpia, los comandos del README: `mvn clean test`, `mvn clean verify -DskipUnitTests=true` y `mvn clean verify`. Comparar clases/casos y fallos, errores y omitidas por runner; revisar los dos reportes JaCoCo del dominio. La referencia actual es 198 + 148, sin duplicación. Si cambia la suite, actualizar inventarios, casos, clasificación y cifras a partir de los nuevos reportes.
2. Comprobar el flujo público GameFlowSmokeIT y el smoke OpenGL documentado. El cierre tras muerte pasa por GameSession, igual que R; restablece vida/posición y estamina sin cambiar el material ni levantar una pausa.
3. Completar baseline y estrés conforme a [carga.md](recuperacion-c2/carga.md). Conservar CSV, consola y manifiestos en perf/results antes de limpiar target. Comparar el mismo commit, JDK, heap y protocolo; informar p95, throughput, errores, población y condiciones de parada. Analizar el cuello de botella y las limitaciones con evidencia.
4. Actualizar la columna Resultado de la tabla de seis columnas del README, las secciones de resultados/análisis de carga y los resúmenes de arquitectura, correcciones y TODO. Mantener separados los resultados históricos y los actuales. No marcar cumplimiento del SLO si falta el nivel de 80 enemigos, hay déficit o alguna repetición incumple.
5. Contrastar el ADR y los diagramas con clases, imports y llamadas reales. Verificar las siete secciones arquitectónicas y los enlaces a pruebas/evidencias. Declarar cualquier criterio no satisfecho; UI/UX son opcionales y no se confunden con la prueba gráfica automatizada.
6. Revisar completos los archivos afectados y el diff del índice antes de publicar: contenido técnico final, sin datos privados ni instrucciones de preparación, y evidencias coherentes con la versión. Comprobar también los archivos rastreados; gitignore no retira archivos ya agregados.
7. Verificar el commit remoto publicado y sus enlaces. La solicitud de revisión identifica ese commit, la tabla de trazabilidad, el ADR, los diagramas y los reportes de pruebas/carga. Conservar el comprobante de envío; las coevaluaciones anteriores no se presentan como recuperadas.

La verificación no se da por terminada mientras falten resultados o existan contradicciones entre código, documentación y evidencia.
