# Evolución de Minecraft2 en los tres cortes

Este documento recorre el sistema en los tres cortes (diseño, arquitectura y pruebas, entrega continua y segura), indica qué decisiones se mantuvieron y cuáles cambiaron y por qué, registra cómo se trataron los pendientes del Corte 2 y declara los límites conocidos del sistema final. Lo lee quien revisa el proyecto y necesita entender por qué el sistema está como está.

> **Estado del documento: esqueleto con las partes de los Cortes 1 y 2 ya redactadas a partir de los documentos existentes.** Todo lo del Corte 3 está marcado «Previsto — responsable: Persona N» y se completa con la evidencia real; los marcadores se retiran antes de la entrega.

Responsable: Persona 1.

## 1. Recorrido por corte

| Corte | Enfoque | Qué se construyó | Fuentes |
| --- | --- | --- | --- |
| 1. Diseño | Patrones de diseño y capas | Menú de consola; crear, listar, cargar, guardar y eliminar mundos; chunks y bloques; jugador con movimiento, salto, gravedad y colisiones; raycast; render 3D con LibGDX; persistencia JSON v1. Patrones Factory (bloques), Observer (cambios de bloques) y Singleton (mundo actual). | [Factory](factory.md), [Observer](observer.md), [Singleton](singleton.md), [decisiones compartidas](decisiones-compartidas.md), [UML histórico](uml.md) |
| 2. Arquitectura y pruebas | Estilo en capas con frontera de aplicación explícita; pruebas unitarias, de integración y de carga | Enemigos con FSM y A\*, oleadas, vida y pistola. En la recuperación: coordinación del jugador trasladada de presentación a aplicación (`PlayerControlService`, `PlayerFrameInput`, `GameSession.advance`), [ADR-001](adr/ADR-001-estilo-recuperacion.md), diagramas C4, flujo de caja negra por consola, mutación con PIT y carga con SLO previo. | [Arquitectura](arquitectura.md), [ADR-001](adr/ADR-001-estilo-recuperacion.md), [pruebas](pruebas.md), [carga](recuperacion-c2/carga.md), [correcciones](recuperacion-c2/correcciones.md) |
| 3. Entrega continua y segura | Contenedores, pipeline y seguridad | Dockerfiles, Compose y variables de entorno; integración continua con cobertura, reglas de arquitectura y controles de seguridad; despliegue continuo con reversión; servidor headless y multijugador según el [ADR-002](adr/ADR-002-despliegue.md); guía de implementación. Previsto — responsable: Persona 1 | [devops](devops.md), [seguridad](seguridad.md), [implementación](implementacion.md), [ADR-002](adr/ADR-002-despliegue.md), [ADR-003](adr/ADR-003-seguridad.md) |

