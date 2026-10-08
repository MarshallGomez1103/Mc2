# Pruebas de la recuperación del Corte 2

## Versión y alcance

Resultado de las fuentes de `feature/c2-Thomas`, sobre la base `4220c4051cf306cabfaaaa79ea8e4c90c868b631`. La [huella y hashes de fuentes](recuperacion-c2/evidencias/thomas/manifest.json) identifican exactamente las fuentes comprobadas y permiten contrastarlas con la versión del repositorio.

Se ejecutaron Java Microsoft OpenJDK 17.0.20, Maven 3.9.16 y Linux. La verificación se repitió desde una copia de fuentes sin `target/` previo y con las dependencias ya disponibles en la caché de Maven; no se afirma una instalación inicial del sistema ni una descarga desde cero. En esa copia pasaron `mvn -o clean test`, `mvn -o clean verify -DskipUnitTests=true` y `mvn -o clean verify`.

## Resultados por nivel

| Ejecución | Runner | Clases | Casos | Fallos | Errores | Omitidas |
| --- | --- | --- | --- | --- | --- | --- |
| Unitarias y adaptadores aislados | Surefire | 35 | 191 | 0 | 0 | 0 |
| Integración de componentes o archivos reales | Failsafe | 28 | 144 | 0 | 0 | 0 |
| Verificación conjunta | Ambos, selecciones disjuntas | 63 | 335 | 0 | 0 | 0 |

[CSV por clase](recuperacion-c2/evidencias/thomas/pruebas.csv), [casos ejecutados](recuperacion-c2/evidencias/thomas/casos.csv) e [inventario clasificado](recuperacion-c2/evidencias/thomas/clasificacion.csv).

Las 60 clases de la base se conservaron: GameInputStaminaTest se trasladó a PlayerControlStaminaTest, conservando sus tres escenarios y añadiendo dos límites de tiempo. No hay una clase ejecutada por ambos runners ni una clase heredada fuera de la unión. Los totales 238/313 de septiembre describen versiones históricas distintas, no la recuperación.

## Clasificación y estrategia

La propiedad `integration.test.patterns` del POM es la selección común: Surefire la excluye y Failsafe la incluye. Selecciona expresamente los coordinadores actuales, los tests de disco, el Observer/menu y las clases mixtas; además admite `IT*`, `*IT` e `*ITCase` para los nuevos flujos. Al añadir un coordinador con sufijo Test debe revisarse su clasificación; ese sufijo no prueba aislamiento.

Se movieron a Failsafe LocalWorldPersistenceTest, SaveEdgeCasesTest, WorldLifecycleTest, JsonWorldStorageTest, MainMenuSettingsTest, WorldDirectoryTest y GpuPreferenceTest porque usan archivos reales. Los coordinadores de sesión/controles/combate/IA verifican fronteras reales en memoria. ZombieSeparationTest y TerrainGenerationTest incluyen colaboración con aplicación/códec y también se clasifican conservadoramente como integración, aunque contengan algunos casos aislados.

Las unitarias de negocio permanecen en dominio. Surefire también ejecuta parser/códec en memoria y adaptadores/valores visuales aislados; esos casos no se anuncian todos como lógica de negocio. WorldSizeTest utiliza un stub RecordingStorage sin disco. GameInputMappingTest utiliza un doble Input y comprueba traducción sin Gdx global ni ventana. JaCoCo limita el reporte solicitado a `domain/**`.

Las nuevas regresiones separan Arrange, Act y Assert donde hay preparación de estado e interacción. Incluyen intención neutra, límites del delta antes/en/después de 0,05 s, no finitos/negativos/cero, sprint bloqueado/agotar/recuperar, pausa/muerte y acciones simultáneas. Los casos de servicios reales se presentan como integración, no como unitarias aisladas.

## Aportes de esta recuperación

| Clase | Aporte |
| --- | --- |
| PlayerControlStaminaTest | Tres escenarios heredados migrados desde presentación; dos casos nuevos caracterizados antes del traslado: delta grande e inválido; invariancia 30/60/144 FPS |
| GameSessionTest | Cuatro escenarios heredados adaptados al contrato; siete casos adicionales ejecutados, incluidos cinco valores del delta; pausa/muerte congelan controles/arma, reaparición conserva material/pausa |
| CombatSessionRegressionTest | Adaptación mecánica de cinco regresiones al contrato tipado; se conservaron sus aserciones |
| PlayerControlServiceTest | Ocho casos nuevos de mirada/salto, melee/bloques, selección/arma, cooldown e instantánea inmutable |
| PlayerFrameInputTest | Dos casos nuevos de neutral y validación del contrato |
| GameInputMappingTest | Tres casos nuevos del traductor con doble de dispositivo, incluyendo las siete selecciones y cursor liberado |
| PlayerControlsVisualSmoke | Arnés gráfico automatizado nuevo, fuera del conteo JUnit, con OpenGL real y comprobaciones de estado |

