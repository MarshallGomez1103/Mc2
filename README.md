# Minecraft2

Videojuego voxel de escritorio desarrollado en Java 17, Maven y LibGDX por Elioth Thomas Gomez Morales, Jasub Sastre y Ethian Daniel White Ortiz.

## Plan de entrega del Corte 3

El [plan y checklist del Corte 3](Minecraft2/README.md#corte-3) reúne los requisitos del enunciado y la rúbrica, frentes para repartir entre el equipo, dependencias, entregables y evidencias de aceptación. Incluye Kubernetes, observabilidad y DAST; la bonificación conjunta está limitada a 10 puntos. La lista de comprobación por persona, el contrato común de nombres y el calendario están en el [TODO](Minecraft2/TODO.md).

**Estado: Corte 3 en curso, con los frentes asignados a las tres personas del equipo.** Fecha límite: lunes 16 de noviembre de 2026, 23:59; se evalúa el último commit de `main` anterior a esa hora y su pipeline debe terminar en verde. Los resultados del Corte 2 conservan su alcance histórico.

**Alcance acordado de palabra con el profesor (confirmación escrita pendiente).** El sistema se separa en dos contenedores: `mc2-server` (servidor del juego, sin interfaz gráfica) y `mc2-front` (cliente LibGDX accesible desde el navegador mediante noVNC). El SLO se verifica sobre la comunicación del front con el back, la ejecución es local y el multijugador funciona en la misma red compartiendo el contenedor del front. El cambio de arquitectura se registra en el ADR-002 (`previsto`). Se conserva el SLO-CARGA-01 del Corte 2 y se propone el SLO-RED-01 para la comunicación front→back; ambos están pendientes de confirmación con el profesor.

**Orden de trabajo (obligatorio).** Primero, el esqueleto de una sola sesión con `main` verde; después, el multijugador y las skins; al final, los adicionales. Si el pipeline de `main` no termina en verde en el último commit antes de la fecha límite, los criterios de integración continua, arquitectura verificada y despliegue continuo quedan como máximo en nivel Básico (Rúbrica Corte 3, sección 6).

## Asignación del Corte 3

Cada integrante encuentra su trabajo bajo el encabezado de su persona. Los identificadores `C3-xx` son los del [plan del Corte 3](Minecraft2/README.md#corte-3). Las rutas, clases y pruebas marcadas como `previsto` todavía no existen: las crea cada persona en su rama y este README solo las lista. Los puertos, variables de entorno, servicios, protocolo y métricas están fijados en el contrato común del [TODO](Minecraft2/TODO.md) y no cambian sin avisar al equipo.

**Skins.** Hay tres skins de jugador (`skinId` 0–2, una por integrante). La persona 1 añade `Player.skinId` en el dominio y lo cablea en `VoxelGame`; la persona 2 lo transporta en el protocolo (`hello` y `state`) y lo dibuja con la clase de presentación `RemotePlayerRenderer`.

### Persona 1 — Thomas

- **Rama:** `c3/thomas`.
- **Frentes:** C3-00 (confirmación por escrito del alcance, revisión del ADR-002, multijugador y skin en dominio/aplicación), C3-01 (pendientes del Corte 2 y `evolucion.md`), C3-03 (integración continua y protección de `main`), C3-05.4 (cableado de los cuatro controles en el workflow), C3-10 (README raíz, `evolucion.md`, `implementacion.md`) y C3-11 (integración final y presentación).
- **Archivos de escritura exclusiva:** `Minecraft2/pom.xml`, `.github/workflows/ci.yml` (`previsto`), `.github/dependabot.yml` (`previsto`), `scripts/sca.sh`, `scripts/secretos.sh`, `scripts/escaneo-imagen.sh`, `scripts/umbral-sast.sh` (`previsto`), `README.md` (raíz; Jasub edita solo el bloque de la tabla de trazabilidad), `.gitignore`, `Minecraft2/docs/evolucion.md`, `Minecraft2/docs/implementacion.md` (`previsto`), y en dominio y aplicación las clases que hoy usan `world.getPlayer()`: `World`, `Player`, `GameSession`, `EnemyUpdateService`, `PistolService`, `PlayerControlService` y el cableado en `VoxelGame`.
- **Primera prueba roja a escribir (CP2):** `WorldMultiPlayerTest` (`previsto`) y después `GameSessionMultiPlayerTest` (`previsto`). El primer entregable (CP1) es `ci.yml` con los gates y el umbral de cobertura de `domain/**`.
- **Comando de aceptación:** `cd Minecraft2 && mvn -B clean verify` con JDK 17 (unitarias, integración, gate de cobertura ≥ 70 % de líneas de `domain/**` y reglas de arquitectura); durante el desarrollo, `mvn -B test -Dtest=WorldMultiPlayerTest,GameSessionMultiPlayerTest`. En GitHub, un check verde por etapa en el pull request.
- **Criterio de la rúbrica que alimenta:** Integración continua (15), el cableado de los gates de DevSecOps (20), Documentación y repositorio (10), Guía de implementación (10) y apoyo a la presentación (10).
- **Dependencias:** el servidor de una sola sesión con `/health` (Jasub, CP1) y los Dockerfiles (Ethian) para el job de imagen; el multijugador del dominio debe integrarse antes del protocolo con varios jugadores de Jasub.
- **Evidencia:** ejecuciones de Actions por push y pull request, pull requests aprobados por otro integrante, regla de protección de `main`, reportes de pruebas y cobertura como artefactos, duración del pipeline < 10 minutos.
- **Coevaluaciones:** entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.

### Persona 2 — Jasub

- **Rama:** `c3/jasub`.
- **Frentes:** adaptador de red, arranque headless del servidor (incluidos `/health` y `/metrics`) y cliente, con protocolo v1; integración y humo de caja negra; C3-04 (reglas de arquitectura y tabla de trazabilidad de ocho columnas); C3-05 (STRIDE, `seguridad.md`, ADR de seguridad, registro de hallazgos y pull request negativo); C3-09 (DAST con ZAP) y `pruebas.md`.
- **Archivos de escritura exclusiva:** el paquete nuevo del adaptador de red y el arranque del servidor (nombre a fijar con el contrato común), `RemotePlayerRenderer` en presentación y sus recursos de skin (`previsto`), `Minecraft2/test/architecture/ArchitectureBoundaryTest.java` (se amplía), `Minecraft2/test/presentation/GameFlowSmokeIT.java` (se amplía), `Minecraft2/docs/seguridad.md`, `Minecraft2/docs/adr/ADR-003-seguridad.md`, `Minecraft2/docs/pruebas.md` y, en el README raíz, el bloque de la tabla de trazabilidad. El paso de ZAP se entrega como fragmento para que Ethian lo cablee en `cd.yml`.
- **Primera prueba roja a escribir (CP1):** `ProtocolCodecTest` (`previsto`); después `GameServerIT`, `RemotePlayerRendererTest` y una regla nueva de `ArchitectureBoundaryTest` para la frontera de red (todas `previsto`).
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
| README (badge, URL, Compose, pipeline, trazabilidad) | [README.md](README.md) (raíz) | Thomas | Existe; el badge y la URL están `previsto` |
| Dockerfile | [Minecraft2/Dockerfile](Minecraft2/Dockerfile) (servidor) y `Minecraft2/Dockerfile.front` | Ethian | Existe (imagen del juego en modo consola); se ajusta al servidor; el del front está `previsto` |
| .dockerignore | [Minecraft2/.dockerignore](Minecraft2/.dockerignore) | Ethian | Existe |
| docker-compose.yml | `docker-compose.yml` (raíz, contexto `./Minecraft2`) | Ethian | `previsto` |
| .env.example | `.env.example` (raíz) | Ethian | `previsto` |
| Workflow de integración continua | `.github/workflows/ci.yml` | Thomas | `previsto` |
| Workflows de despliegue y carga | `.github/workflows/cd.yml`, `.github/workflows/load.yml` | Ethian | `previsto` |
| Dependabot | `.github/dependabot.yml` | Thomas | `previsto` |
| CHANGELOG.md | `CHANGELOG.md` (raíz) | Ethian | `previsto` |
| docs/devops.md | [Minecraft2/docs/devops.md](Minecraft2/docs/devops.md) | Ethian, con Thomas | Existe como esqueleto; se completa con ejecuciones reales |
| docs/seguridad.md | [Minecraft2/docs/seguridad.md](Minecraft2/docs/seguridad.md) | Jasub, con Thomas | Existe como esqueleto; se completa con ejecuciones reales |
| docs/evolucion.md | `Minecraft2/docs/evolucion.md` | Thomas | `previsto` |
| docs/implementacion.md | `Minecraft2/docs/implementacion.md` | Thomas | `previsto` |
| Vista de despliegue | sección nueva de [Minecraft2/docs/arquitectura.md](Minecraft2/docs/arquitectura.md) | Ethian | El archivo existe; la sección está `previsto` |
| docs/pruebas.md | [Minecraft2/docs/pruebas.md](Minecraft2/docs/pruebas.md) | Jasub | Existe; se actualiza |
| ADR de despliegue (ADR-002) | [Minecraft2/docs/adr/ADR-002-despliegue.md](Minecraft2/docs/adr/ADR-002-despliegue.md) | Ethian, con Thomas | Existe como propuesta; pendiente de confirmación con el profesor |
| ADR de seguridad (ADR-003) | [Minecraft2/docs/adr/ADR-003-seguridad.md](Minecraft2/docs/adr/ADR-003-seguridad.md) | Jasub, con Thomas | Existe como propuesta; las herramientas se confirman al probarlas |
| ADR del Corte 2 (antecedente) | [Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md) | — | Existe |
| k8s/ (bonificación) | `k8s/` | Ethian | `previsto` |
| observabilidad/ (bonificación) | `observabilidad/` | Ethian | `previsto` |
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

El reto comunicado oralmente fue **incorporar un enemigo** (zombis que perciben, persiguen y atacan al jugador). No hay una lista escrita adicional de retos. Cada fila es un aspecto de ese mismo reto con su atributo de calidad, su decisión y su evidencia; no son retos nuevos.

| Reto | Atributo de calidad | Decisión arquitectónica | Dónde está | Prueba | Resultado | Etapa del pipeline | Umbral que detiene el pipeline |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Incorporar un enemigo: actualizar muchos zombis por fotograma sin salirse del presupuesto de 60 FPS | Rendimiento | IA headless en dominio, coordinada por `EnemyUpdateService`: repath acotado (no hay A\* por tick) y A\* con presupuesto de expansiones. El índice de chunks de `World` usa una clave sin colisiones de hash, corregida tras perfilar | [EnemyUpdateService](Minecraft2/src/application/EnemyUpdateService.java), [AStarPathfinder](Minecraft2/src/domain/enemy/AStarPathfinder.java), [World](Minecraft2/src/domain/world/World.java), [perf/](Minecraft2/perf/README.md) | Carga headless baseline + estrés con SLO previo: p95 ≤ 16,67 ms, 0 ticks fallidos, 80 zombis ([protocolo y análisis](Minecraft2/docs/recuperacion-c2/carga.md), [CSV y manifiestos](Minecraft2/perf/results/README.md)); regresión [WorldChunkIndexTest](Minecraft2/test/domain/world/WorldChunkIndexTest.java) | **Cumple.** 80 zombis: p95 1,759 / 1,926 / 1,679 ms y 0 errores en las tres repeticiones; 160 zombis: ≤ 13,3 ms; parada en 320 (mediana 50,3 ms). JFR demostró el cuello de botella (51 % del tiempo en el índice de chunks). Su corrección bajó el p95 con 80 zombis de 7,0–9,9 a 1,7–1,9 ms. Límites: headless, sin FPS gráficos; ahora limitan las lecturas de bloques de A\* y la separación cuadrática | **Previsto:** despliegue → readiness → humo → prueba de carga corta contra el servidor desplegado | **Previsto:** con 80 zombis activos, en cada una de tres repeticiones: p95 del tick > 16,67 ms, cualquier tick fallido o cualquier tick con déficit de población; umbral pendiente de confirmación con el profesor |
| Incorporar un enemigo: ajustar dificultad y oleadas sin tocar la IA ni el render | Modificabilidad | Reglas puras en dominio: `WaveRules` (oleadas) y `Difficulty` → `ZombieParameters`. `HordeManager` solo programa oleadas en aplicación; `GameSettings` es configuración de sesión | [WaveRules](Minecraft2/src/domain/enemy/WaveRules.java), [Difficulty](Minecraft2/src/domain/enemy/Difficulty.java), [HordeManager](Minecraft2/src/application/HordeManager.java), [GameSettings](Minecraft2/src/application/GameSettings.java) | [WaveRulesTest](Minecraft2/test/domain/enemy/WaveRulesTest.java), [DifficultyTest](Minecraft2/test/application/DifficultyTest.java), [HordeManagerTest](Minecraft2/test/application/HordeManagerTest.java), [GameSettingsTest](Minecraft2/test/application/GameSettingsTest.java); mutación con PIT | **Cumple.** Pruebas aprobadas; VERY_HARD usa los mismos algoritmos con otros parámetros. PIT (ejecución conservada de `b5a8e7c`, selección sin cambios): mutantes eliminados en Difficulty 5/5, GameSettings 14/14, WaveRules 30/32 y HordeManager 79/94. Límite: un nivel de dificultad nuevo requiere añadirlo a `Difficulty` | **Previsto:** pruebas unitarias y de integración → reglas de arquitectura | **Previsto:** falla una prueba vinculada al reto o se infringe una frontera de dependencias; no se atribuye un umbral de mutación (PIT) que no es un gate |
| Incorporar un enemigo: probar conducta del zombi y controles del jugador sin ventana ni GPU | Testabilidad | FSM, A\*, navegación y física del zombi en `domain.enemy`, sin LibGDX. `GameInput` solo traduce dispositivos a `PlayerFrameInput` inmutable, y `PlayerControlService` coordina movimiento, física, estamina y combate en aplicación | [ZombieStateMachine](Minecraft2/src/domain/enemy/ZombieStateMachine.java), [PlayerFrameInput](Minecraft2/src/application/PlayerFrameInput.java), [PlayerControlService](Minecraft2/src/application/PlayerControlService.java), [GameInput](Minecraft2/src/presentation/game/GameInput.java) | [FSM](Minecraft2/test/domain/enemy/ZombieStateMachineTest.java), [A\*](Minecraft2/test/domain/enemy/AStarPathfinderTest.java), [controles/combate](Minecraft2/test/application/PlayerControlServiceTest.java), [estamina](Minecraft2/test/application/PlayerControlStaminaTest.java), [fronteras](Minecraft2/test/architecture/ArchitectureBoundaryTest.java), [smoke OpenGL](Minecraft2/test/presentation/game/PlayerControlsVisualSmoke.java) | **Cumple.** **199 unitarias/adaptadores + 148 integración; 0 fallos, errores u omitidas.** Dominio con unitarias: líneas **887/984 (90,14 %)**, ramas **555/693 (80,09 %)**. FSM: 26/29 mutantes eliminados. Las reglas de import se comprueban automáticamente. El smoke OpenGL automatizado pasó en Linux/Intel y Windows/AMD; no es una prueba con usuarios. [Reporte](Minecraft2/docs/pruebas.md) | **Previsto:** pruebas unitarias y de integración → reglas de arquitectura | **Previsto:** falla una prueba o aparece una dependencia prohibida; cobertura de líneas de `domain/**` < 70 % como gate transversal |
| Incorporar un enemigo sin romper mundo, guardado ni estado de partida | Mantenibilidad | `GameSession.advance(delta, PlayerFrameInput)` es el único avance del tick: pausa y muerte congelan jugador, hordas e IA. Los enemigos son estado de sesión y JSON v1 no cambia. Persistencia detrás de `WorldStorage` | [GameSession](Minecraft2/src/application/GameSession.java), [WorldApplicationService](Minecraft2/src/application/WorldApplicationService.java), [JsonWorldStorage](Minecraft2/src/persistence/JsonWorldStorage.java) | [GameSessionTest](Minecraft2/test/application/GameSessionTest.java), [CombatSessionRegressionTest](Minecraft2/test/application/CombatSessionRegressionTest.java), [LocalWorldPersistenceIT](Minecraft2/test/application/LocalWorldPersistenceIT.java), [WorldLifecycleIT](Minecraft2/test/application/WorldLifecycleIT.java), [stub de almacenamiento](Minecraft2/test/application/RecoveryWorldStorageFailureTest.java), caja negra [GameFlowSmokeIT](Minecraft2/test/presentation/GameFlowSmokeIT.java) | **Cumple.** Regresiones de pausa, muerte, reaparición y combate aprobadas. Tres escenarios públicos del JAR por consola aprobados: crear, guardar, reabrir en otro proceso y eliminar; cancelar el borrado; JSON inválido sin sustituir el mundo. Límites: `application` depende de `persistence.WorldStorage` (inversión incompleta) y persiste el ciclo `domain.world` ↔ `domain.player` ([ADR](Minecraft2/docs/adr/ADR-001-estilo-recuperacion.md)) | **Previsto:** pruebas de integración → humo del flujo principal tras el despliegue | **Previsto:** falla una regresión de sesión o persistencia, o el recorrido público de crear, guardar, reabrir y eliminar |

Las dos columnas nuevas están **previstas**: se verifican cuando el pipeline las ejecute y no modifican los resultados ya registrados, que conservan su fecha y alcance. Aspectos sin verificación automática prevista: la comprobación gráfica con OpenGL de los controles (requiere GPU y no está prevista en el pipeline) y la validación humana de usabilidad y equilibrio de juego. El SLO-RED-01 propuesto para la comunicación front→back se incorporará a la tabla cuando el profesor lo confirme.

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
