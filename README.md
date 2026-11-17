# Minecraft2

[![ci](https://github.com/MarshallGomez1103/Mc2/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/MarshallGomez1103/Mc2/actions/workflows/ci.yml)

Videojuego voxel de escritorio desarrollado en Java 17, Maven y LibGDX por Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz.

## Plan de entrega del Corte 3

El [plan y checklist del Corte 3](Minecraft2/README.md#corte-3) reúne los requisitos del enunciado y la rúbrica, frentes para repartir entre el equipo, dependencias, entregables y evidencias de aceptación. Incluye Kubernetes, observabilidad y DAST; la bonificación conjunta está limitada a 10 puntos. La lista de comprobación por persona, el contrato común de nombres y el calendario están en el [TODO](Minecraft2/TODO.md).

**Estado: Corte 3 en curso, con los frentes asignados a las tres personas del equipo.** `main` ya tiene el pipeline de integración continua en verde ([ejecución 43959899398](https://github.com/MarshallGomez1103/Mc2/actions/runs/43959899398), commit `fa52cbb`, fusión del pull request #6: cinco jobs correctos) y está protegida: pull request con una aprobación y cinco checks obligatorios, sin push directo. También tiene el servidor headless con protocolo v1, el cliente remoto y el multijugador de hasta ocho conexiones ([servidor](Minecraft2/docs/servidor.md), [cliente de red](Minecraft2/docs/cliente-red.md), [evidencia gráfica y de carga local](Minecraft2/docs/evidencias/fase-c/README.md)). Tiene además los contenedores (servidor y front), el despliegue continuo con reversión y la carga contra el despliegue ([devops](Minecraft2/docs/devops.md), [implementación](Minecraft2/docs/implementacion.md)). Fecha límite: lunes 16 de noviembre de 2026, 23:59; se evalúa el último commit de `main` anterior a esa hora y su pipeline debe terminar en verde. Los resultados del Corte 2 conservan su alcance histórico.

**Alcance acordado de palabra con el profesor (confirmación escrita pendiente).** El sistema se separa en dos contenedores: `mc2-server` (servidor del juego, sin interfaz gráfica) y `mc2-front` (cliente LibGDX accesible desde el navegador mediante noVNC). El SLO se verifica sobre la comunicación del front con el back, la ejecución es local y el multijugador funciona en la misma red compartiendo el contenedor del front. El cambio de arquitectura se registra en el [ADR-002](Minecraft2/docs/adr/ADR-002-despliegue.md). Se conserva el SLO-CARGA-01 del Corte 2 (su confirmación escrita sigue pendiente) y el equipo define el SLO-RED-01 para la comunicación front→back; sus umbrales están en [devops.md](Minecraft2/docs/devops.md#umbrales-del-slo-en-el-despliegue).

**Orden de trabajo (obligatorio).** Primero, el esqueleto de una sola sesión con `main` verde; después, el multijugador y las skins; al final, los adicionales. Si el pipeline de `main` no termina en verde en el último commit antes de la fecha límite, los criterios de integración continua, arquitectura verificada y despliegue continuo quedan como máximo en nivel Básico (Rúbrica Corte 3, sección 6).

## Asignación del Corte 3

Cada integrante encuentra su trabajo bajo el encabezado de su persona. Los identificadores `C3-xx` son los del [plan del Corte 3](Minecraft2/README.md#corte-3). Las rutas, clases y pruebas marcadas como `previsto` todavía no existen: las crea cada persona en su rama y este README solo las lista. Las que ya existen en `main` no llevan marca. Los puertos, variables de entorno, servicios, protocolo y métricas están fijados en el contrato común del [TODO](Minecraft2/TODO.md) y no cambian sin avisar al equipo.

**Skins.** Hay tres skins de jugador (`skinId` 0–2, una por integrante). La persona 1 añade `Player.skinId` en el dominio y lo cablea en `VoxelGame`; la persona 2 lo transporta en el protocolo (`hello` y `state`) y lo dibuja con la clase de presentación `RemotePlayerRenderer`.

### Persona 1 — Thomas

- **Rama:** `c3/thomas`.
- **Frentes:** C3-00 (confirmación por escrito del alcance, revisión del ADR-002, multijugador y skin en dominio/aplicación), C3-01 (pendientes del Corte 2 y `evolucion.md`), C3-03 (integración continua y protección de `main`), C3-05.4 (cableado de los cuatro controles en el workflow), C3-10 (README raíz, `evolucion.md`, `implementacion.md`) y C3-11 (integración final y presentación).
- **Archivos de escritura exclusiva:** `Minecraft2/pom.xml`, `.github/workflows/ci.yml`, `.github/dependabot.yml`, `scripts/sca.sh`, `scripts/secretos.sh`, `scripts/escaneo-imagen.sh`, `scripts/umbral-sast.sh`, `README.md` (raíz; Jasub edita solo el bloque de la tabla de trazabilidad), `.gitignore`, `Minecraft2/docs/evolucion.md`, `Minecraft2/docs/implementacion.md` (existe como esqueleto), y en dominio y aplicación las clases que hoy usan `world.getPlayer()`: `World`, `Player`, `GameSession`, `EnemyUpdateService`, `PistolService`, `PlayerControlService` y el cableado en `VoxelGame`.
- **Primera prueba roja a escribir (CP2):** `WorldMultiPlayerTest` y después `GameSessionMultiPlayerTest` (ambas ya están en `main`). El primer entregable (CP1) fue `ci.yml` con los gates y el umbral de cobertura de `domain/**`, ya activo en `main`.
- **Comando de aceptación:** `cd Minecraft2 && mvn -B clean verify` con JDK 17 (unitarias, integración, gate de cobertura ≥ 70 % de líneas de `domain/**` y reglas de arquitectura); durante el desarrollo, `mvn -B test -Dtest=WorldMultiPlayerTest,GameSessionMultiPlayerTest`. En GitHub, un check verde por etapa en el pull request.
- **Criterio de la rúbrica que alimenta:** Integración continua (15), el cableado de los gates de DevSecOps (20), Documentación y repositorio (10), Guía de implementación (10) y apoyo a la presentación (10).
- **Dependencias:** el servidor de una sola sesión con `/health` (Jasub, CP1) y los Dockerfiles (Ethian) para el job de imagen; el multijugador del dominio debe integrarse antes del protocolo con varios jugadores de Jasub.
- **Evidencia:** ejecuciones de Actions por push y pull request, pull requests aprobados por otro integrante, regla de protección de `main`, reportes de pruebas y cobertura como artefactos, duración del pipeline < 10 minutos.
- **Coevaluaciones:** entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.

### Persona 2 — Jasub

- **Rama:** `c3/jasub`.
- **Frentes:** adaptador de red, arranque headless del servidor (incluidos `/health` y `/metrics`) y cliente, con protocolo v1; integración y humo de caja negra; C3-04 (reglas de arquitectura y tabla de trazabilidad de ocho columnas); C3-05 (STRIDE, `seguridad.md`, ADR de seguridad, registro de hallazgos y pull request negativo); C3-09 (DAST con ZAP) y `pruebas.md`.
- **Archivos de escritura exclusiva:** el paquete `network` (`Minecraft2/src/network/`) y el arranque del servidor (`bootstrap.ServerBootstrap`), `RemotePlayerRenderer` en presentación (las tres skins son tres colores definidos en esa clase, sin recursos de imagen), `Minecraft2/test/architecture/ArchitectureBoundaryTest.java` (se amplía), `Minecraft2/test/presentation/GameFlowSmokeIT.java` (se amplía), `Minecraft2/docs/seguridad.md`, `Minecraft2/docs/adr/ADR-003-seguridad.md`, `Minecraft2/docs/pruebas.md` y, en el README raíz, el bloque de la tabla de trazabilidad. El paso de ZAP se entrega como fragmento para que Ethian lo cablee en `cd.yml`.
- **Primera prueba roja a escribir (CP1):** `ProtocolCodecTest`; después `GameServerIT` y `RemotePlayerRendererTest` (las tres ya están en `main`) y una regla nueva de `ArchitectureBoundaryTest` para la frontera de red (`previsto`: `ArchitectureBoundaryTest` todavía no tiene ninguna regla sobre `network`).
- **Comando de aceptación:** `cd Minecraft2 && mvn -B test -Dtest=ProtocolCodecTest` y `mvn -B verify -DskipUnitTests=true -Dit.test=GameServerIT`; los dos deben pasar y, al introducir a propósito una dependencia prohibida, la regla de arquitectura debe romper el job.
- **Criterio de la rúbrica que alimenta:** Arquitectura verificada (15; reglas y tabla), DevSecOps (20; modelo STRIDE, hallazgos y pull request bloqueado), DAST (+5) y Documentación (10; `seguridad.md` y ADR de seguridad).
- **Dependencias:** `World` y servicios con varios jugadores (Thomas) para el protocolo con varios jugadores; `pom.xml` y workflows pasan por Thomas; los nombres del contrato común.
- **Evidencia:** check de arquitectura que falla con una infracción, `GameServerIT` ejecutado en Actions, `seguridad.md` con STRIDE (al menos cinco amenazas) y registro de hallazgos, pull request negativo cerrado sin fusionar y enlazado, reporte de ZAP como artefacto.
- **Coevaluaciones:** entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.

### Persona 3 — Ethian

- **Rama:** `c3/ethian`.
- **Frentes:** C3-02 (contenedores, Compose y `.env.example`), C3-04.3 a C3-04.6 (script de carga y umbrales del SLO), C3-06 (CD, registro, despliegue, reversión, versiones y Releases), C3-07 (Kubernetes), C3-08 (observabilidad), `devops.md`, ADR-002 y vista de despliegue.
- **Archivos de escritura exclusiva:** `Minecraft2/Dockerfile` (se ajusta al servidor), `Minecraft2/Dockerfile.front`, `Minecraft2/.dockerignore`, `docker-compose.yml`, `.env.example`, `.github/workflows/cd.yml`, `.github/workflows/load.yml`, `scripts/deploy.sh`, `scripts/rollback.sh`, `CHANGELOG.md`, `Minecraft2/perf/`, `k8s/`, `observabilidad/`, `Minecraft2/docs/devops.md`, `Minecraft2/docs/adr/ADR-002-despliegue.md` (con Thomas, que confirma el alcance con el profesor) y `Minecraft2/docs/arquitectura.md` (vista de despliegue); todos `previsto` salvo los que ya existen.
- **Primera prueba roja a escribir (CP1):** la prueba del script de carga `LoadSloGate` (`LoadSloGateTest`, `previsto`): debe fallar con código distinto de cero cuando falta un resultado, no se alcanza la población o se rompe un umbral.
- **Comando de aceptación:** desde la raíz del repositorio, `cp .env.example .env && docker compose up --build --wait`, con `mc2-server` y `mc2-front` en estado saludable y `curl -fsS http://localhost:8080/health` correcto; la carga se ejecuta contra ese entorno con `MC2_LOAD_ZOMBIES=80`.
- **Criterio de la rúbrica que alimenta:** Contenerización (5), Despliegue continuo (15), la parte de SLO desplegado de Arquitectura verificada (15), Documentación (10; `devops.md` y vista de despliegue) y las bonificaciones de Kubernetes y observabilidad (+5 cada una, máximo conjunto +10).
- **Dependencias:** el servidor con `/health`, `/metrics` y el cliente de red de Jasub (CP1) para el healthcheck y la carga; `ci.yml` de Thomas para los nombres de checks; los adicionales solo empiezan con CI y CD en verde.
- **Evidencia:** `docker compose up` desde copia limpia, ejecución de `cd.yml` en Actions con imagen en el registro etiquetada con SHA y versión, prueba de humo y carga después del despliegue, reversión ejecutada una vez, tag `v1.0.0` con Release y JAR del cliente, y evidencia independiente de cada adicional.
- **Coevaluaciones:** entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.

## Cómo empezar

Pasos válidos para cualquier integrante:

1. Leer la sección de su persona y la lista de comprobación de su persona en el [TODO](Minecraft2/TODO.md), incluido el contrato común.
2. Comprobar el entorno: JDK 17 (`mvn -version`), Git y Docker.
3. Configurar su propia identidad de Git (`git config user.name` y `git config user.email`, con el correo `noreply` de GitHub) y crear su rama desde `main`: `git fetch origin && git switch -c c3/<nombre> origin/main`.
4. Trabajar con TDD: un commit con la prueba que falla y otro con el cambio mínimo que la hace pasar, firmados por quien los escribe. Los commits rojos viven en la rama; `main` solo recibe pull requests cuyo último commit pasa todos los checks.
5. Antes de abrir el pull request, ejecutar `cd Minecraft2 && mvn -B clean verify` y revisar el diff completo.
6. Abrir el pull request hacia `main`; lo aprueba otro integrante. No se hace push directo a `main`.
7. Respetar los archivos calientes: `Minecraft2/pom.xml`, `.github/workflows/ci.yml`, el README raíz y `.gitignore` son de Thomas; `cd.yml` y `load.yml` son de Ethian. Quien necesite un cambio en un archivo ajeno lo pide en el pull request con el fragmento exacto y el dueño lo incorpora. Excepciones: cada persona marca solo su propia sección del TODO y Jasub edita el bloque de la tabla de trazabilidad.
8. Marcar `[x]` en el TODO solo con evidencia real (enlace a la ejecución, al pull request o al commit).
9. Antes de cada commit y de la entrega, revisar el control del equipo: requisito literal de la rúbrica, archivos completos, rutas personales, marcadores sin resolver, enlaces, cifras con su fecha y commit, y que ninguna prueba omitida se presente como aprobada.

## Orden de integración

Cada persona integra su trabajo en su rama y abre su pull request, aprobado por otro integrante. Entra primero la persona 1 (Thomas); después la persona 2 (Jasub), con rebase sobre `main`; por último la persona 3 (Ethian), también con rebase. Quien integra el último reúne todo, ejecuta `mvn clean verify` y el pipeline completo, y deja la versión final en `main` sin conflictos.

## Artefacto de la rúbrica → ruta

La rúbrica sugiere los documentos bajo `docs/` en la raíz del repositorio. Este proyecto mantiene la documentación en `Minecraft2/docs/`; el README de la raíz es el punto de entrada y enlaza cada documento. **Previsto** significa que el archivo todavía no existe; solo se enlazan los existentes.

| Artefacto de la rúbrica | Ruta real | Responsable | Estado |
| --- | --- | --- | --- |
| README (badge, URL, Compose, pipeline, trazabilidad) | [README.md](README.md) (raíz) | Thomas | Existe; el badge del pipeline está en la cabecera; la URL del despliegue está `previsto` |
| Dockerfile | [Minecraft2/Dockerfile](Minecraft2/Dockerfile) (servidor) y `Minecraft2/Dockerfile.front` | Ethian | Existen los dos: el del servidor (modo consola) y el del front (noVNC) |
| .dockerignore | [Minecraft2/.dockerignore](Minecraft2/.dockerignore) | Ethian | Existe |
| docker-compose.yml | `docker-compose.yml` (raíz, contexto `./Minecraft2`) | Ethian | Existe |
| .env.example | `.env.example` (raíz) | Ethian | Existe |
| Workflow de integración continua | [.github/workflows/ci.yml](.github/workflows/ci.yml) | Thomas | Existe y está activo en `main` (cinco jobs) |
| Workflows de despliegue y carga | `.github/workflows/cd.yml`, `.github/workflows/load.yml` | Ethian | Existe |
| Dependabot | [.github/dependabot.yml](.github/dependabot.yml) | Thomas | Existe y está en `main` ([pull request #3](https://github.com/MarshallGomez1103/Mc2/pull/3)) |
| CHANGELOG.md | `CHANGELOG.md` (raíz) | Ethian | Existe |
| docs/devops.md | [Minecraft2/docs/devops.md](Minecraft2/docs/devops.md) | Ethian, con Thomas | Existe: integración continua, despliegue, carga y reversión documentados con ejecuciones reales |
| docs/seguridad.md | [Minecraft2/docs/seguridad.md](Minecraft2/docs/seguridad.md) | Jasub, con Thomas | Existe (parcial): los cuatro controles corren en el pipeline y hay dos demostraciones de bloqueo; el resto se completa con ejecuciones reales |
| docs/evolucion.md | [Minecraft2/docs/evolucion.md](Minecraft2/docs/evolucion.md) | Thomas | Existe en parte: Cortes 1 y 2 y pendientes del Corte 2 redactados; lo del Corte 3 que depende del despliegue sigue `previsto` |
| docs/implementacion.md | [Minecraft2/docs/implementacion.md](Minecraft2/docs/implementacion.md) | Thomas | Completa: nueve partes, dos escenarios de costos y problemas reales; los pasos no se repitieron desde una máquina limpia ajena al grupo |
| Vista de despliegue | sección nueva de [Minecraft2/docs/arquitectura.md](Minecraft2/docs/arquitectura.md) | Ethian | Existe (sección 8) |
| docs/pruebas.md | [Minecraft2/docs/pruebas.md](Minecraft2/docs/pruebas.md) | Jasub | Existe; incluye las secciones del servidor (fase B) y del cliente y el multijugador (fase C) |
| ADR de despliegue (ADR-002) | [Minecraft2/docs/adr/ADR-002-despliegue.md](Minecraft2/docs/adr/ADR-002-despliegue.md) | Ethian, con Thomas | Existe como propuesta; el equipo define el SLO-RED-01 y el alcance sigue pendiente de confirmación escrita con el profesor |
| ADR de seguridad (ADR-003) | [Minecraft2/docs/adr/ADR-003-seguridad.md](Minecraft2/docs/adr/ADR-003-seguridad.md) | Jasub, con Thomas | Existe como propuesta; las herramientas se confirman al probarlas |
| ADR del Corte 2 (antecedente) | [Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md) | — | Existe |
| k8s/ (bonificación) | `k8s/` | Ethian | Existe |
| observabilidad/ (bonificación) | `observabilidad/` | Ethian | Existe |
| perf/ | [Minecraft2/perf/](Minecraft2/perf/README.md) | Ethian | Existe (Corte 2); se amplía con el SLO desplegado |

## Recuperación del Corte 2

El candidato común integra Thomas (`e730e9c`), Jasub (`b7c95a3`, `jasub/final-a`) y Ethian (`d186d46`). Controles, cobertura, flujo público, ADR y diagramas están incorporados y comprobados. Las corridas de carga de recuperación (baseline y estrés) y su análisis están incorporados: el SLO de 80 zombis se cumple en el equipo medido, y el perfilado identificó y corrigió el cuello de botella del índice de chunks. [Correcciones y estado](Minecraft2/docs/recuperacion-c2/correcciones.md).

| Integrante | Rama | Responsabilidad |
| --- | --- | --- |
| Thomas | `feature/c2-Thomas` | Controles en aplicación, regresiones, cobertura, build, documentación e integración de los tres frentes |
| Jasub | `jasub/final-a` | Integración, flujo público de caja negra y comprobación de fronteras |
| Ethian | `feature/c2-Ethian` | Comparación de estilos, ADR, contexto/componentes, carga y cierre de entrega |

El plan de recuperación del Corte 2 (archivado) y el [TODO](Minecraft2/TODO.md) delimitaron los archivos de cada frente. La integración conserva el historial de las tres ramas y un respaldo de la base. Ethian completó la carga y la verificación técnica del candidato. Los criterios de cierre están en [pruebas](Minecraft2/docs/pruebas.md#verificación-del-candidato-de-entrega).

## Tabla de trazabilidad

El reto comunicado oralmente fue **incorporar un enemigo** (zombis que perciben, persiguen y atacan al jugador). Cada fila es un aspecto de ese mismo reto y conserva las ocho columnas acordadas. Los resultados históricos del Corte 2 se distinguen de la verificación del servidor/cliente del Corte 3. La configuración de un gate no demuestra por sí sola que el despliegue final ya lo ejecutó.

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado | Etapa del pipeline | Umbral que detiene el pipeline |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Incorporar un enemigo: actualizar muchos zombis sin exceder el presupuesto de cálculo | Rendimiento | IA headless en dominio, repath acotado y A\* con presupuesto; servidor autoritativo con métricas de tick y ack por conexión | [EnemyUpdateService](Minecraft2/src/application/EnemyUpdateService.java), [AStarPathfinder](Minecraft2/src/domain/enemy/AStarPathfinder.java), [World](Minecraft2/src/domain/world/World.java), [GameServer](Minecraft2/src/network/GameServer.java), [ServerMetrics](Minecraft2/src/network/ServerMetrics.java) | [Regresión del índice de chunks](Minecraft2/test/domain/world/WorldChunkIndexTest.java); [carga histórica C2](Minecraft2/docs/recuperacion-c2/carga.md); [CSV y condiciones de tres clientes C3](Minecraft2/docs/evidencias/fase-c/README.md#carga-local-complementaria-de-tres-clientes) | C2: 80 zombis, tick p95 1,759 / 1,926 / 1,679 ms en harness JVM. C3, código funcional `f545114`, servidor Docker y tres clientes de prueba: tick p95 10,409 / 7,898 / 5,993 ms; RTT p95 30,012 / 54,946 / 50,063 ms; 80 vivos y cero fallos/déficit/errores. Calentamiento de 30 s por repetición y frentes gráficos detenidos. No prueba FPS ni CD frío | CI `build-pruebas`: regresión del índice. **Pendiente de integración con PR #2:** CD → readiness → humo → carga contra servidor desplegado. La carga local anterior no se atribuye a Actions | Carga: en cada una de tres repeticiones, p95 tick >16,67 ms, cualquier tick fallido o déficit con 80 zombis. Red: p95 entrada→ack >100 ms o errores en el consumidor de Ethian. El umbral de red es la decisión del equipo aplicada a la herramienta; su confirmación escrita con el profesor sigue pendiente |
| Incorporar un enemigo: ajustar dificultad y oleadas sin tocar IA ni render | Modificabilidad | Reglas puras en dominio; aplicación coordina oleadas y recibe configuración. La entrada de red depende del núcleo, nunca a la inversa | [WaveRules](Minecraft2/src/domain/enemy/WaveRules.java), [Difficulty](Minecraft2/src/domain/enemy/Difficulty.java), [HordeManager](Minecraft2/src/application/HordeManager.java), [GameSettings](Minecraft2/src/application/GameSettings.java), [ADR-004](Minecraft2/docs/adr/ADR-004-frontera-red.md) | [WaveRulesTest](Minecraft2/test/domain/enemy/WaveRulesTest.java), [DifficultyTest](Minecraft2/test/application/DifficultyTest.java), [HordeManagerTest](Minecraft2/test/application/HordeManagerTest.java), [GameSettingsTest](Minecraft2/test/application/GameSettingsTest.java), [fronteras](Minecraft2/test/architecture/ArchitectureBoundaryTest.java) | Suite local de D aprobada; el escáner rechaza dependencias desde dominio/aplicación hacia transporte. PIT histórico `b5a8e7c`: Difficulty 5/5, GameSettings 14/14, WaveRules 30/32, HordeManager 79/94; no se presenta como mutación ejecutada de nuevo en D. Una dificultad nueva aún exige añadirla al enum | CI `build-pruebas` → `mvn verify` → Surefire/Failsafe, incluidas reglas de arquitectura. Configuración vigente en [ci.yml](.github/workflows/ci.yml); ejecuciones de D se registran en [pruebas.md](Minecraft2/docs/pruebas.md) | Cualquier prueba fallida o dependencia prohibida hace fallar Maven y el check. No hay gate de mutación PIT; no se inventa un umbral |
| Incorporar un enemigo: probar conducta y controles sin ventana ni GPU | Testabilidad | FSM, A\*, física y navegación en dominio; controles como PlayerFrameInput. Adaptador network sin presentación, bootstrap ni gráficos; excepción WorldStorage declarada | [ZombieStateMachine](Minecraft2/src/domain/enemy/ZombieStateMachine.java), [PlayerFrameInput](Minecraft2/src/application/PlayerFrameInput.java), [PlayerControlService](Minecraft2/src/application/PlayerControlService.java), [GameInput](Minecraft2/src/presentation/game/GameInput.java), [ADR-001](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md), [ADR-004](Minecraft2/docs/adr/ADR-004-frontera-red.md) | [FSM](Minecraft2/test/domain/enemy/ZombieStateMachineTest.java), [A\*](Minecraft2/test/domain/enemy/AStarPathfinderTest.java), [controles](Minecraft2/test/application/PlayerControlServiceTest.java), [estamina](Minecraft2/test/application/PlayerControlStaminaTest.java), [ArchitectureBoundaryTest](Minecraft2/test/architecture/ArchitectureBoundaryTest.java) | E local 2026-11-16, corrección `acbff1c`: **279 unitarias +164 integración**, sin fallos/errores/omitidas; dominio 935/1006 líneas (92,94 %), gate ≥70 % activo. [Actions correcto](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976250231) y [fallo controlado](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976266242). Arquitectura: 22 casos, que incluyen cuatro fronteras, fixtures y controles del escáner. Ensayo negativo real: dos dependencias compilables prohibidas produjeron dos fallos; clases retiradas y JAR limpio. Tres casos de seguridad adicionales verifican log, entradas abusivas y límite de conexiones; [registro/STRIDE](Minecraft2/docs/seguridad.md). El escáner de fuentes no detecta toda reflexión ni dependencias transitivas | CI `build-pruebas`: Surefire ejecuta arquitectura y JaCoCo verifica dominio. `ci.yml` comprueba que el XML de arquitectura existe y tiene casos, y que JaCoCo no omitió el gate | Una prueba fallida, import/uso plenamente cualificado prohibido, ausencia de ejecución del gate o cobertura de líneas del dominio <70 %. No se suman coberturas unitaria e integración |
| Incorporar un enemigo sin romper mundo, guardado ni estado de partida | Mantenibilidad | GameSession coordina `advance` local y `advanceAll` del servidor. Jugadores con identidad/vida propia; JSON v1 se conserva y WorldStorage separa el almacenamiento | [GameSession](Minecraft2/src/application/GameSession.java), [WorldApplicationService](Minecraft2/src/application/WorldApplicationService.java), [JsonWorldStorage](Minecraft2/src/persistence/JsonWorldStorage.java), [ServerSimulation](Minecraft2/src/network/ServerSimulation.java) | [GameSessionTest](Minecraft2/test/application/GameSessionTest.java), [regresión de sesión](Minecraft2/test/application/CombatSessionRegressionTest.java), [persistencia](Minecraft2/test/application/LocalWorldPersistenceIT.java), [ciclo del mundo](Minecraft2/test/application/WorldLifecycleIT.java), [GameFlowSmokeIT](Minecraft2/test/presentation/GameFlowSmokeIT.java), [multijugador](Minecraft2/test/network/MultiplayerServerIT.java) | D `d4ad1d1`: los tres flujos públicos de consola y un flujo nuevo del **JAR real** pasaron: crear mundo, servidor headless, salud, dos clientes, acción/ack aislados, bye, métrica 2→1→0 y reingreso sin fantasma. No usa un GameServer en memoria en ese humo. El hijo se inicia con puertos 0 y se detiene al finalizar. Persisten WorldStorage en persistence, ciclo world↔player y acoplamiento network/JSON declarado | CI `build-pruebas` → package → Failsafe `integration-test` y `verify`. Se verifica ejecución efectiva en XML. **Pendiente:** humo contra el despliegue de main en CD | Fallo/error del humo o regresión, proceso que no arranca/responde, identidad/ack incorrectos, contador que no refleja bajas o persistencia inválida. Failsafe bloquea verify: [ensayo remoto](https://github.com/MarshallGomez1103/Mc2/actions/runs/43976623068), 162 casos y un fallo deliberado |

Las verificaciones del código se ejecutan en CI; despliegue/humo/carga automáticos se cierran con el trabajo de Ethian y la aceptación final. Las evidencias y SHA específicos se registran en [pruebas.md](Minecraft2/docs/pruebas.md). Las capturas gráficas son apoyo, no sustituyen ejecuciones de Actions.

**Evaluación humana y límites:** jugabilidad, claridad visual, usabilidad y equilibrio requieren personas; no se atribuye una evaluación con usuarios a los tests. Los smokes OpenGL independientes y la comprobación noVNC de C no forman parte del job headless ni prueban FPS bajo carga. La desconexión gráfica de causa no establecida de C permanece [documentada](Minecraft2/docs/evidencias/fase-c/README.md#incidentes-conservados-y-límites); las nuevas reglas no se presentan como una corrección.

La separación de capas añade contratos y coordinación; no acelera A\* por sí sola ni elimina las deudas declaradas. El SLO se demuestra mediante mediciones en sus condiciones reales.

## Integración continua y seguridad

[`ci.yml`](.github/workflows/ci.yml) se ejecuta en cada pull request hacia `main`, en cada push a `main` y a demanda. Son cinco jobs independientes y cada uno es un check del pull request; ninguno usa `continue-on-error`. El detalle de etapas, umbrales y su justificación está en [devops.md](Minecraft2/docs/devops.md); el modelo de amenazas, los controles y el registro de hallazgos, en [seguridad.md](Minecraft2/docs/seguridad.md).

| Job | Qué hace | Umbral que lo detiene |
| --- | --- | --- |
| `build-pruebas` | `mvn verify` con JDK 17: pruebas unitarias (Surefire), de integración (Failsafe), cobertura con JaCoCo y reglas de arquitectura (`ArchitectureBoundaryTest`, cuyo informe debe existir y traer pruebas) | Falla una prueba o una regla de arquitectura, o la cobertura de líneas de `domain/**` con pruebas unitarias es menor que 70 % |
| `sast` | CodeQL para Java con compilación manual y [`scripts/umbral-sast.sh`](scripts/umbral-sast.sh) | Hallazgo alto o crítico (`security-severity` ≥ 7,0) sin excepción registrada, o ningún archivo Java extraído |
| `sca` | Trivy sobre las dependencias de `pom.xml` ([`scripts/sca.sh`](scripts/sca.sh)) | Vulnerabilidad HIGH o CRITICAL con corrección disponible |
| `secretos` | Gitleaks sobre todo el historial ([`scripts/secretos.sh`](scripts/secretos.sh)) | Cualquier hallazgo: el umbral es cero |
| `imagen` | Construye `Minecraft2/Dockerfile` como `mc2-server:ci` y escanea la imagen ([`scripts/escaneo-imagen.sh`](scripts/escaneo-imagen.sh)) | Vulnerabilidad CRITICAL con corrección disponible; el `docker build` solo empaqueta el JAR: las pruebas y el umbral de cobertura los verifica `build-pruebas` |

**Versiones fijadas.** JDK 17 (Temurin, `actions/setup-java` v6.2.0), Trivy 0.76.2 y Gitleaks 8.30.1 (descarga con verificación SHA-256 en cada ejecución) y `github/codeql-action` v4.39.3, sin fijar por separado el paquete de CodeQL. Las acciones de GitHub están fijadas por SHA de commit. El runner es `ubuntu-latest`.

**Protección de `main`.** El ruleset `proteger-main` está activo: pull request con una aprobación, los cinco checks anteriores como requeridos (con esos nombres exactos) y sin push directo.

**Ejecutarlo en local.** La verificación de `build-pruebas` se reproduce con `cd Minecraft2 && mvn -B clean verify` y JDK 17. Los scripts `sca.sh` y `secretos.sh` solo se ejecutan dentro de GitHub Actions o de un contenedor: se niegan a correr en el equipo anfitrión. `escaneo-imagen.sh` no tiene esa guarda: requiere Docker (o Podman), Linux x86_64 y construye la imagen; el equipo lo ejecuta en CI.

**Bloqueos demostrados.** Dos pull requests de demostración, cerrados sin fusionar:

- Cobertura: [pull request #4](https://github.com/MarshallGomez1103/Mc2/pull/4), con el umbral subido a 0,99. La [ejecución 43736947725](https://github.com/MarshallGomez1103/Mc2/actions/runs/43736947725) falla en `build-pruebas` e `imagen` con «lines covered ratio is 0.92, but expected minimum is 0.99». En esa ejecución `secretos` también falló, pero por la rama de la otra demostración y no por la cobertura ([explicación](Minecraft2/docs/seguridad.md#5-evidencia-pull-request-bloqueado)).
- Secretos: [pull request #5](https://github.com/MarshallGomez1103/Mc2/pull/5), con una credencial falsa. La [ejecución 43736948906](https://github.com/MarshallGomez1103/Mc2/actions/runs/43736948906) falla en `secretos`.

**Demostraciones que faltan.** Una dependencia vulnerable para `sca`, un hallazgo alto para `sast`, una imagen con una vulnerabilidad crítica para `imagen` y una infracción intencional de `ArchitectureBoundaryTest`.

**Despliegue.** Compose, despliegue continuo, carga y reversión están en `main`. En la [ejecución 44005663312](https://github.com/MarshallGomez1103/Mc2/actions/runs/44005663312) el primer intento, en un equipo del grupo, incumplió el SLO de carga y revirtió solo a la versión anterior; el segundo, en el runner hospedado de GitHub, lo cumplió. Detalles en [devops.md](Minecraft2/docs/devops.md) y [implementacion.md](Minecraft2/docs/implementacion.md).

## Compilar y jugar

Requisitos: **JDK 17**, Maven y una sesión gráfica compatible con OpenGL para jugar. Verificar el JDK de Maven con `mvn -version`. Desde la raíz Git:

```bash
cd Minecraft2
mvn clean package
java -Dfile.encoding=UTF-8 -jar target/minecraft2-0.1.0-SNAPSHOT.jar
```

El empaquetado ejecuta unitarias; la verificación completa requiere `verify`. El comando de verificación local requiere JDK 17 (`mvn -version`) y la compilación falla si la cobertura de líneas de `domain/**` con pruebas unitarias baja del 70 % (propiedad `jacoco.domain.line.minimum` del `pom.xml`). [Controles, consola y guardados](Minecraft2/README.md).

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

Surefire genera `target/surefire-reports/`; Failsafe genera `target/failsafe-reports/`. JaCoCo genera `target/site/jacoco-domain-unit/` y `target/site/jacoco-domain-integration/`, con HTML, XML y CSV de `domain/**`. No se suman ambos porcentajes. El único umbral configurado es el de líneas de `domain/**` con pruebas unitarias (≥ 70 %, `jacoco.domain.line.minimum`); la regla `unit-domain-check` lo comprueba en la fase `test` y detiene el pipeline. La cobertura de integración no tiene umbral. Ejecutar un JAR separado no incorpora automáticamente su cobertura al reporte.

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
- [Servidor autoritativo y protocolo v1](Minecraft2/docs/servidor.md): arranque headless, variables `MC2_*`, mensajes, `/health` y `/metrics`.
- [Cliente remoto y mundo compartido](Minecraft2/docs/cliente-red.md) y su [evidencia gráfica y de carga local](Minecraft2/docs/evidencias/fase-c/README.md), con los límites que declara (sin FPS medidos, sin reconexión automática y sin ejecución de despliegue continuo).
- [Índice de correcciones](Minecraft2/docs/recuperacion-c2/correcciones.md).
- [Wiki e índice de etapas](Wiki.md).
- [ADR y comparación de estilos](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md).
- [Contexto, contenedores y evolución de componentes](Minecraft2/docs/diagramas/recuperacion-c2/README.md). El [UML previo](Minecraft2/docs/uml.md) conserva sus versiones históricas.

Mundos finitos en memoria; enemigos, restos, estamina y arma son estado de sesión sin persistencia JSON. Se conserva JSON v1. La prueba gráfica automatizada no acredita equilibrio de jugabilidad, usabilidad ni rendimiento gráfico bajo carga. La carga de recuperación es headless y no acredita FPS gráficos; se midió en un solo equipo Windows.
