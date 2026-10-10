# Minecraft2

Videojuego voxel de escritorio desarrollado en Java 17, Maven y LibGDX por Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz.

## Recuperación del Corte 2

El candidato común integra Thomas (`e730e9c`), Jasub (`b7c95a3`, `jasub/final-a`) y Ethian (`d186d46`). Controles, cobertura, flujo público, ADR y diagramas están incorporados y comprobados. Las corridas de carga de recuperación (baseline y estrés) y su análisis están incorporados; el SLO de 80 zombis se cumple en el equipo medido. [Correcciones y estado](Minecraft2/docs/recuperacion-c2/correcciones.md).

| Integrante | Rama | Responsabilidad |
| --- | --- | --- |
| Thomas | `feature/c2-Thomas` | Controles en aplicación, regresiones, cobertura, build, documentación e integración de los tres frentes |
| Jasub | `jasub/final-a` | Integración, flujo público de caja negra y comprobación de fronteras |
| Ethian | `feature/c2-Ethian` | Comparación de estilos, ADR, contexto/componentes, carga y cierre de entrega |

El [plan compartido](Minecraft2/docs/recuperacion-c2/plan-equipo.md) y el [TODO](Minecraft2/TODO.md) delimitan los archivos de cada frente. La integración conserva el historial de las tres ramas y un respaldo de la base. Ethian completó la carga y la verificación técnica del candidato. Los criterios de cierre están en [pruebas](Minecraft2/docs/pruebas.md#verificación-del-candidato-de-entrega).

## Tabla de trazabilidad

El reto comunicado oralmente fue **incorporar un enemigo**. Conducta, coordinación y coste de actualización son aspectos del mismo reto; no se inventa una lista adicional de retos docentes.

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado |
| --- | --- | --- | --- | --- | --- |
| Incorporar un enemigo con conducta/navegación que coexista con movimiento, interacción y estado de partida | Mantenibilidad, testabilidad y rendimiento de actualización | Capas con coordinación explícita en aplicación: FSM y navegación en dominio; GameSession coordina controles, vida, hordas y enemigos; presentación convierte dispositivos en datos y renderiza | [GameSession](Minecraft2/src/application/GameSession.java), [PlayerControlService](Minecraft2/src/application/PlayerControlService.java), [entrada inmutable](Minecraft2/src/application/PlayerFrameInput.java), [GameInput](Minecraft2/src/presentation/game/GameInput.java), [FSM](Minecraft2/src/domain/enemy/ZombieStateMachine.java), [A*](Minecraft2/src/domain/enemy/AStarPathfinder.java) | [FSM](Minecraft2/test/domain/enemy/ZombieStateMachineTest.java), [A*](Minecraft2/test/domain/enemy/AStarPathfinderTest.java), [sesión](Minecraft2/test/application/GameSessionTest.java), [controles/combate](Minecraft2/test/application/PlayerControlServiceTest.java), [estamina](Minecraft2/test/application/PlayerControlStaminaTest.java), [smoke OpenGL](Minecraft2/test/presentation/game/PlayerControlsVisualSmoke.java). [Flujo público](Minecraft2/test/presentation/GameFlowSmokeIT.java), [fronteras](Minecraft2/test/architecture/ArchitectureBoundaryTest.java) y [carga headless baseline/estrés](Minecraft2/docs/recuperacion-c2/carga.md) con [CSV, consola y manifiestos](Minecraft2/perf/results/README.md) | Verificación integrada: **198 unitarias/adaptadores aislados + 148 integración; 0 fallos, errores u omitidas**. Dominio con unitarias: líneas **885/982 (90,12 %)**, ramas **555/693 (80,09 %)**. Smoke gráfico automatizado aprobado. [Reporte y evidencia](Minecraft2/docs/pruebas.md). Tres escenarios públicos aprobados. Carga de IA headless (commit medido `d180eb9`, mismo árbol que `620f901`; JDK 17, 1 GB): **SLO cumplido con 80 zombis**, p95 7,05–9,86 ms en las tres repeticiones, 0 ticks fallidos y población sostenida; parada en 160 (mediana p95 59,9 ms). Cuello de botella no aislado (A\* y separación como candidatos); no mide FPS gráficos ni el coste de los controles |

La separación añade contratos, cableado y una conversión por tick. No demuestra aceleración de A*, FPS gráficos bajo carga ni eliminación de los ciclos internos del dominio.

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
