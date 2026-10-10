# Recuperación del Corte 2 — Minecraft2

## Objetivo y alcance

Completar la arquitectura y las evidencias que faltan en el proyecto existente, manteniendo la jugabilidad y el formato de guardado. La revisión se organizará por observación corregida, ubicación del cambio, prueba reproducible y resultado.

Equipo: Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz. Thomas integra los tres frentes.

Base revisada: `main`, commit `4d33d6af90f9328717901703603bc1184bcb9a2c`, sin cambios locales al preparar este plan. Los 313 tests corresponden a la ejecución histórica del 26 de septiembre; el total de la recuperación se determinará al ejecutarla.

Meta interna: terminar y solicitar revisión el jueves 8 de octubre de 2026, antes del viernes. Este calendario distribuye trabajo entre el 7 y el 8; no establece una hora oficial de entrega.

Incluye arquitectura, código necesario para respetarla, pruebas y documentación del proyecto. No incluye recuperación de talleres ni de coevaluaciones, funciones nuevas, multijugador, contenedores, un pipeline de tercer corte o bonificaciones opcionales. La decisión arquitectónica se confirma en el primer punto de control; el plan no registra tareas como ejecutadas.

Este documento conserva el reparto y contrato iniciales. El estado de implementación y las comprobaciones actuales están en el [TODO](../../TODO.md), el [reporte integrado](../pruebas.md) y el [índice de correcciones](correcciones.md). Las casillas de este plan son la planificación original. La integración de los tres frentes ya está publicada. El cierre vigente de carga, verificación y entrega corresponde a Ethian; los criterios están en el [reporte actual](../pruebas.md#verificación-del-candidato-de-entrega). Este cierre sustituye el calendario y las dependencias de revisión del reparto inicial.

## 1. Correcciones que deben quedar comprobables

| ID | Observación o requisito | Corrección y condición de aceptación | Responsable |
| --- | --- | --- | --- |
| REC-01 | Falta tabla de trazabilidad en el README | Tabla de seis columnas en el README principal: Reto, Atributo de calidad, Decisión arquitectónica, Dónde está, Prueba, Resultado; enlaces existentes y resultados recién ejecutados | Thomas |
| REC-02 | No hay comparación de estilos ni ADR | Comparar capas con frontera de aplicación explícita y puertos/adaptadores; justificar con el enemigo, mantenibilidad, pruebas y coste de cambio; ADR con contexto, opciones, decisión, ganancias y sacrificios | Ethian |
| REC-03 | Faltan contexto y componentes coherentes | Contexto, contenedor/aplicación de escritorio y componentes con dependencias reales; comparar Corte 1 y recuperación; distinguir vistas históricas | Ethian; Thomas enlaza |
| REC-04 | `GameInput` coordina física y movimiento desde presentación | Entrada de dispositivos convertida en datos; coordinación del tick en aplicación; reglas existentes en dominio; pruebas de regresión y de fronteras | Thomas |
| REC-05 | La arquitectura declarada no explica la evolución | Documento único de arquitectura con antes/después, reglas de dependencia, alternativa descartada, límites y ubicación de cada componente | Thomas, a partir del ADR y diagramas de Ethian |
| REC-06 | Se anuncia un flujo de caja negra ausente | Versionar y ejecutar `GameFlowSmokeIT` a través de la consola pública del programa empaquetado; proceso real y archivos temporales; declarar su alcance de consola | Jasub |
| REC-07 | Evidencias de pruebas deben corresponder a la entrega | Unitarias aisladas con AAA, nombres descriptivos, clases de equivalencia, valores límite y dobles cuando apliquen; integración entre componentes reales; cobertura de dominio y resultados separados por nivel, fallos y omitidas | Thomas configura; Jasub aporta integración y casos propios |
| REC-08 | La carga debe conectarse con reto y arquitectura | Scripts en `perf/`, al menos baseline y estrés, SLO escrito antes de medir, p95, throughput, errores, configuración y análisis del cuello de botella | Ethian |
| REC-09 | Documentación dispersa y conteos contradictorios | README de entrada, arquitectura canónica y reporte actual; documentos anteriores identificados como históricos y enlazados; no repetir cifras como si fueran del mismo build | Thomas |
| REC-10 | La revisión debe localizar las correcciones | Índice breve con cada observación, archivo/commit, comando y evidencia del resultado; accesible desde el README | Thomas |

La ponderación de referencia es la del resumen y hoja de calificación de la rúbrica: retos 15, estilo 15, modelado 10, unitarias 15, integración 15, carga 10 y documentación 10. No se estima una nota nueva ni se prometen puntos.

### Fuente y tratamiento del reto

El reto comunicado al equipo fue incorporar un enemigo. No existe una lista escrita adicional aportada al proyecto. La tabla lo identificará como reto oral y lo descompondrá en conducta, coordinación y coste de actualización, sin convertir esas partes ni las mejoras de biomas/menús en retos docentes inventados.

La fila del reto debe explicar cómo FSM y navegación quedan en dominio, la sesión coordina en aplicación y la presentación lee entrada/dibuja. Debe enlazar las clases reales, las pruebas unitarias de conducta, las pruebas de integración y el escenario de rendimiento pertinente. Los resultados se completan únicamente después de ejecutarlos. Las limitaciones conocidas también deben aparecer.

## 2. Decisión técnica propuesta

Mantener una aplicación de escritorio en un solo proceso, organizada por capas con una frontera de aplicación explícita. Compararla con puertos/adaptadores antes de confirmarla. El sistema ya tiene `WorldStorage` como abstracción de almacenamiento y reglas puras; completar la frontera de entrada evita una migración extensa en dos días.

Reglas a comprobar:

- `domain` conserva movimiento, gravedad, colisiones, estamina, FSM, navegación y reglas de oleadas. No importa aplicación, presentación, persistencia ni LibGDX.
- `application` coordina los casos de uso y el avance de sesión. No depende de LibGDX ni de presentación. Su dependencia actual de la interfaz `persistence.WorldStorage` se declara; no se presenta como una arquitectura hexagonal completa.
- `presentation.game.GameInput` lee dispositivos y forma una solicitud de entrada. No construye ni coordina `PlayerPhysics`, `CollisionResolver`, `PlayerMovementService` o `Stamina`, ni calcula sus reglas.
- `VoxelGame` controla el ciclo gráfico y entrega la solicitud a aplicación. Cámara, HUD, geometría y recursos gráficos permanecen en presentación.
- `persistence` conserva JSON v1 y operaciones de archivo. La composición crea las dependencias en bootstrap o en el punto de creación de la sesión, fuera del traductor de dispositivos.
- La vista puede consultar el modelo para dibujar. Esa lectura no autoriza a coordinar reglas de juego desde la vista.

Ganancias a contrastar: controles comprobables sin dispositivo, un único avance de sesión, responsabilidades verificables y menor acoplamiento del código de entrada. Costes: nuevas clases y conversión de datos por tick, adaptación de HUD/tests y mantenimiento de contratos. Permanecen el ciclo interno `domain.world` ↔ `domain.player`, el Singleton existente y los mundos finitos en memoria. No se declara que desaparezcan todos los ciclos ni que exista streaming.

## 3. Contrato común y dependencias entre frentes

Las tres ramas de trabajo parten de la misma versión de preparación de main. Thomas implementa el contrato de entrada y los tres revisan sus nombres, firmas y semántica. Jasub puede empezar el flujo público, persistencia y la clasificación de pruebas; Ethian puede empezar comparación, ADR, diagramas y scripts. Las pruebas del nuevo control esperan la implementación funcional de Thomas. Después, los dos incorporan ese avance para probar los consumidores. Cualquier cambio posterior del contrato se coordina antes de editar consumidores.

Propuesta de API, todavía por implementar:

- `application.PlayerFrameInput`: datos inmutables con intención `MovementInput`, salto, deltas de mirada, acción primaria/secundaria, alternancia de pistola y bloque seleccionado. No contiene `Gdx`, cámara ni servicios. Incluye una entrada neutra reutilizable.
- `application.PlayerControlService`: coordina los servicios existentes de movimiento, física, colisión, estamina e interacción; no cambia sus reglas.
- `application.PlayerControlState`: consulta inmutable para la vista de energía, agotamiento, movimiento y sprint.
- `GameSession.advance(double rawDelta, PlayerFrameInput input)`: única entrada de avance de juego; incorpora el control de jugador y conserva la secuencia de vida, hordas y enemigos.
- `GameSession.controls()`: estado para HUD/cámara. `respawn()` también restablece los controles. Se preservan los accesos existentes a vida, enemigos, hordas y dificultad necesarios para sus consumidores.

Semántica propuesta para verificar: PAUSED y DEAD no avanzan entrada, física, estamina, vida, hordas ni enemigos; los deltas inválidos no avanzan la sesión; el delta positivo se limita a 0,05 s; Shift consume según desplazamiento real; chocar o quedarse quieto no gasta estamina; reaparecer restablece el estado. Antes de mover código se caracteriza el comportamiento actual para conservar el sprint, salto, mirada, combate y prioridad de interacción. Distinguir el contrato de tiempo de `GameSession` del de los métodos que rechazan argumentos mediante excepción; no alterar silenciosamente esas prioridades al migrar las pruebas.

Una firma vacía que compila no acredita la tarea. Se necesita una primera implementación funcional del contrato para integrar las pruebas y mediciones dependientes.

## 4. Reparto sin cruces de archivos

Todas las rutas de esta sección son relativas a la raíz Git `Mc2`. La aplicación Maven está dentro de `Minecraft2/`.

| Persona | Rama | Archivos de escritura exclusiva |
| --- | --- | --- |
| Thomas | `feature/c2-Thomas` | `Minecraft2/src/application/GameSession.java`; nuevos `PlayerFrameInput`, `PlayerControlService`, `PlayerControlState`; `Minecraft2/src/presentation/game/GameInput.java`, `VoxelGame.java`, `GameHud.java`; `Minecraft2/src/domain/player/MovementInput.java` si necesita depurar su comentario histórico; `Minecraft2/test/application/GameSessionTest.java`, nuevos tests de controles; `Minecraft2/test/presentation/game/GameInputStaminaTest.java`; `Minecraft2/pom.xml`; ambos `.gitignore`; nuevo `README.md` raíz, `Minecraft2/README.md`, `Minecraft2/TODO.md`, `Wiki.md`; documentación canónica y enlaces existentes |
| Jasub | `feature/c2-jasub` | Nuevos `Minecraft2/test/integration/GameSessionFlowIT.java`, `WorldPersistenceIT.java`, `GameFlowSmokeIT.java`; `Minecraft2/test/architecture/ArchitectureBoundaryTest.java`; nuevo `Minecraft2/test/application/RecoveryWorldStorageFailureTest.java` si falta el caso aislado de fallo; nuevos tests de dominio con prefijo `Recovery` si hay huecos comprobados; `Minecraft2/scripts/verify-public-flow.sh` y `.ps1`; `Minecraft2/docs/recuperacion-c2/integracion.md`; evidencia propia en `Minecraft2/docs/recuperacion-c2/evidencias/integracion/` |
| Ethian | `feature/c2-Ethian` | `Minecraft2/test/loadtest/EnemyLoadHarness.java` si requiere corregir métricas; nuevos harnesses/helpers solo bajo `Minecraft2/test/loadtest/`; `Minecraft2/perf/`; `Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md`; `Minecraft2/docs/diagramas/recuperacion-c2/`; `Minecraft2/docs/recuperacion-c2/carga.md` |

Thomas mantiene el documento canónico `Minecraft2/docs/arquitectura.md`, el agregador `Minecraft2/docs/pruebas.md`, el índice `Minecraft2/docs/recuperacion-c2/correcciones.md` y los enlaces/avisos en `docs/uml.md` y documentos anteriores. Ethian entrega ADR y diagramas; Jasub entrega su reporte de integración. Ninguno edita los archivos asignados a otra persona.

Los paquetes de dominio, persistencia, IA y generación se conservan. Si una prueba revela un defecto que exige modificarlos, se registra el fallo y Thomas asigna ese archivo a un único responsable antes de cambiarlo. No se producen refactors paralelos por iniciativa individual.

### Thomas — frontera de aplicación e integración

- [ ] REC-T1. Caracterizar y trasladar la coordinación de entrada/movimiento a aplicación, manteniendo comportamiento.
- [ ] REC-T2. Conectar `GameInput` → `GameSession` y adaptar `VoxelGame`, HUD, cámara y reaparición; retirar la coordinación por callback desde presentación en la ruta normal.
- [ ] REC-T3. Mantener o migrar los tests de estamina a su responsabilidad nueva y cubrir pausa, muerte, respawn, tiempo inválido y delta limitado.
- [ ] REC-T4. Configurar JaCoCo y reporte explícito de `domain.*`; separar integración con Failsafe del conjunto unitario de Surefire. Inventariar los tests existentes antes de configurar inclusiones/exclusiones: `LocalWorldPersistenceTest`, `WorldLifecycleTest`, `SaveEdgeCasesTest`, `JsonWorldStorageTest` y otras clases con infraestructura o interacción real deben clasificarse por contenido, aunque terminen en `Test`. Se pueden seleccionar desde Failsafe sin renombrarlas; ninguna prueba debe perderse ni ejecutarse dos veces. No fijar un porcentaje mínimo atribuido a la rúbrica: esta exige reportar cobertura del dominio.
- [ ] REC-T5. Hacer que `mvn verify` empaquete el JAR antes de las pruebas de proceso y ejecute unitarias e integración. Conservar la configuración existente de PIT y empaquetado.
- [ ] REC-T6. Integrar las ramas, crear tabla/índice y consolidar documentación; verificar ejecución desde una copia limpia.

### Jasub — integración y flujo público

- [ ] REC-J1. Revisar y clasificar los tests existentes; añadir casos propios necesarios, con AAA, nombres descriptivos, equivalencia y límites, sin duplicar pruebas para aumentar el conteo. Usar un stub de `WorldStorage` para aislar una regla o fallo del caso de uso cuando corresponda; esas pruebas no usan disco ni se presentan como integración real. El reporte diferencia pruebas heredadas, añadidas y modificadas.
- [ ] REC-J2. `GameSessionFlowIT`: componentes reales, entrada neutral/movimiento, pausa sin consumo ni avance, muerte y reaparición; verificar la frontera de aplicación implementada por Thomas.
- [ ] REC-J3. `WorldPersistenceIT`: servicio y `JsonWorldStorage` reales en carpeta temporal; guardar/reabrir y JSON inválido que no sustituya el mundo actual; aislar el Singleton en cada prueba.
- [ ] REC-J4. `GameFlowSmokeIT`: iniciar el JAR como proceso separado con `-Dmc2.console=true`, `-Dmc2.gpu=AUTO` y carpeta de mundos temporal; enviar opciones reales al menú, crear/listar/guardar/salir y volver a ejecutar para cargar; comprobar salida, código de proceso y timeout.
- [ ] REC-J5. Cubrir cancelación/eliminación o archivo inválido como segundo escenario público, sin tocar mundos personales. El test usa la interfaz pública y no llama al servicio para simular que el flujo pasó.
- [ ] REC-J6. Comprobar fronteras arquitectónicas: dominio y aplicación sin dependencias gráficas/superiores; `GameInput` sin los coordinadores de física/movimiento/estamina. Usar una comprobación reproducible que detecte la regresión, sin añadir una dependencia innecesaria.
- [ ] REC-J7. Publicar instrucciones y reporte con comandos, resultados reales y límites. El flujo de consola no se denomina una automatización del juego gráfico ni demuestra controles 3D; estos se revisan por separado.

### Ethian — estilo, diagramas y carga

- [ ] REC-E1. Comparar los dos estilos con criterios concretos del enemigo, testabilidad, coordinación, coste de cambio y proporcionalidad. Redactar el ADR y confirmar la elección con el equipo.
- [ ] REC-E2. Dibujar contexto con el jugador y límite del sistema; contenedor de escritorio/JVM, OpenGL y almacenamiento local; componentes y dependencias actuales. Un contenedor de C4 no implica Docker ni un microservicio.
- [ ] REC-E3. Documentar la arquitectura inicial desde un commit real del Corte 1 y la arquitectura recuperada desde el código integrado. Las vistas iniciales no se rotulan como actuales.
- [ ] REC-E4. Preparar scripts reproducibles de baseline y estrés bajo `Minecraft2/perf/`, reutilizando el harness existente cuando corresponda; incluir `.sh` y `.ps1` o comandos equivalentes para los equipos del grupo.
- [ ] REC-E5. Añadir throughput por segundo real y tasa de errores a las métricas del harness, conservando p95, población, búsquedas A*, expansiones y tiempo simulado/real separados. Definir el denominador de errores y el tratamiento de muestras fallidas.
- [ ] REC-E6. Registrar antes de correr el SLO de actualización headless: p95 ≤ 16,67 ms y cero excepciones para la población objetivo que se declare. El presupuesto es una referencia de 60 FPS, no una garantía de FPS gráficos.
- [ ] REC-E7. Propuesta de corridas acotadas: mismo mundo/seed/dificultad/heap; baseline de 3 enemigos y estrés escalonado con 20, 40, 80, 160 y 320. Fijar antes de ejecutar población objetivo del SLO, calentamiento excluido del muestreo, medición por tick y tres repeticiones de 30 s simulados por nivel; detener al superar el presupuesto o alcanzar el límite real acordado. Si no se alcanza degradación, informar el rango ensayado sin afirmar que se encontró el límite del sistema. Registrar la duración real de cada ejecución. Thomas valida previamente configuración y límite de tiempo; no iniciar resistencia prolongada.
- [ ] REC-E8. Copiar resultados seleccionados a `perf/results/` antes de limpiar `target/`; incluir manifiesto de commit, JDK, hardware, SO, parámetros y hash de scripts. No publicar rutas del equipo ni logs de terminal sin revisar.
- [ ] REC-E9. Analizar qué explica el rendimiento y qué aporta la separación arquitectónica. No atribuir una aceleración al traslado entre capas sin una medición comparable. El harness de IA no demuestra el coste de un frame gráfico ni del control completo del jugador.

## 5. Orden de trabajo y puntos de control

| Momento | Thomas | Jasub | Ethian | Condición para avanzar |
| --- | --- | --- | --- | --- |
| 7 de octubre: primera hora | Contrato y reglas de capas; fija base común | Revisa contrato y diseña escenarios | Compara estilos y define medición | Nombres, semántica, ownership y base acordados |
| 7 de octubre: resto del día | Refactor y conexión gráfica; prepara build | Implementa flujo público/persistencia, que pueden empezar sin el nuevo control | ADR, scripts/métricas y borradores de diagramas | Compila; primera ruta de control funcional; trabajo paralelo sin editar hotspots |
| 8 de octubre: mañana | Termina regresiones y configuración | Completa flujo de sesión y prueba de fronteras | Corre carga acotada sobre la base común con el refactor funcional | Unitarias e integración reales pasan; evidencia nueva disponible |
| 8 de octubre: mediodía | Integra Jasub y luego Ethian; ejecuta suite final | Revisa resultados y aislamiento | Ajusta diagramas al código integrado | Candidato único; no quedan ramas sin probar contra ese candidato |
| 8 de octubre: tarde | README, tabla y revisión desde copia limpia | Repite flujo público y revisa enlaces/pruebas | Revisa ADR, carga y coherencia visual | Un recorrido completo por cada criterio de la rúbrica |
| 8 de octubre: cierre | Revisión final, publicación autorizada y aviso de entrega | Verifica que su evidencia se vea en la versión publicada | Verifica diagramas y resultados publicados | Solicitud concreta de revisión y enlaces del commit entregado |

El calendario se ajusta a la disponibilidad del grupo sin desplazar el margen de revisión al último momento. Si falta tiempo, se suspenden mejoras opcionales y se completan los requisitos obligatorios. Una prueba que falla, un skip no explicado o un diagrama falso no se marca como terminado.

## 6. Integración sin perder cambios

1. Actualizar cada clon desde su rama de trabajo: `feature/c2-Thomas`, `feature/c2-jasub` o `feature/c2-Ethian`. Estas ramas se sincronizan con la base de preparación de `main`; no comenzar desde una copia anterior sin actualizar.
2. Cada integrante trabaja en su rama propuesta y solo en sus archivos. Antes de empezar comprueba que no hay trabajo previo sin conservar.
3. Thomas integra primero su frontera funcional; Jasub y Ethian incorporan esa base a sus ramas para probar sus consumidores.
4. Jasub entrega su rama con pruebas verificadas; Thomas la revisa e integra. Luego integra el trabajo de Ethian.
5. Si hay conflictos, los resuelve Thomas con el dueño del archivo y ejecuta las pruebas afectadas. No reemplaza archivos completos sin revisar el contenido de ambos lados.
6. Ejecutar la verificación final sobre el candidato combinado. Si las métricas se generaron en un commit anterior, registrar ese commit y comprobar que no cambiaron las fuentes medidas; si cambiaron, repetir la medición.
7. Mantener commits reales y reversibles con mensajes técnicos. No reescribir historia, fabricar TDD o forzar el push.

Comandos de aceptación previstos después de implementar la configuración, desde `Minecraft2/` y con JDK 17:

```bash
mvn clean verify
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

El primer comando debe ejecutar unitarias, integración y generar cobertura. El segundo ejecuta la mutación configurada y se documenta por separado. Los comandos de flujo público y carga se fijan cuando los scripts existan y se hayan probado; no se anuncian como disponibles antes de eso.

## 7. Documentación de entrada y entrega

El README raíz es la entrada para revisar el repositorio: cómo entrar a `Minecraft2/`, compilar/jugar, ejecutar cada nivel de pruebas, tabla obligatoria e índice de correcciones. El README de la aplicación conserva controles y límites, y enlaza la misma tabla para evitar duplicaciones.

El documento de arquitectura canónico reúne los siete apartados del enunciado:

1. Descripción del sistema y resumen de lo entregado en el Corte 1.
2. Fuente y análisis del reto oral, con la tabla completa de trazabilidad. La tabla del README es la referencia; si se reproduce aquí, se verifica que coincidan sus seis columnas, enlaces y resultados.
3. Comparación de al menos dos estilos con criterios explícitos derivados del reto y ADR enlazado, incluyendo consecuencias positivas y negativas.
4. Arquitectura inicial y evolucionada con diagramas y reglas de dependencias que correspondan a cada versión.
5. Estrategia de pruebas: qué se prueba en cada nivel, por qué y con qué comando del README.
6. Resultados: reportes de ejecución, cobertura del dominio y análisis de carga con sus evidencias.
7. Límites conocidos y trabajo pendiente para el tercer corte, distinguiendo pendientes del equipo de requisitos todavía no confirmados de ese corte.

`docs/pruebas.md` reúne resultados actuales y enlaces a reportes de Jasub/Ethian. `Wiki.md` resume y enlaza estos documentos, sin volver a copiar resultados divergentes. El documento también diferencia aportes existentes y aportes de la recuperación.

La integración real se adapta a la infraestructura del proyecto: servicio de aplicación, dominio y almacenamiento JSON con archivos reales temporales. No se añade una base de datos o servidor HTTP para aparentar cumplimiento de ejemplos de herramientas del enunciado. La justificación de esta equivalencia debe acompañar la evidencia; el flujo de caja negra usa la consola pública y la jugabilidad gráfica se revisa aparte.

El README especifica JDK 17, Maven, requisitos gráficos para jugar, carpeta inicial correcta y comandos probados desde un clon limpio. Incluye un comando identificable para unitarias y otro para integración, además de carga; `mvn verify` puede servir como verificación completa. Describe las ubicaciones de Surefire, Failsafe y JaCoCo y enlaza la evidencia seleccionada que sí se publica. No basta con que un reporte exista únicamente en `target/` ignorado. La cobertura de procesos separados solo se atribuye al reporte si ese proceso fue instrumentado; ejecutar un JAR no agrega automáticamente su cobertura a JaCoCo.

Los tres integrantes preparan un recorrido técnico breve de reto, alternativa, decisión, código, prueba, resultado y coste, y deben poder explicar cualquier parte del proyecto. No se programa una nueva coevaluación ni se promete recuperar presentación. Si se solicita otra demostración, se adapta al tiempo y canal indicados.

El índice de correcciones incluye una fila por observación: problema original → cambio verificable → ruta/commit → comando → resultado. Hace posible revisar directamente cada corrección. La tabla de trazabilidad en el README sigue siendo obligatoria aunque exista este índice.

Los documentos del 26 de septiembre quedan identificados como históricos. Las afirmaciones de arquitectura vigente en los puntos de entrada se actualizan; no se ocultan bajo un enlace a un reporte nuevo. La documentación final distingue: pruebas unitarias, integración, flujo de consola, comprobación gráfica, mutación y carga. No equipara un test detectado u omitido con uno aprobado.

## 8. Revisión antes de entregar

- [ ] Las diez correcciones tienen evidencia accesible; la tabla se ve en el README raíz.
- [ ] El ADR compara al menos dos estilos y declara consecuencias negativas.
- [ ] Hay contexto y componentes actualizados; la arquitectura inicial está identificada por su fuente histórica.
- [ ] La ruta de entrada/paso de simulación respeta el contrato de aplicación y existe una comprobación contra regresiones.
- [ ] El juego conserva controles, pausa/muerte/respawn, combate y JSON v1; los mundos personales no se utilizaron en pruebas.
- [ ] Desde una copia limpia funcionan build, unitarias, integración, flujo público y comandos de carga documentados.
- [ ] Se reporta cobertura de dominio por líneas y ramas; PIT se presenta como mutación, no como sustituto de cobertura.
- [ ] Cada prueba se clasificó por lo que ejecuta; integración heredada no se cuenta como unitaria por terminar en `Test`. Ninguna prueba queda omitida o duplicada por la configuración de Maven.
- [ ] Unitarias tienen AAA, nombres descriptivos, equivalencia, límites y dobles cuando aplican; el reporte distingue aportes existentes y nuevos.
- [ ] El documento arquitectónico contiene los siete apartados exigidos, incluida trazabilidad y pendientes explícitos para el tercer corte.
- [ ] README documenta cada nivel de prueba y enlaza reportes seleccionados y accesibles, no solo carpetas locales ignoradas.
- [ ] Resultados indican versión, ejecutadas, fallos, errores y omitidas; no se sigue anunciando 313 como total de la versión nueva por inercia.
- [ ] Baseline y estrés tienen SLO previo, CSV y reporte; no se promete rendimiento gráfico a partir de IA headless.
- [ ] Se probaron realmente los controles gráficos afectados y se informa qué fue manual y qué automatizado.
- [ ] Se leyeron completos todos los archivos cambiados y sus evidencias visuales. La documentación contiene solo el contenido técnico y los acuerdos del equipo necesarios para el proyecto.
- [ ] El índice de Git contiene solo código, pruebas, diagramas, documentación y resultados revisados del proyecto.
- [ ] Se revisaron las instrucciones de la recuperación y la rúbrica criterio por criterio; no basta con una suite verde.
- [ ] La publicación tiene aprobación; el remoto y la solicitud de revisión apuntan al commit entregado.

## 9. Exclusiones y materiales de trabajo

El `.gitignore` raíz excluye las notas de trabajo y la configuración personal de cada integrante, para que esas exclusiones viajen a los clones. El material de trabajo personal permanece fuera de la entrega. Este plan de tareas compartidas se versiona como acuerdo del equipo, una vez revisado.

No se ignoran README, ADR, pruebas, diagramas ni resultados obligatorios. `target/`, mundos locales, configuración personal y archivos del IDE siguen excluidos. Los CSV/manifiestos seleccionados dentro de `perf/results/` se versionan tras revisar su contenido; no se publica todo `target/`.

Una exclusión no retira un archivo ya rastreado: hay que revisar tanto los archivos de trabajo como el índice. Las comprobaciones locales de entrega no se trasladan por sí solas a las máquinas de los compañeros y no sustituyen la revisión completa.

## 10. Ubicación del plan y seguimiento

Este documento está en `Minecraft2/docs/recuperacion-c2/plan-equipo.md`. La primera sección de `Minecraft2/TODO.md` enlaza este plan e incluye REC-T1–T6, REC-J1–J7 y REC-E1–E9 como pendientes. Se conserva el historial anterior sin confundirlo con la recuperación.

Cada tarea se marca completada únicamente con su ruta de implementación, prueba y evidencia real. El plan organiza el trabajo; el README, arquitectura, ADR, diagramas y reportes constituyen la evidencia final.