Cifras de referencia del Corte 2 sobre el candidato de entrega: 199 pruebas unitarias y 148 de integración, sin fallos, errores ni omitidas; cobertura de líneas del dominio con pruebas unitarias 887/984 (90,14 %) ([pruebas](pruebas.md#resultados-por-nivel)); SLO-CARGA-01 cumplido con 80 zombis en las tres repeticiones (p95 ≤ 1,926 ms) en el equipo medido ([carga](recuperacion-c2/carga.md#6-resultados)). Son resultados históricos del Corte 2: los del Corte 3 se registran aparte, con la ejecución de Actions que los respalda.

## 2. Decisiones mantenidas y cambiadas

| Decisión | Corte | Estado | Por qué | Fuente |
| --- | --- | --- | --- | --- |
| Factory, Observer y Singleton | 1 | Se mantienen | Siguen siendo apoyos del dominio; la arquitectura los declara y no los presenta como hexagonal completo. | [Arquitectura §3](arquitectura.md#3-estilo-y-alternativas) |
| Persistencia JSON v1 en archivos locales | 1 | Se mantiene | El Corte 2 no cambió el formato; el Corte 3 conserva JSON v1 y los mundos locales. | [ADR-001](adr/ADR-001-estilo-recuperacion.md#6-consecuencias) |
| Estilo en capas con frontera de aplicación explícita | 2 | Se mantiene | Elegido frente a puertos y adaptadores completos por proporción y riesgo de regresión; se revisa si aparece entrada de red o multijugador. | [ADR-001](adr/ADR-001-estilo-recuperacion.md#4-comparación) |
| Coordinación del jugador en la presentación (`GameInput` con callback) | 1 y 2 | **Cambió** en la recuperación del Corte 2 | La física y la estamina avanzaban desde la vista; se trasladó a aplicación para probarlas sin ventana y tener una sola entrada de avance. | [ADR-001](adr/ADR-001-estilo-recuperacion.md#1-contexto), [correcciones](recuperacion-c2/correcciones.md) |
| Aplicación de un solo proceso y un solo jugador | 1 y 2 | **Cambia** en el Corte 3 (propuesto) | El enunciado pide un componente de servidor al que dirigir humo y carga; se propone un servidor con varios jugadores. Pendiente de confirmación del profesor. | [ADR-002](adr/ADR-002-despliegue.md) |
| Reglas de dependencia de ADR-001 | 2 | Se mantienen y se ejecutan en el pipeline | Pasan de comprobarse a mano a detener el pipeline. Previsto — responsable: Persona 2 | [`ArchitectureBoundaryTest`](../test/architecture/ArchitectureBoundaryTest.java) |
| Inversión de dependencias de `WorldStorage` | 2 | Revisión pendiente | ADR-001 la declara incompleta y prevé reevaluarla si aparece entrada de red o multijugador; este corte introduce ambas. Previsto — responsable: Persona 1 | [ADR-001 §6](adr/ADR-001-estilo-recuperacion.md#cuándo-revisar-esta-decisión) |

## 3. Pendientes del Corte 2

La tabla parte de las observaciones del [índice de correcciones](recuperacion-c2/correcciones.md) de la recuperación del Corte 2, que es la fuente de cada fila, y se extiende con las del Corte 3 cuando se resuelvan. «Corregido (Corte 2)» significa lo que esa fuente registra; «revisión pendiente» indica que la corrección se vuelve a comprobar sobre el último commit de `main` del Corte 3 antes de declararla resuelta aquí. Responsable de la revisión: Persona 1.

| Requisito | Fuente | Estado | Corrección | Evidencia |
| --- | --- | --- | --- | --- |
| Tabla de trazabilidad en el README | [correcciones](recuperacion-c2/correcciones.md), fila «Tabla de trazabilidad ausente del README» | Corregido (Corte 2); revisión pendiente | Tabla de seis columnas: un reto oral desglosado en rendimiento, modificabilidad, testabilidad y mantenibilidad. El Corte 3 la amplía con la etapa del pipeline y el umbral. | [README raíz](../../README.md#tabla-de-trazabilidad) |
| La física y el movimiento no debían coordinarse desde `GameInput` | [correcciones](recuperacion-c2/correcciones.md), fila «Física/movimiento coordinados desde GameInput» | Corregido (Corte 2); revisión pendiente | Entrada inmutable, coordinación en aplicación y sin callback `Runnable`. | [GameInput](../src/presentation/game/GameInput.java), [GameSession](../src/application/GameSession.java), [PlayerControlService](../src/application/PlayerControlService.java); `mvn clean verify` y smoke OpenGL del README, aprobados según la fuente |
| Conservar pausa, muerte y reaparición | [correcciones](recuperacion-c2/correcciones.md), fila «Pausa, muerte y reaparición debían conservarse» | Corregido (Corte 2); revisión pendiente | La sesión congela el tick; el respawn restablece controles y conserva material y pausa. | [GameSessionTest](../test/application/GameSessionTest.java), [PlayerControlStaminaTest](../test/application/PlayerControlStaminaTest.java) |
| Conservar el combate y la prioridad de acciones | [correcciones](recuperacion-c2/correcciones.md), fila «Debía preservarse combate y prioridad de acciones» | Corregido (Corte 2); revisión pendiente | Mirada y movimiento antes de la acción; pistola, melee y bloque conservan su prioridad. | [PlayerControlServiceTest](../test/application/PlayerControlServiceTest.java) |
| Cobertura del dominio y separación de pruebas | [correcciones](recuperacion-c2/correcciones.md), fila «Cobertura de dominio y separación de pruebas ausentes» | Corregido (Corte 2); revisión pendiente | JaCoCo del dominio y selecciones disjuntas de Surefire y Failsafe. El Corte 3 añade el umbral que rompe la construcción (Previsto — responsable: Persona 1). | [pom.xml](../pom.xml), [pruebas](pruebas.md) |
| Cifras y documentación dispersas | [correcciones](recuperacion-c2/correcciones.md), fila «Cifras y documentación dispersas» | Corregido (Corte 2); revisión pendiente | Arquitectura y reporte de pruebas canónicos, con las etapas anteriores marcadas como históricas. | [arquitectura](arquitectura.md), [pruebas](pruebas.md), [Wiki](../../Wiki.md) |
| Comparación y ADR formal | [correcciones](recuperacion-c2/correcciones.md), fila «Sin comparación/ADR formal» | Corregido (Corte 2); revisión pendiente | Comparación de estilos y ADR-001 contrastados con la implementación. | [Arquitectura §3](arquitectura.md#3-estilo-y-alternativas), [ADR-001](adr/ADR-001-estilo-recuperacion.md) |
| Contexto, componentes y UML actualizados | [correcciones](recuperacion-c2/correcciones.md), fila «Sin contexto/componentes completos y UML desactualizado» | Corregido (Corte 2); revisión pendiente | UML previo marcado como histórico; contexto, contenedores y tres etapas de componentes. El Corte 3 añade la vista de despliegue (Previsto — responsable: Persona 3). | [Arquitectura §4](arquitectura.md#4-arquitectura-inicial-y-evolucionada), [UML](uml.md), [diagramas](diagramas/recuperacion-c2/README.md) |
| Flujo de caja negra ausente | [correcciones](recuperacion-c2/correcciones.md), fila «Flujo GameFlowSmoke ausente» | Corregido (Corte 2); revisión pendiente | `GameFlowSmokeIT` ejecuta tres escenarios del JAR mediante la consola pública. | [GameFlowSmokeIT](../test/presentation/GameFlowSmokeIT.java) |
| Carga sin evidencia completa | [correcciones](recuperacion-c2/correcciones.md), fila «Carga sin evidencia completa de recuperación» | Corregido (Corte 2); revisión pendiente | Scripts y protocolo en `perf/`; baseline y estrés con SLO previo; el perfilado identificó el índice de chunks y se corrigió con una regresión. Medición headless, sin FPS gráficos. | [carga §6–§7](recuperacion-c2/carga.md#6-resultados), [perf/results](../perf/results/README.md), [WorldChunkIndexTest](../test/domain/world/WorldChunkIndexTest.java) |
| Texto privado en entregables | [correcciones](recuperacion-c2/correcciones.md), fila «Texto privado en entregables» | Corregido (Corte 2); revisión pendiente | Exclusiones conservadas y reportes seleccionados sin rutas personales. | Revisión de archivos e índice antes de publicar; la revisión se repite en la entrega del Corte 3 (Previsto — responsable: Persona 1) |
| Retroalimentación original del Corte 2 | No hay un archivo con esa retroalimentación en el repositorio. | Revisión pendiente | Previsto — responsable: Persona 1: incorporar y clasificar los puntos que no estén en las filas anteriores. | Previsto — responsable: Persona 1 |

Ajustes de jugabilidad del Corte 2, ya aplicados: el registro del [26 de septiembre](corte2/testing/feedback-20260926.md) describe cambios en menús, selección de GPU, estamina, dificultad, ataque de los zombis y mensajes de muerte. Es un registro histórico de esa etapa; no deja pendientes para el Corte 3.

## 4. Límites conocidos del sistema final

Los límites del Corte 2 se conservan: no se declaran resueltos.

| Límite | Estado | Fuente |
| --- | --- | --- |
| Mundo finito completo en memoria; no hay streaming ni arquitectura distribuida. | Se conserva. En el Corte 3 se añade un servidor, pero el mundo sigue viviendo en la memoria del proceso. | [Arquitectura §7](arquitectura.md#7-límites-y-tercer-corte) |
| Un solo jugador y estado global de `WorldManager`. | Cambia con el multijugador propuesto en el ADR-002. Previsto — responsable: Persona 1 | [Arquitectura §7](arquitectura.md#7-límites-y-tercer-corte), [ADR-002](adr/ADR-002-despliegue.md) |
| `application` depende de `persistence.WorldStorage`: la inversión de dependencias está incompleta. | Se conserva; revisión pendiente (ver la sección 2). | [ADR-001 §6](adr/ADR-001-estilo-recuperacion.md#6-consecuencias) |
| Ciclo `domain.world` ↔ `domain.player`, Singleton `WorldManager` y apoyo en Factory, Observer y Singleton. | Se conserva. | [ADR-001 §6](adr/ADR-001-estilo-recuperacion.md#6-consecuencias) |
| `presentation.game.GraphicalGame` importa `bootstrap.GpuPreference`. | Se conserva como dependencia declarada. | [ADR-001 §6](adr/ADR-001-estilo-recuperacion.md#6-consecuencias) |
| Validación gráfica y humana: el smoke OpenGL es automatizado y no es una sesión con personas, una evaluación de usabilidad ni una medición de FPS bajo carga; macOS no se ha comprobado. | Se conserva. | [Pruebas](pruebas.md#comprobación-gráfica) |
| Carga headless de la IA, medida en un solo equipo Windows y con población fija; el coste por tick de `PlayerFrameInput` no se midió. | Se conserva. | [Arquitectura §7](arquitectura.md#7-límites-y-tercer-corte), [carga](recuperacion-c2/carga.md) |
| Rendimiento pendiente de la IA: cachear la caminabilidad durante A\*, escalonar o compartir rutas e indexar los zombis por celda. | Se conserva. | [Arquitectura §7](arquitectura.md#7-límites-y-tercer-corte) |

Límites nuevos del Corte 3, por confirmar con la evidencia final. Previsto — responsable: Persona 1:

| Límite previsto | Origen |
| --- | --- |
| El SLO se verifica en un equipo del grupo y con una población fija; no equivale a un entorno de producción. | [ADR-002](adr/ADR-002-despliegue.md) |
| El front dibuja con OpenGL por software dentro de un contenedor; su rendimiento no está medido. | [ADR-002](adr/ADR-002-despliegue.md) |
| Protocolo TCP y noVNC sin autenticación de jugador; el multijugador solo se prueba en la misma red. | [seguridad](seguridad.md#21-autenticación-y-autorización) |
| El runner autohospedado depende de la disponibilidad de un equipo del grupo y es un riesgo residual de seguridad. | [seguridad](seguridad.md), [ADR-003](adr/ADR-003-seguridad.md) |
| Lo que se haga distinto con más tiempo. | Previsto — responsable: Persona 1: se redacta con la decisión del equipo; no se completa sin ella. |
