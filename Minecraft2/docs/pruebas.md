# Pruebas — candidato integrado de recuperación del Corte 2

## Versión y alcance

Se integraron Thomas (`e730e9c`), Jasub (`b7c95a3`, rama jasub/final-a) y Ethian (`d186d46`). El [manifiesto integrado](recuperacion-c2/evidencias/integracion/manifest.json) identifica la base, las fuentes exactas mediante hashes y el JAR usado por el flujo público.

Java Microsoft OpenJDK 17.0.20, Maven 3.9.16 y Linux. En una copia sin target previo pasaron `mvn -o -B clean test`, `mvn -o -B clean verify -DskipUnitTests=true` y `mvn -o -B clean verify`. Se usó la caché de dependencias existente; no se afirma una descarga o instalación desde cero.

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

PIT conserva su selección de FSM, A*, WaveRules, Difficulty, BiomeResolver, HordeManager y GameSettings. Nueva ejecución: 215 mutantes, 185 KILLED, 29 SURVIVED, 1 NO_COVERAGE y 0 TIMED_OUT. Cobertura solo de clases mutadas: 235/238 líneas; 113 tests examinados y 1532 ejecuciones durante la mutación. Killed/generados: 86,05 %. No son 1532 pruebas distintas ni cobertura de todo el dominio. [CSV](recuperacion-c2/evidencias/integracion/mutaciones.csv).

## Comprobación gráfica

PlayerControlsVisualSmoke ejecutó el VoxelGame real con LWJGL/OpenGL, entrada automatizada y mundo determinista en memoria. Verificó sprint, salto, mirada, pausa, muerte, R, melee, minería, colocación, pistola y selección mientras se renderizaban HUD/cámara. Salida 0 en Mesa Intel(R) Graphics (RPL-S), Linux. [Captura](recuperacion-c2/evidencias/integracion/control-integrated.png).

El [README](../../README.md#controles-con-opengl-real) documenta el comando. La captura no demuestra por sí sola todas las acciones: las comprueba el arnés. No es una sesión humana, SUS, flujo de consola ni medición de FPS bajo carga. Windows/PowerShell y macOS no se han comprobado aquí.

## Pendiente de carga y cierre

Ethian debe ejecutar baseline y estrés sobre el mismo candidato integrado, conservar resultados seleccionados en perf/results y completar [carga.md](recuperacion-c2/carga.md) con p95, throughput, errores, población viva real y análisis arquitectónico. Las comprobaciones breves del harness no satisfacen este requisito. No se ha ejecutado el protocolo de carga de recuperación ni se afirma que cumpla el SLO.

Después de recibir esa evidencia se contrastan sus hashes/commit con el candidato final, se actualiza la fila del README y se repite la revisión de entrega. Los tres integrantes revisan ADR/diagramas y deben poder explicar la solución. Los reportes de septiembre siguen como históricos y no acreditan las nuevas corridas.
