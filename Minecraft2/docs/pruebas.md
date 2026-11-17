# Pruebas y verificación de Minecraft2

## Corte 3 — seguridad E (2026-11-16)

`mvn -B -ntp clean verify`, Java17.0.8.7 Windows, final16/11/2026 21:02:05 UTC−05:00: **279 unitarias +164 integración**, cero fallos/errores/omitidas; dominio935/1006 (92,94 %), gate>=70 % activo. Se conservan los resultados D/B/C como históricos. GameServerIT pasa de5 a8 casos; arquitectura22 y humo JAR4 siguen activos, sin cambios de selección POM.

Prueba roja antes de corregir E-LOG: un cliente con campo JSON duplicado y salto de línea podía inyectar texto en el log mediante el mensaje de la excepción. [Corrección acbff1c](https://github.com/MarshallGomez1103/Mc2/commit/acbff1ccb72d35735d246e28b562df2bbfecf665) conserva cierre/ID/clase sin contenido controlado por el cliente. Los otros casos nuevos verifican recuperación del dueño después de ataques, privacidad de métricas y rechazo del noveno conectado. No son pruebas de DoS distribuido, cuota por origen ni plazo absoluto de lectura.

[Seguridad/STRIDE y registro](seguridad.md), [ADR003](adr/ADR-003-seguridad.md) y [evidencia E](evidencias/fase-e/README.md) enlazan escaneos, cinco fixtures del script SARIF y el PR5 negativo ya existente. No se modifica ni se sustituye el workflow de Thomas. Los comandos completos/enfocados del apartado D siguiente siguen aplicando; para enfocar GameServerIT se usa `mvn -B -ntp verify "-DskipUnitTests=true" "-Dit.test=GameServerIT" "-Djacoco.skip=true"` (parcial, no aceptación global).


## Corte 3 — verificación actual de D (2026-11-16)

Base funcional `fa52cbb` (PR #6 fusionado), rebasada sobre `e618961` para conservar la documentación de #14. D amplía las reglas de [ADR-001](adr/ADR-001-estilo-recuperacion.md) mediante [ADR-004](adr/ADR-004-frontera-red.md) y el humo público del JAR. Los apartados inferiores conservan resultados históricos de C2/B/C; no se deben usar como conteos de la cabecera actual.

### Comandos y selección real

Con Maven usando JDK 17 (`mvn -version`), desde `Minecraft2/`:

```sh
# Aceptación completa: unitarias, gate de dominio, empaquetado, integración y reportes.
mvn -B -ntp clean verify

# Solo reglas: omite JaCoCo intencionalmente; no es aceptación global.
mvn -B -ntp test "-Dtest=ArchitectureBoundaryTest" "-Djacoco.skip=true"

# Solo humo del JAR recién empaquetado: sin unitarias ni gate global de cobertura.
mvn -B -ntp clean verify "-DskipUnitTests=true" "-Dit.test=GameFlowSmokeIT" "-Djacoco.skip=true"
```

En PowerShell se mantienen las comillas de los argumentos Maven con puntos. Los comandos enfocados no sustituyen el verify completo. Maven ejecuta package antes de integration-test; el humo usa `target/minecraft2-0.1.0-SNAPSHOT.jar`, no `original-*.jar` ni clases de test como servidor.

| Clase | Ejecutor y fase | Qué verifica |
| --- | --- | --- |
| ArchitectureBoundaryTest | Surefire, test | 22 casos: fronteras de domain/application/network/persistence, dependencia permitida de WorldStorage, controles/fixtures y fuente real no vacía. No son 22 reglas arquitectónicas distintas. |
| GameFlowSmokeIT | Failsafe, integration-test + verify | Cuatro escenarios del JAR: tres de consola y uno de servidor headless con HTTP/TCP reales. |
| GameServerIT, NetworkClientIT, MultiplayerServerIT | Failsafe | Salud/métricas, protocolo, población, cancelación, aislamiento, terreno compartido y ciclo multijugador. |
| Test heredados de aplicación/persistencia seleccionados por el POM | Failsafe según integration.test.patterns | Integración con mundo/archivos/servicios reales; el sufijo Test no determina por sí solo la categoría. |

Se comprobó el XML de Surefire con 22 casos de arquitectura y el de Failsafe con los cuatro de GameFlowSmokeIT. No aparece GameFlowSmokeIT en Surefire ni ArchitectureBoundaryTest en Failsafe. El POM y ci.yml ya hacen esta selección y no se modifican en D.

### Resultado local completo

JDK 17.0.8.7, Windows, `mvn -B -ntp clean verify`, repetido después del rebase y finalizado 2026-11-16 18:14:36 UTC−05:00: **279 unitarias y 161 integración**, cero fallos, errores u omitidas. Dominio: 935/1006 líneas = **92,94 %**, gate ≥70 % activo. No se suman coberturas de unitarias e integración.

El humo imprime `D_JAR_SMOKE sha256=…` en su salida y el XML, vinculando la ejecución al JAR recién construido. Crea el mundo mediante la consola pública y arranca otro proceso con `-Dmc2.server=true -Djava.awt.headless=true`; obtiene sus puertos efímeros del banner, espera salud real y conecta dos pares TCP. Verifica acción aplicada, ack independiente, bajas, métrica de conectados y reingreso sin jugador fantasma. Nunca instancia GameServer/GameSession dentro del JVM de prueba. La captura del hijo usa un pipe y se cierra antes de borrar temporales de Windows. La parada del proceso no se presenta como prueba universal de guardado ante terminación forzada.

### Ensayo negativo de arquitectura y restauración

Se añadieron temporalmente dos clases Java compilables: `domain.world.ArchitectureViolationProbe` con dependencia de `network.ProtocolCodec`, y `network.GraphicsViolationProbe` con dependencia de `com.badlogic.gdx.Gdx`. La compilación terminó y Surefire falló por **dos infracciones de arquitectura entre 22 casos**, salida Maven 1. Se conservaron ambas fuentes, log y XML antes de retirarlas.

Después se eliminaron las clases de infracción y se ejecutó `clean verify` completo. Se comprobó además que el JAR final no contiene ninguna de esas clases. El código de entrega no las incorpora. Las copias del ensayo se guardan fuera de src/ y del repositorio operativo.

### Gate en Actions y límites

El job `build-pruebas` de [.github/workflows/ci.yml](../../.github/workflows/ci.yml) ejecuta `mvn verify` con bash y pipefail. Un fallo de Surefire o de Failsafe llega al exit code del job. Comprueba explícitamente que arquitectura tenga XML/casos y que JaCoCo ejecutó el gate. Publica Surefire/Failsafe/site como artefacto `reportes-pruebas`, incluso si falla una prueba. [Actions correcto](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976250231), commit `d4ad1d1`: cinco checks aprobados, 279 unitarias +161 integración, sin fallos/errores/omitidas. Los XML descargados confirman arquitectura 22 y humo 4. [Ensayo de arquitectura](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976266242): 279 unitarias, dos fallos exactos en las fronteras prohibidas. [Ensayo de integración](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976623068): 279 unitarias aprobadas y 162 integración con un único fallo deliberado en IntegrationGateProbeIT; el humo real conserva cuatro casos aprobados. Ambos ensayos bloquean build-pruebas. Fuentes incorrectas fuera del PR; metadatos, logs y ZIP comprobados en [evidencia D](evidencias/fase-d/README.md).

Las reglas escanean fuentes y no todo el bytecode/reflexión; las dependencias JSON/almacenamiento del adaptador y WorldStorage en persistence se declaran en ADR-004. La cobertura del proceso hijo JAR no se añade automáticamente al JaCoCo del JVM de Failsafe. El humo no abre GPU, no prueba noVNC ni sustituye humo/carga después de desplegar en main. Los resultados gráficos y carga calentada de C conservan sus condiciones y su incidente de desconexión no explicado. Los ensayos de arquitectura/integración de D no sustituyen el PR negativo de seguridad de la fase E.


## Versión y alcance — registro histórico del Corte 2

Se integraron Thomas (`e730e9c`), Jasub (`b7c95a3`, rama jasub/final-a) y Ethian (`d186d46`). El [manifiesto integrado](recuperacion-c2/evidencias/integracion/manifest.json) identifica la base, las fuentes exactas mediante hashes y el JAR usado por el flujo público.

Java Microsoft OpenJDK 17.0.20, Maven 3.9.16 y Linux. En una copia sin target previo pasaron `mvn -o -B clean test`, `mvn -o -B clean verify -DskipUnitTests=true` y `mvn -o -B clean verify`. Se usó la caché de dependencias existente; no se afirma una descarga o instalación desde cero.

Verificación del candidato de entrega: en un clon limpio de `c2e03e7`, con Temurin 17.0.20.1, Maven 3.9.11 y Windows 11, pasaron `mvn -B clean test` (38 clases, 199 casos), `mvn -B clean verify -DskipUnitTests=true` (30 clases, 148 casos) y `mvn -B clean verify` (199 + 148). No hubo fallos, errores ni omitidas, ni clases compartidas entre runners. Ese commit corrige la clave del índice de chunks de `World`, hallada al perfilar la carga, y añade su regresión (ver [carga.md §7.1](recuperacion-c2/carga.md#71-cuello-de-botella-identificado)). Las cifras de este reporte salen de esa ejecución: [manifiesto final](recuperacion-c2/evidencias/final/manifest.json). El [manifiesto integrado](recuperacion-c2/evidencias/integracion/manifest.json) queda como evidencia de la etapa anterior (198 + 148).

## Resultados por nivel

| Ejecución | Runner | Clases | Casos | Fallos | Errores | Omitidas |
| --- | --- | --- | --- | --- | --- | --- |
| Unitarias y adaptadores/métricas aislados | Surefire | 38 | 199 | 0 | 0 | 0 |
| Componentes, archivos o proceso reales | Failsafe | 30 | 148 | 0 | 0 | 0 |
| Verificación conjunta | Selecciones disjuntas | 68 | 347 | 0 | 0 | 0 |

[Resultados por clase](recuperacion-c2/evidencias/final/pruebas.csv), [casos](recuperacion-c2/evidencias/final/casos.csv) e [inventario clasificado](recuperacion-c2/evidencias/final/clasificacion.csv). Las 60 clases de la base original siguen presentes, considerando la migración GameInputStaminaTest → PlayerControlStaminaTest y los cuatro renombrados de Jasub. No hay clases omitidas o compartidas por ambos runners.

Los conteos 238/313 son históricos de septiembre. Las [evidencias de Thomas](recuperacion-c2/evidencias/thomas/manifest.json) conservan la etapa e730e9c (335 casos); sus hashes y nombres corresponden a esa etapa, anterior a los renombrados. El resultado actual es 347 (346 de la etapa integrada más la regresión del índice de chunks), no una suma entre etapas.

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
| Ethian: regresión del índice de chunks | WorldChunkIndexTest.chunkKeysOfTheLargestWorldHaveDistinctHashCodes: falló con la clave anterior (1 024 claves, 32 hashes) y pasa con World.ChunkKey |
| Ethian: métricas y protocolo; ajuste de población | EnemyLoadHarnessMetricsTest verifica latencias/errores/capacidad; EnemyLoadHarnessIT verifica que los cadáveres no reemplazan la población viva. Son comprobaciones breves, no corridas del protocolo de carga |

Se pasó de 313 a 347 casos, 34 adicionales; no se presenta trabajo heredado como nuevo ni se reconstruye TDD. Las nuevas pruebas usan AAA y nombres descriptivos. Los límites/prioridades de delta, pausa/muerte, sprint bloqueado, selección/arma y acciones simultáneas están en las pruebas de controles; el error de almacenamiento usa un doble explícito.

## Integración y caja negra

[Reporte de integración](recuperacion-c2/integracion.md). El almacenamiento real sigue siendo JSON local en directorios temporales; no se añaden HTTP, Docker o BD para imitar ejemplos de herramientas.

[GameFlowSmokeIT](../test/presentation/GameFlowSmokeIT.java) ejecuta exclusivamente el JAR por stdin/stdout, usa la JVM de las pruebas y AUTO para GPU, y arranca con cwd y mundos temporales. Cada proceso tiene un timeout de 30 s y salida redirigida a archivo para evitar un bloqueo al leer antes del timeout. Comprueba salida/código y efectos en disco. No llama a WorldApplicationService ni a clases de juego para producir los resultados públicos.

La persistencia de cambios, un servicio nuevo, el Singleton aislado y el archivo inválido se prueban en LocalWorldPersistenceIT y WorldLifecycleIT; los casos de altura/eliminación se conservan en SaveEdgeCasesIT. GameSessionTest y CombatSessionRegressionTest comprueban la sesión con componentes reales; no se crean pruebas duplicadas solo para que exista otro nombre de clase del plan.

## Cobertura del dominio

| Fuente | Líneas cubiertas/total | Líneas | Ramas cubiertas/total | Ramas |
| --- | --- | --- | --- | --- |
| Unitarias/adaptadores aislados | 887/984 | 90,14 % | 555/693 | 80,09 % |
| Integración | 917/984 | 93,19 % | 553/693 | 79,80 % |

[CSV unitarias](recuperacion-c2/evidencias/final/cobertura-dominio-unit.csv) y [CSV integración](recuperacion-c2/evidencias/final/cobertura-dominio-integration.csv). JaCoCo incluye todas las clases compiladas de domain y produce HTML/XML/CSV separados en target/site/jacoco-domain-unit y target/site/jacoco-domain-integration. No se suman porcentajes ni se atribuye un umbral a la rúbrica.

El proceso JAR de caja negra y el smoke OpenGL no añaden automáticamente cobertura a esos reportes: no están instrumentados con ese agente. Los casos skipped no se cuentan como aprobados; aquí todos los casos de las selecciones ejecutadas pasaron.

## Mutación

PIT conserva su selección de FSM, A*, WaveRules, Difficulty, BiomeResolver, HordeManager y GameSettings. Ejecución conservada de `b5a8e7c` (las clases y pruebas seleccionadas por PIT no cambiaron en este cierre): 215 mutantes, 185 KILLED, 29 SURVIVED, 1 NO_COVERAGE y 0 TIMED_OUT. Cobertura solo de clases mutadas: 235/238 líneas; 113 tests examinados y 1532 ejecuciones durante la mutación. Killed/generados: 86,05 %. No son 1532 pruebas distintas ni cobertura de todo el dominio. [CSV](recuperacion-c2/evidencias/integracion/mutaciones.csv).

## Comprobación gráfica

PlayerControlsVisualSmoke ejecutó el VoxelGame real con LWJGL/OpenGL, entrada automatizada y mundo determinista en memoria. Verificó sprint, salto, mirada, pausa, muerte, R, melee, minería, colocación, pistola y selección mientras se renderizaban HUD/cámara. También comprobó el cierre de la vista después de morir: GameSession restaura vida/posición y controles, conservando pausa y material. Salida 0 en Mesa Intel(R) Graphics (RPL-S), Linux. [Captura](recuperacion-c2/evidencias/integracion/control-integrated.png).

El [README](../../README.md#controles-con-opengl-real) documenta el comando. La captura no demuestra por sí sola todas las acciones: las comprueba el arnés. No es una sesión humana, SUS, flujo de consola ni medición de FPS bajo carga. El 10 de octubre de 2026 el mismo arnés pasó también con el comando de PowerShell del README en Windows 11, renderer AMD Radeon (Ryzen 5 4500U), sobre `d180eb9` y de nuevo sobre el candidato final `c2e03e7`: salida 0 y las mismas comprobaciones, incluido el cierre tras muerte ([captura](recuperacion-c2/evidencias/final/control-windows.png)). macOS no se ha comprobado.

## Carga de recuperación

Ethian ejecutó baseline y estrés conforme a [carga.md](recuperacion-c2/carga.md) con el mismo JDK 17 (Temurin 17.0.20.1), heap de 1 GB y equipo (Ryzen 5 4500U, Windows 11). Los manifiestos no registran cambios locales de fuentes, pruebas, scripts, POM ni protocolo.

La primera medición (`d180eb9`, mismo árbol que `620f901`) cumplía el SLO y se detuvo en 160 zombis. El perfilado con JFR identificó el cuello de botella en el índice de chunks. Tras corregirlo con una regresión, se repitió la carga sobre `c2e03e7`, el candidato final, con 199 + 148 casos verdes.

| Escenario | Commit | Niveles | p95 por tick de IA | Errores | Población |
| --- | --- | --- | --- | --- | --- |
| REC_BASELINE final | `c2e03e7` | 3 zombis | mediana 0,044 ms | 0 ticks fallidos | sostenida |
| REC_STRESS final | `c2e03e7` | 20, 40, 80, 160, 320 | 80: 1,759 / 1,926 / 1,679 ms; 160: ≤ 13,270 ms; 320: mediana 50,341 ms (parada) | 0 ticks fallidos | sostenida en todos los niveles |
| REC_STRESS previo | `d180eb9` | 20, 40, 80, 160 | 80: 9,857 / 9,829 / 7,048 ms; 160: mediana 59,893 ms (parada) | 0 ticks fallidos | sostenida en todos los niveles |

SLO-CARGA-01 se cumple en el equipo medido: las tres repeticiones de 80 zombis quedan bajo 16,67 ms, sin fallos ni déficit, en ambas mediciones. La corrección no cambió la conducta simulada (mismas búsquedas A\*, expansiones y muertes), solo el coste de las consultas. Throughput, búsquedas/expansiones A\*, máximos, heap, análisis y límites están en [carga.md §6–§7](recuperacion-c2/carga.md#6-resultados). [CSV, consola y manifiestos](../perf/results/README.md). La carga es headless: no mide FPS gráficos ni la ruta de controles del jugador. Las comprobaciones breves del harness (EnemyLoadHarnessMetricsTest, EnemyLoadHarnessIT) siguen siendo pruebas de las métricas, no corridas del protocolo.

Los tres integrantes deben poder explicar la solución. Los reportes de septiembre siguen como históricos y no se comparan con estas corridas.

## Verificación del candidato de entrega

La validación corresponde al commit que se entrega. Si cambian fuentes, POM, scripts o protocolo después de medir, las corridas afectadas deben repetirse; un commit posterior que solo incorpora resultados debe identificar el commit medido.

1. Ejecutar con JDK 17, desde una copia limpia, los comandos del README: `mvn clean test`, `mvn clean verify -DskipUnitTests=true` y `mvn clean verify`. Comparar clases/casos y fallos, errores y omitidas por runner; revisar los dos reportes JaCoCo del dominio. La referencia actual es 199 + 148, sin duplicación. Si cambia la suite, actualizar inventarios, casos, clasificación y cifras a partir de los nuevos reportes.
2. Comprobar el flujo público GameFlowSmokeIT y el smoke OpenGL documentado. El cierre tras muerte pasa por GameSession, igual que R; restablece vida/posición y estamina sin cambiar el material ni levantar una pausa.
3. Completar baseline y estrés conforme a [carga.md](recuperacion-c2/carga.md). Conservar CSV, consola y manifiestos en perf/results antes de limpiar target. Comparar el mismo commit, JDK, heap y protocolo; informar p95, throughput, errores, población y condiciones de parada. Analizar el cuello de botella y las limitaciones con evidencia.
4. Actualizar la columna Resultado de la tabla de seis columnas del README, las secciones de resultados/análisis de carga y los resúmenes de arquitectura, correcciones y TODO. Mantener separados los resultados históricos y los actuales. No marcar cumplimiento del SLO si falta el nivel de 80 enemigos, hay déficit o alguna repetición incumple.
5. Contrastar el ADR y los diagramas con clases, imports y llamadas reales. Verificar las siete secciones arquitectónicas y los enlaces a pruebas/evidencias. Declarar cualquier criterio no satisfecho; UI/UX son opcionales y no se confunden con la prueba gráfica automatizada.
6. Revisar completos los archivos afectados y el diff del índice antes de publicar: contenido técnico final, sin datos privados ni instrucciones de preparación, y evidencias coherentes con la versión. Comprobar también los archivos rastreados; gitignore no retira archivos ya agregados.
7. Verificar el commit remoto publicado y sus enlaces. La solicitud de revisión identifica ese commit, la tabla de trazabilidad, el ADR, los diagramas y los reportes de pruebas/carga. Conservar el comprobante de envío; las coevaluaciones anteriores no se presentan como recuperadas.

La verificación no se da por terminada mientras falten resultados o existan contradicciones entre código, documentación y evidencia.

## Corte 3 — servidor inicial de Jasub (fase B, 2026-11-16)

Incremento inicial `57be0e1` y mejora de relleno `7de8745`, sobre `main` `572b65b`. Ver [servidor y protocolo](servidor.md) para arranque, contrato, límites y explicación del modo de carga.

- Nuevas unitarias Surefire: `network.ProtocolCodecTest` (6), `network.ServerConfigTest` (2) y `network.ServerMetricsTest` (1).
- Nueva integración Failsafe: `network.GameServerIT` (5), con sockets/HTTP reales, población viva de 80 zombis, cierre y reinicio, y un proceso JAR con `-Dmc2.server=true` sin gráficos. Su nombre `*IT` ya está cubierto por el POM vigente, sin modificarlo.
- `mvn -B -ntp clean verify`, JDK 17.0.8 en Windows: **227 unitarias y 153 integración**, 0 fallos, errores u omitidas. JaCoCo de líneas de dominio: 912/984 = **92,68 %**, gate ≥70 % aprobado. No se suman estos conteos a los históricos del Corte 2.
- Los reportes están en `target/surefire-reports`, `target/failsafe-reports` y `target/site/jacoco-domain-unit`; se conservó copia durable fuera de target junto a los logs de esta fase. Al publicar el PR deben conservarse los artefactos y enlaces de Actions del SHA correspondiente.
- Un comando enfocado como `mvn -B -ntp test '-Dtest=ProtocolCodecTest' '-Djacoco.skip=true'` omite cobertura deliberadamente porque ejecuta solo pruebas del protocolo; no es el comando de aceptación. La verificación completa anterior conserva el gate obligatorio.
- Evidencia rojo → verde: `1fe08c3`, `40aa59c`, `57be0e1`. Las pruebas rojas fallan por clases aún inexistentes; el último incremento compila y ejecuta la suite completa. Se conservó también el fallo inicial de limpieza de un log temporal en Windows y su corrección. La suite completa se repitió con `7de8745` y mantuvo los mismos conteos y cero fallos.

Alcance: una conexión de jugador a la vez. Las pruebas no acreditan todavía cliente gráfico remoto, varios jugadores, SLO de tres clientes ni despliegue/CD en Actions. Esas evidencias se incorporan al completar las siguientes fases y la integración con los compañeros.


## Corte 3 — cliente y multijugador de Jasub (fase C, 2026-11-16)

Integra el núcleo de Thomas de `main` `2df9c14`. La verificación completa con JDK 17, `mvn -B -ntp clean verify`, pasó con **271 pruebas unitarias y 160 de integración**, cero fallos, errores u omitidas, y el gate de cobertura ≥70 % activo. El código funcional del incremento está en `f545114`; la revisión posterior mueve la baja inicial al hilo del tick y añade diagnósticos de cierre. La suite completa se repitió después de ambos cambios y mantuvo los mismos conteos, sin fallos. No se reutilizan los conteos históricos de B como resultados de C.

- `ClientProtocolTest` y `TerrainCodecTest`: snapshots inmutables, mensajes inválidos, límites y paleta/RLE del terreno.
- `NetworkInputBufferTest` y `RemotePlayerRendererTest`: acciones entre frames, selección del jugador local y las tres variantes de skin, sin GPU.
- `NetworkClientIT`: TCP real, cierre/cancelación, error de saludo y una acción confirmada por GameServer.
- `MultiplayerServerIT`: tres clientes simultáneos con identidad/skin y ack separados, desconexión/reingreso sin jugador fantasma, terreno real y eliminación de un bloque observada en ambos clientes.
- `GameServerIT`: se conserva arranque del JAR headless, salud, métricas, carga real de 80 zombis y persistencia/cierre. Una segunda conexión válida ahora se admite; los mensajes inválidos siguen aislados.

Los XML de Surefire/Failsafe y JaCoCo se conservan antes de limpiar target; el workflow vigente los publica en Actions. El PR existente es [#6](https://github.com/MarshallGomez1103/Mc2/pull/6); hay que comprobar los checks de su SHA actual. No se modifican POM ni workflows.

Se construyeron las dos imágenes de Ethian con el código integrado y se comprobaron dos clientes gráficos mediante su front/noVNC. [Procedimiento, capturas y límites](evidencias/fase-c/README.md). Una captura registra tres conexiones (dos gráficas y una de prueba), y otra la visión del segundo cliente gráfico. Se conserva también una desconexión previa de causa no establecida; la repetición prolongada no la reprodujo. No son resultados de tres personas jugando ni del CD de Actions.

La carga local de C con el consumidor real de Ethian y calentamiento pasó para tres clientes de prueba y 80 zombis. [CSV, parámetros, veredicto y límites](evidencias/fase-c/README.md#carga-local-complementaria-de-tres-clientes). Las compilaciones y los clientes gráficos estaban detenidos durante la medición. Esto no sustituye despliegue y SLO final de Actions.