Se pasó de 313 a 335 casos, **22 adicionales**, conservando los escenarios anteriores. No se reconstruye ni se afirma un historial de TDD retrospectivo. No se añadieron casos triviales solo para aumentar el conteo.

## Cobertura del dominio

| Fuente de ejecución | Líneas cubiertas/total | Líneas | Ramas cubiertas/total | Ramas |
| --- | --- | --- | --- | --- |
| Unitarias/adaptadores aislados | 885/982 | 90,12 % | 555/693 | 80,09 % |
| Integración | 900/982 | 91,65 % | 540/693 | 77,92 % |

[CSV unitarias](recuperacion-c2/evidencias/thomas/cobertura-dominio-unit.csv) y [CSV integración](recuperacion-c2/evidencias/thomas/cobertura-dominio-integration.csv). Los denominadores incluyen las clases de dominio compiladas; no se excluyen reglas difíciles para elevar el porcentaje. Los dos reportes son independientes y no se suman. No se fijó un mínimo impuesto por la rúbrica, que pide informar cobertura.

JaCoCo se conecta a las JVM de Surefire y Failsafe mediante propiedades de agente separadas y produce HTML/XML/CSV en sus fases correspondientes. `mvn clean test` produce el reporte unitario; `mvn clean verify -DskipUnitTests=true` produce solo cobertura de integración; `mvn clean verify` produce ambos. La omisión explícita de unitarias en el comando de integración no equivale a casos skipped en el resultado Failsafe.

El JAR se empaqueta en package antes de integration-test. Un proceso JAR separado necesita instrumentación propia para añadir cobertura: su ejecución no se incluye automáticamente. Tampoco se incluye la comprobación OpenGL en estos porcentajes.

Referencias de configuración: [agente JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/prepare-agent-mojo.html), [reporte JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/report-mojo.html) y [selecciones de Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/examples/inclusion-exclusion.html).

## Mutación

Se conservó y ejecutó la selección existente de PIT: FSM, A*, WaveRules, Difficulty, BiomeResolver, HordeManager y GameSettings; targetTests domain/application. No se presenta como mutación de todo el repositorio.

Resultado: **215 mutantes: 185 KILLED, 29 SURVIVED y 1 NO_COVERAGE; 0 TIMED_OUT**. PIT informó 235/238 líneas cubiertas en las clases mutadas, 111 tests examinados y 1536 ejecuciones durante la mutación. Estas ejecuciones no son 1536 pruebas distintas. El porcentaje killed/generados es 86,05 % y no equivale a la cobertura de líneas del dominio. [CSV original revisado](recuperacion-c2/evidencias/thomas/mutaciones.csv).

## Comprobación gráfica automatizada

[PlayerControlsVisualSmoke](../test/presentation/game/PlayerControlsVisualSmoke.java) abre LWJGL/OpenGL y utiliza el VoxelGame real con un mundo determinista en memoria y entrada automatizada. No lee/guarda mundos personales. Comprueba:

- Sprint con consumo y movimiento; salto y mirada traducida hacia cámara.
- Pausa y muerte sin avance físico; R restaura controles/vida.
- Melee antes de picar, picar sin enemigo, selección y colocación en el mismo tick.
- Pistola antes de melee/bloques, cooldown congelado al pausar, selección que desequipa antes de actuar.
- Render de HUD/cámara durante el recorrido.

Resultado: salida 0 y todas las aserciones aprobadas en **Mesa Intel(R) Graphics (RPL-S)**. [Captura final revisada](recuperacion-c2/evidencias/thomas/control-smoke.png). La captura por sí sola no demuestra todas las acciones; las verifica el arnés ejecutable. El backend restablece Gdx.input por fotograma, por lo que el doble se coloca en cada render antes de llamar a VoxelGame.

Comandos reproducibles en el [README raíz](../../README.md#controles-con-opengl-real). Requiere sesión gráfica y puede fallar en un equipo sin OpenGL disponible. No es una prueba de la consola pública, una sesión humana, una prueba SUS ni rendimiento bajo carga. Linux/Intel es el entorno comprobado; no se afirma validación gráfica en AMD, Windows o macOS.

## Evidencias históricas y pendientes

Los reportes de `docs/corte2/testing/` se conservan con sus fechas/alcances históricos. La nueva carga y su análisis corresponden a Ethian y aún no están en esta rama. La prueba pública GameFlowSmokeIT y la comprobación automática ArchitectureBoundaryTest corresponden a Jasub y no se anuncian como ejecutadas aquí.

La revisión textual actual no encontró imports de presentation/LibGDX en application ni de capas superiores/LibGDX en domain, y GameInput dejó de contener los coordinadores. Esta inspección no sustituye la prueba contra regresiones arquitectónicas pendiente.

Antes de entregar se deben integrar los otros frentes, repetir `mvn clean verify`, revisar el flujo público/carga/diagramas contra el candidato conjunto y actualizar resultados y hashes si cambian las fuentes. Los comandos y evidencia publicados permiten revisar lo ya comprobado sin asumir cumplimiento de los requisitos aún pendientes.
