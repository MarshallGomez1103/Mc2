# CORTE 3 — Entrega continua y segura — checklist activo

Plan y requisitos: [Corte 3 en el README del proyecto](README.md#corte-3). Reparto, ramas y archivos por persona: [README raíz](../README.md#asignación-del-corte-3). Fecha límite: **lunes 16 de noviembre de 2026, 23:59** (se evalúa el último commit de `main`).

Leyenda: `[ ]` pendiente; `[x]` terminada con evidencia. **Marcar solo con evidencia real**: enlace a la ejecución de Actions, al pull request o al commit, o el comando con su resultado. Los archivos, clases y pruebas citados aquí que todavía no existen están **previstos**: cada persona los crea en su rama; este documento solo los lista. Cada persona marca únicamente su propia sección.

Orden obligatorio: (1) esqueleto de una sola sesión con `main` verde; (2) multijugador y skins; (3) adicionales. Si el pipeline de `main` no termina en verde en el último commit, los criterios de integración continua, arquitectura verificada y despliegue continuo quedan como máximo en nivel Básico.

## Contrato común (CP0)

Estos nombres se fijan en CP0 y no cambian sin avisar al equipo; los documentos, scripts y pruebas los siguen tal cual.

- **Servicios:** `mc2-server` (back) y `mc2-front` (front). Dockerfiles: `Minecraft2/Dockerfile` (servidor) y `Minecraft2/Dockerfile.front`. Compose en la raíz del repositorio (`docker-compose.yml`), `.env.example` en la raíz, contexto de construcción `./Minecraft2`.
- **Puertos:** 7777 TCP juego; 8080 HTTP `/health` y `/metrics` del servidor; 6080 HTTP noVNC del front.
- **Variables de entorno:** `MC2_SERVER_HOST` (`mc2-server`), `MC2_SERVER_PORT` (`7777`), `MC2_HEALTH_PORT` (`8080`), `MC2_TICK_HZ` (`20`), `MC2_LOAD_ZOMBIES` (`0`; `80` para carga), `MC2_PLAYER_NAME`, `MC2_SKIN_ID` (0–2), `MC2_WORLD_DIR` (`/data/worlds`), `MC2_IMAGE_TAG`.
- **Protocolo v1:** TCP con líneas JSON. Mensajes: `hello{name,skinId}`, `welcome{playerId,tickHz}`, `input{seq,...campos de PlayerFrameInput}`, `state{tick,players[],zombies[]}`, `bye`.
- **Métricas Prometheus del servidor:** `mc2_tick_duration_seconds` (histograma), `mc2_tick_failures_total`, `mc2_zombies_active`, `mc2_players_connected`, `mc2_population_deficit_ticks_total`.
- **Workflows:** `.github/workflows/ci.yml`, `.github/workflows/cd.yml` y `.github/workflows/load.yml` (`workflow_dispatch`). **Scripts:** `scripts/sca.sh`, `scripts/secretos.sh`, `scripts/escaneo-imagen.sh`, `scripts/umbral-sast.sh`, `scripts/deploy.sh`, `scripts/rollback.sh`.
- **SLO:** SLO-CARGA-01 conservado (80 zombis activos, p95 del tick ≤ 16,67 ms, 0 ticks fallidos, 0 ticks con déficit, tres repeticiones) y SLO-RED-01 propuesto (p95 de ida y vuelta entrada→estado ≤ 50 ms, 0 errores). Ambos están **pendientes de confirmación con el profesor** hasta que se confirmen por escrito.
- **Alcance acordado de palabra con el profesor (confirmación escrita pendiente):** front y back en contenedores separados, SLO de la comunicación front→back, ejecución local y multijugador en la misma red compartiendo el contenedor del front. El cambio de arquitectura se registra en el ADR-002.
- **Skins:** tres (`skinId` 0–2, una por integrante). `Player.skinId` y su cableado en `VoxelGame` son de la persona 1; el protocolo y `RemotePlayerRenderer`, de la persona 2.
- **Quién escribe cada archivo compartido:** `Minecraft2/pom.xml`, `ci.yml`, `dependabot.yml`, los cuatro scripts de seguridad, el README raíz y `.gitignore`: persona 1. `cd.yml`, `load.yml`, `deploy.sh`, `rollback.sh`, Docker, Compose, `perf/`, `k8s/` y `observabilidad/`: persona 3. El paso de ZAP lo entrega la persona 2 como fragmento y la persona 3 lo cablea en `cd.yml`. ADR-002: persona 3 (la persona 1 confirma el alcance con el profesor y revisa); ADR-003: persona 2 (la persona 1 revisa). `devops.md`: persona 3; `seguridad.md`: persona 2. `arquitectura.md` (vista de despliegue): persona 3.

Lista de CP0:

- [ ] Persona 1: confirmar por escrito el alcance acordado con el profesor y los SLO propuestos; la persona 3 registra el resultado en el ADR-002.
- [ ] Persona 1: crear las ramas `c3/thomas`, `c3/jasub` y `c3/ethian` desde `main` y configurar la protección de `main` (pull request obligatorio, aprobación de otro integrante, checks requeridos).
- [ ] Personas 2 y 3: revisar el contrato (protocolo, puertos, servicios, variables) y señalar cualquier cambio antes de empezar.
- [ ] Probar Gitleaks sobre el historial completo del repositorio antes de convertirlo en control bloqueante.
- [ ] Probar Trivy sobre la imagen base antes de convertirlo en control bloqueante.

## Calendario

| Momento | Persona 1 — Thomas | Persona 2 — Jasub | Persona 3 — Ethian | Condición para avanzar |
| --- | --- | --- | --- | --- |
| **CP0 · vie 13** | Confirma por escrito el alcance acordado con el profesor; ramas, protección de `main`, contrato fijo | Revisa el contrato (protocolo v1, puertos) | Revisa el contrato (servicios, `.env.example`) | Contrato de nombres, puertos y umbrales fijado en este TODO; Gitleaks sobre el historial completo y Trivy sobre la imagen base probados antes de bloquear |
| **CP1 · vie 13–sáb 14 · esqueleto** | `ci.yml` con gates y JaCoCo ≥ 70 % de `domain/**`; `GameSession` de una sola sesión lista para el servidor (sin red); el arranque headless y `/health` son de la persona 2 (C3-00.8) | Adaptador de red mínimo (hola → estado → entrada), reglas de arquitectura, humo | Dockerfiles del back y del front, Compose con healthcheck, `cd.yml` a GHCR con despliegue y humo; prueba de concepto de noVNC | Pull request con todos los checks en verde; `main` verde con despliegue y humo |
| **CP2 · sáb 14–dom 15** | Multijugador en dominio y aplicación (rojo → verde), `skinId` en `Player` | Protocolo con varios jugadores y tres skins renderizadas (`RemotePlayerRenderer`, cableado en `VoxelGame` por la persona 1), STRIDE y seguridad, pull request negativo | Carga SLO contra el entorno desplegado, reversión ejecutada, tag `v1.0.0` y Release con el JAR | Dos o tres clientes juegan juntos en la red local; SLO bloqueante probado con un umbral roto |
| **CP3 · dom 15 · adicionales** | `evolucion.md`, `implementacion.md`, README | DAST con ZAP y hallazgos | Kubernetes en kind, observabilidad (Prometheus y Grafana, alerta, métrica de negocio) | Cada adicional con evidencia independiente |
| **CP4 · lun 16 · cierre** | Integración final, README, ensayo de la demostración | Tabla de trazabilidad de ocho columnas, revisión cruzada | Vista de despliegue, `devops.md`, ADR | `main` verde; documentos, código y Compose coinciden; coevaluaciones entregadas |

## Persona 1 — Thomas

Rama: `c3/thomas`. Criterios que alimenta: Integración continua (15), cableado de DevSecOps (20), Documentación (10), Guía de implementación (10), apoyo a la presentación (10).

### Alcance, ADR y pendientes del Corte 2

- [ ] **C3-00.1 / C3-00.2** Alcance acordado y SLO propuestos confirmados por escrito con el profesor. Archivos: el resultado se registra en `Minecraft2/docs/adr/ADR-002-despliegue.md` (lo escribe la persona 3). Evidencia: confirmación escrita enlazada o citada en el ADR-002. Depende de: respuesta del profesor.
- [ ] **C3-00.4** Revisar el ADR-002 de despliegue (contenedores separados, multijugador, SLO de la comunicación front→back) escrito por la persona 3 y aprobar su pull request. Evidencia: ADR-002 en `main` con el estado confirmado.
- [ ] **C3-01.1 a C3-01.3** Inventario de pendientes y retroalimentación del Corte 2 con requisito, fuente, estado, corrección y evidencia; resolver los exigidos. Archivos: `Minecraft2/docs/evolucion.md` (previsto) y las correcciones necesarias. Evidencia: cada pendiente enlaza a prueba o cambio real; si falta la retroalimentación original, la revisión queda pendiente sin inferirla. Depende de: fuentes del Corte 2.

### Multijugador y skin (dominio y aplicación, después del esqueleto)

- [ ] **C3-00.5** `World` con varios `Player` por identificador y servicios con jugador explícito (hoy `world.getPlayer()` en `GameSession`, `EnemyUpdateService`, `PistolService` y `PlayerControlService`). Pruebas a crear: `WorldMultiPlayerTest`, `GameSessionMultiPlayerTest` (rojo primero, verde después, commits separados). Evidencia: `cd Minecraft2 && mvn -B test -Dtest=WorldMultiPlayerTest,GameSessionMultiPlayerTest` y la suite completa en verde con JSON v1 sin cambios. Depende de: esqueleto de una sola sesión con `main` verde.
- [ ] **C3-00.6** `Player.skinId` (0–2) y cableado en `VoxelGame` con `MC2_SKIN_ID`. Pruebas: incluidas en `WorldMultiPlayerTest`. Evidencia: tres jugadores con tres skins distintas en la red local. Depende de: C3-00.5 y del protocolo de la persona 2.

### Integración continua y protección de `main`

- [ ] **C3-03.1** Protección de `main`: pull request obligatorio, aprobación de otro integrante y checks requeridos; sin push directo. Evidencia: configuración de reglas e historial de pull requests aprobados.
- [ ] **C3-03.2 / C3-03.3** `ci.yml` en cada `push` y `pull_request` con un check por etapa (construcción, unitarias, integración, arquitectura, seguridad, imagen); sin `continue-on-error` en controles obligatorios. Archivos: `.github/workflows/ci.yml` (previsto). Evidencia: ejecuciones en Actions. Depende de: Dockerfiles de la persona 3 para el job de imagen.
- [ ] **C3-03.4** Gate JaCoCo de líneas de `domain/**` ≥ 70 % que rompe la construcción. Archivos: `Minecraft2/pom.xml`. Evidencia: ejecución de Actions que falla al bajar el umbral a propósito y ejecución que pasa. Depende de: ninguna.
- [ ] **C3-03.5 / C3-03.6** Reportes (pruebas, cobertura, seguridad, carga) como artefactos del SHA, caché de Maven y duración < 10 minutos. Evidencia: duraciones reales de las ejecuciones.
- [ ] **C3-03.7** Badge del pipeline y URL o mecanismo del entorno en el README raíz. Evidencia: badge visible con el último estado real.
- [ ] **C3-03.8** `.github/dependabot.yml` (previsto). Evidencia: archivo en `main`; el control de SCA sigue siendo independiente.
- [ ] **C3-05.4** Cableado de los cuatro controles (SAST, SCA, secretos, imagen) con versiones fijas y un paso que aplica el umbral cuando la herramienta solo reporta. Archivos: `scripts/sca.sh`, `scripts/secretos.sh`, `scripts/escaneo-imagen.sh`, `scripts/umbral-sast.sh`. Evidencia: cada control falla el job con un caso preparado. Depende de: justificación de umbrales de la persona 2 (ADR-003).

### Documentación, integración y presentación

- [ ] **C3-10.1** README raíz: badge, URL o mecanismo, arranque con Compose, pipeline, versiones y cliente, tabla de trazabilidad de ocho columnas y tabla artefacto → ruta actualizada. Evidencia: pasos comprobados desde una copia limpia.
- [ ] **C3-10.6** `Minecraft2/docs/evolucion.md` (previsto): tres cortes, decisiones mantenidas y cambiadas, resolución de pendientes del Corte 2 y límites finales.
- [ ] **C3-10.8 a C3-10.11** `Minecraft2/docs/implementacion.md` (previsto) con las nueve partes, dos escenarios de costos, al menos cinco problemas con síntoma, causa y solución, y comandos comprobados. Depende de: sistema desplegado.
- [ ] **C3-11** Integración final, presentación de 15 minutos y ensayo de las dos demostraciones (un cambio que pasa y otro que se bloquea). Material de seguridad de la persona 2 y de despliegue de la persona 3.
- [ ] Entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.
- [ ] Revisar el control antes de cada commit y de la entrega: requisito literal, archivos completos, rutas personales, marcadores sin resolver, enlaces, cifras con fecha y commit, pruebas omitidas informadas por separado.

## Persona 2 — Jasub

Rama: `c3/jasub`. Criterios que alimenta: Arquitectura verificada (15), DevSecOps (20), Documentación (10), DAST (+5).

### Red, servidor headless y skins

- [ ] **C3-00.7** Protocolo v1 y adaptador de red (TCP con líneas JSON, `ServerSocket` de la biblioteca estándar, sin dependencias nuevas). Pruebas a crear: `ProtocolCodecTest` (rojo primero), `GameServerIT`. Archivos: paquete nuevo del adaptador de red y cliente (nombre a fijar en CP0). Evidencia: `cd Minecraft2 && mvn -B test -Dtest=ProtocolCodecTest` y `mvn -B verify -DskipUnitTests=true -Dit.test=GameServerIT`. Depende de: contrato común.
- [ ] **C3-00.8** Arranque headless del servidor con `/health` y `/metrics` (`com.sun.net.httpserver`) y las métricas del contrato; configuración por las variables `MC2_*`. Evidencia: `curl` a `/health` y `/metrics` con las cinco métricas del contrato; `GameServerIT` cubre el arranque. Depende de: `GameSession` de una sola sesión (persona 1, CP1); cambios de `pom.xml` por la persona 1.
- [ ] **C3-00.9** Protocolo con varios jugadores y tres skins: `hello` y `state` con `skinId`, `RemotePlayerRenderer` en presentación. Pruebas a crear: `RemotePlayerRendererTest`. Evidencia: dos o tres clientes se ven en la red local con skins distintas. Depende de: C3-00.5 (persona 1).
- [ ] **C3-04.5 (humo)** Ampliar `GameFlowSmokeIT` hasta el servidor (humo de caja negra por el protocolo). Archivos: `Minecraft2/test/presentation/GameFlowSmokeIT.java`. Evidencia: ejecución de Failsafe en Actions.

### Arquitectura y trazabilidad

- [ ] **C3-04.1 / C3-04.2** Al menos dos reglas bloqueantes del ADR-001 (dominio sin dependencias hacia aplicación, presentación, persistencia, bootstrap ni LibGDX; aplicación con `persistence.WorldStorage` como única dependencia hacia persistencia) más una regla nueva para la frontera de red en `ArchitectureBoundaryTest`. Evidencia: una infracción introducida a propósito rompe el job de arquitectura.
- [ ] **C3-04.7 / C3-04.8** Tabla de trazabilidad de ocho columnas en el README raíz (solo el bloque de la tabla): etapa y umbral de cada fila; actualizar resultados con fecha y SHA cuando exista evidencia nueva; declarar los aspectos sin verificación automática y su razón. Depende de: pipeline en ejecución; SLO-RED-01 se incorpora cuando el profesor lo confirme.

### Seguridad y DAST

- [ ] **C3-05.1 a C3-05.3** STRIDE sobre el diagrama de contenedores actualizado a lo desplegado: al menos cinco amenazas específicas (por ejemplo, el flujo de `input` del cliente al servidor, el puerto 7777, `JsonWorldStorage` y los nombres de mundo, la cadena de imagen y JAR) con categoría, componente o flujo, mitigación y verificación; autenticación y autorización explicadas según existan usuarios. Archivos: `Minecraft2/docs/seguridad.md` (previsto).
- [ ] **C3-05.4 (justificación)** `Minecraft2/docs/adr/ADR-003-seguridad.md` (previsto): herramientas y umbrales (SAST sin hallazgos altos o críticos sin tratar; SCA sin altas o críticas con corrección; cero secretos incluido el historial; imagen sin críticas con corrección). Revisión: persona 1.
- [ ] **C3-05.6** Registro de cada hallazgo en `seguridad.md` con una de tres decisiones (corregido con commit, falso positivo con justificación, riesgo aceptado con justificación y trabajo necesario).
- [ ] **C3-05.7** Pull request negativo con una credencial ficticia: check fallido conservado, cerrado sin fusionar y enlazado en `seguridad.md`. Nunca un secreto real.
- [ ] **C3-09** Paso de OWASP ZAP baseline contra `/health` y el front noVNC del entorno desplegado, entregado como fragmento para `cd.yml` (persona 3); hallazgos tratados con las mismas tres decisiones. Evidencia: reporte como artefacto de Actions. Depende de: CD en verde.
- [ ] **C3-10.7** `Minecraft2/docs/pruebas.md`: comandos, clasificación Surefire/Failsafe, reportes y relación con los checks reales.
- [ ] Entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.
- [ ] Revisar el control antes de cada commit y de la entrega: requisito literal, archivos completos, rutas personales, marcadores sin resolver, enlaces, cifras con fecha y commit, pruebas omitidas informadas por separado.

## Persona 3 — Ethian

Rama: `c3/ethian`. Criterios que alimenta: Contenerización (5), Despliegue continuo (15), SLO desplegado de Arquitectura verificada (15), Documentación (10), Kubernetes (+5) y observabilidad (+5).

### Contenedores y entorno

- [ ] **C3-02.1 / C3-02.2** `Minecraft2/Dockerfile` (servidor) y `Minecraft2/Dockerfile.front` multi-etapa, base de versión fija, usuario sin privilegios y `Minecraft2/.dockerignore` ajustado a ambos. Evidencia: construcción de las dos imágenes en Actions; los procesos corren sin root.
- [ ] **C3-02.3 a C3-02.5** `docker-compose.yml` (raíz, contexto `./Minecraft2`) con `mc2-server` y `mc2-front`, `.env.example` con todas las variables del contrato y valores ficticios, healthchecks reales y `depends_on` con condición de salud. Evidencia: `cp .env.example .env && docker compose up --build --wait` desde la raíz deja ambos servicios saludables y `curl -fsS http://localhost:8080/health` responde. Depende de: servidor con `/health` (persona 2, CP1).
- [ ] **C3-02.6** Arranque desde cero en una máquina con Docker, sin Java ni Maven en el host. Evidencia: registro de la comprobación.
- [ ] Prueba de concepto de noVNC con OpenGL por software en `mc2-front`; si falla o es lenta, cliente de consola para humo y SLO. Evidencia: decisión registrada en ADR-002 y resultado real de la prueba.

### Carga y SLO desplegado

- [ ] **C3-04.3 / C3-04.4** Script de carga headless en `Minecraft2/perf/` (previsto; nombre a fijar en CP1) que conecta N clientes, mide el tiempo de ida y vuelta entrada→estado (SLO-RED-01) y lee el histograma `mc2_tick_duration_seconds` de `/metrics` (SLO-CARGA-01); sale con código distinto de cero si falta un resultado, no se alcanza la población o se rompe cualquier umbral, en tres repeticiones. Prueba a crear: `LoadSloGateTest` (prueba del script `LoadSloGate`; rojo primero). Evidencia: ejecución con un umbral roto a propósito que falla y ejecución con umbrales reales que pasa. Depende de: cliente de red de la persona 2.
- [ ] **C3-04.5 / C3-04.6** Después de cada despliegue: readiness con timeout, humo y carga corta (con `MC2_LOAD_ZOMBIES=80`) contra el entorno desplegado; carga larga con `workflow_dispatch` en `.github/workflows/load.yml` (previsto). Evidencia: CSV, manifiesto y reporte como artefactos con SHA y parámetros.

### Entrega continua

- [ ] **C3-06.1 / C3-06.2** `.github/workflows/cd.yml` (previsto): imagen a GHCR con SHA y versión semántica con tag, despliegue automático desde `main` con Compose en un runner autohospedado restringido al job de `main` con Environment, nunca código de pull requests. Archivos: `scripts/deploy.sh`. Evidencia: ejecución de Actions y paquete publicado.
- [ ] **C3-06.3 / C3-06.4** Secretos en GitHub Secrets o Environments; readiness, humo y carga en ese orden con salida distinta de cero si fallan; URL o mecanismo verificable. Evidencia: ejecución de Actions.
- [ ] **C3-06.5** Reversión al tag o SHA anterior con verificación posterior. Archivos: `scripts/rollback.sh`. Evidencia: una ejecución real documentada en `Minecraft2/docs/devops.md`.
- [ ] **C3-06.6 a C3-06.8** Tags semánticos, `CHANGELOG.md` (previsto) o Releases, tag `v1.0.0` y JAR del cliente en GitHub Releases; verificar la descarga y la conexión al servidor desplegado.

### Documentación de despliegue

- [ ] **C3-00.4** `Minecraft2/docs/adr/ADR-002-despliegue.md` (existe como esqueleto propuesto): contexto, alternativas, decisión y consecuencias del despliegue con contenedores separados; completar con la confirmación del profesor y la evidencia de la prueba de noVNC. Revisión: persona 1.
- [ ] **C3-10.2** `Minecraft2/docs/devops.md` (previsto): diagrama del pipeline, etapa y check, entrada y salida, umbral y su justificación, publicación, despliegue y reversión con evidencia.
- [ ] **C3-10.4** Vista de despliegue (C4 de despliegue o vista física 4+1) en `Minecraft2/docs/arquitectura.md`: nodos, contenedores, comunicaciones, datos y límites de confianza, coherente con lo desplegado. Revisión: persona 1.

### Adicionales (solo con CI y CD en verde)

- [ ] **C3-07 · Kubernetes** `k8s/` (previsto): `Deployment`, `Service`, `ConfigMap` y plantilla de `Secret` sin credenciales, probes de liveness y readiness, límites de recursos y al menos una `NetworkPolicy` o política RBAC justificada; desplegado en kind. Evidencia: estado real de los recursos y la política verificada, desde el pipeline o paso a paso.
- [ ] **C3-08 · Observabilidad** `observabilidad/` (previsto): Prometheus y Grafana sobre `/metrics` del servidor, tablero con p95 del tick, tasa de error y throughput, una regla de alerta y la métrica de negocio (jugadores y zombis activos). Las unidades son ticks, no latencia HTTP ni FPS. Evidencia: datos reales del entorno y `promtool check config` en el pipeline.
- [ ] Entregar las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación.
- [ ] Revisar el control antes de cada commit y de la entrega: requisito literal, archivos completos, rutas personales, marcadores sin resolver, enlaces, cifras con fecha y commit, pruebas omitidas informadas por separado.

## Bonificaciones

La bonificación conjunta tiene un máximo de 10 puntos. Cada adicional necesita evidencia independiente y arranca solo con CI y CD en verde.

- [ ] Kubernetes (+5): ver C3-07 en la sección de la persona 3.
- [ ] Observabilidad (+5): ver C3-08 en la sección de la persona 3.
- [ ] DAST con ZAP (+5): ver C3-09 en la sección de la persona 2; el objetivo es `/health` y el front noVNC del entorno desplegado.

## Integración final

- [ ] Orden: la persona 1 integra primero; después la persona 2 (rebase sobre `main`); por último la persona 3 (rebase). Cada pull request lo aprueba otro integrante.
- [ ] Quien integra el último ejecuta `cd Minecraft2 && mvn clean verify` y el pipeline completo, y deja la versión final en `main` sin conflictos.
- [ ] Documentos, código y Compose coinciden: nombres de servicios, variables, puertos y comandos iguales en README, `implementacion.md`, `.env.example`, `docker-compose.yml` y workflows.
- [ ] Revisión final de enlaces, rutas personales y marcadores sin resolver en todos los documentos entregables.
- [ ] El último commit de `main` anterior a la fecha límite termina con el pipeline en verde.
- [ ] Repositorio público o con el profesor como colaborador, con el historial de Actions visible; URL o mecanismo del entorno activo durante la calificación.
- [ ] Captura del juego en el README raíz (opcional): crear primero el archivo de imagen y enlazarlo después; no hay imagen todavía.

## Cierre del equipo

- [ ] SLO-CARGA-01 y SLO-RED-01 confirmados con el profesor y reflejados en los documentos.
- [ ] Presentación de 15 minutos ensayada con las dos demostraciones.
- [ ] Las tres personas entregan las coevaluaciones asignadas de todos los grupos distintos al propio, al terminar cada presentación; no se modifican después.
- [ ] Enlaces de evidencia reunidos: ejecuciones de Actions, pull requests aprobados y bloqueado, Releases, reversión y registro de hallazgos.

---

# Recuperación del Corte 2 — registro anterior

Plan y reparto por archivos: plan del equipo del Corte 2 (archivado fuera del árbol de entrega).
Estado de cierre al 9 de octubre de 2026: los tres frentes están integrados y la carga de recuperación está medida y analizada. Falta publicar el candidato y enviar la solicitud de revisión. La meta del 8 de octubre pertenece al calendario inicial.

Esta sección registra la recuperación. Las secciones que siguen conservan el historial anterior. Marcar `[x]` solo después de revisar implementación, pruebas y evidencia real.

## Base común

- [x] Estilo implementado y reglas/contrato comprobados; Ethian revisó ADR y diagramas contra el código final.
- [ ] Actualizar cada clon a su rama de recuperación publicada y conservar cambios locales.
- [x] Aportes identificados e integrados en orden: Thomas, jasub/final-a y Ethian.

## Thomas — arquitectura e integración

- [x] REC-T1: trasladar coordinación de movimiento/física/estamina a aplicación sin cambiar reglas.
- [x] REC-T2: conectar GameInput/GameSession y adaptar VoxelGame, HUD, cámara y respawn.
- [x] REC-T3: conservar regresiones y probar pausa, muerte, energía, tiempos inválidos y límites.
- [x] REC-T4: configurar JaCoCo y clasificar tests heredados/nuevos; separar unitarias e integración sin omitir ni duplicar pruebas.
- [x] REC-T5: ejecutar empaquetado e integración con `mvn verify`, conservando PIT.
- [x] REC-T6: integrar Jasub y Ethian; tabla del README raíz, arquitectura con siete apartados, reportes publicados y ejecución limpia.

Evidencia de REC-T1–T5: [reporte actual](docs/pruebas.md), [índice](docs/recuperacion-c2/correcciones.md) y arnés gráfico de controles. La integración, tabla/arquitectura, flujo público y comprobación desde copia limpia están disponibles. REC-T6 queda completo respecto de la integración técnica. El resultado/análisis de carga de Ethian está incorporado; queda la publicación y el envío de la entrega.

## Jasub — pruebas y flujo público

- [x] REC-J1: clasificar pruebas existentes y añadir casos propios con AAA, equivalencia, límites y dobles cuando apliquen.
- [x] REC-J2: sesión con componentes reales comprobada por GameSessionTest y CombatSessionRegressionTest; no se duplican casos para crear otro nombre de clase.
- [x] REC-J3: persistencia temporal/aislamiento comprobados por LocalWorldPersistenceIT, WorldLifecycleIT y SaveEdgeCasesIT.
- [x] REC-J4: GameFlowSmokeIT versionado y ejecutado mediante consola pública del JAR.
- [x] REC-J5: segundo escenario público de cancelación/eliminación o archivo inválido.
- [x] REC-J6: comprobación reproducible de fronteras arquitectónicas.
- [x] REC-J7: reporte con comandos, evidencia, resultados y límites de automatización.

Evidencia de integración: [reporte](docs/recuperacion-c2/integracion.md) e [inventario/casos](docs/pruebas.md). La ampliación de caja negra, fronteras, stub de error y regresiones de métricas se completó durante la integración; no se atribuye a Jasub todo el trabajo del candidato.

## Ethian — estilo, modelado y carga

- [x] REC-E1: comparación de al menos dos estilos y ADR con ganancias/costes.
- [x] REC-E2: diagramas de contexto, contenedor y componentes coherentes con código.
- [x] REC-E3: arquitectura inicial y recuperada con referencias de versión reales.
- [x] REC-E4: scripts reproducibles de baseline y estrés en perf/.
- [x] REC-E5: throughput real, tasa de errores y métricas headless bien definidas.
- [x] REC-E6: SLO definido antes de medir y población objetivo declarada.
- [x] REC-E7: corridas acotadas autorizadas, comparables y con parada definida (medición previa en `d180eb9`; final en `c2e03e7`, parada en 320).
- [x] REC-E8: CSV, consola y manifiesto revisados/versionados en perf/results/, tras medir el mismo candidato integrado.
- [x] REC-E9: análisis de cuello de botella, relación con arquitectura y límites del harness ([carga.md §7](docs/recuperacion-c2/carga.md#7-análisis-rec-e9)).

REC-E1–E9 completos. Resultado: SLO-CARGA-01 cumplido con 80 zombis en el equipo medido (p95 ≤ 1,926 ms en las tres repeticiones, 0 fallidos, población sostenida); el estrés se detuvo en 320. JFR identificó el índice de chunks como cuello de botella; se corrigió con regresión y se repitió la carga. La actualización de cada clon sigue siendo responsabilidad de cada integrante.

## Cierre del equipo (recuperación del Corte 2)

- [ ] Revisar cada observación de la retroalimentación y cada requisito obligatorio de la rúbrica contra el candidato con carga (responsable: Ethian); [criterios de cierre](docs/pruebas.md#verificación-del-candidato-de-entrega).
- [x] `mvn clean verify` aprobado; resultados actuales separados por nivel y cobertura de dominio.
- [x] Flujo público reproducible y revisión gráfica real de controles afectados.
- [x] ADR y diagramas corresponden al candidato final integrado.
- [x] Tabla obligatoria en README raíz e índice directo de correcciones para revisión.
- [x] Archivos, evidencias e índice del candidato integrado revisados antes del commit; repetido al incorporar la carga.
- [x] Integración técnica publicada en main; candidato base `b5a8e7c`.
- [x] Evidencia de carga incorporada y candidato verificado (199 + 148, sin fallos, errores ni omitidas).
- [ ] Publicar el candidato final y enviar la solicitud de revisión con enlaces del commit entregado (responsable: Ethian).

---

> Registro histórico del trabajo del Corte 2, cerrado. Las tareas y conteos de cada etapa no describen pendientes del Corte 3.

# TODO — Minecraft 2

Checklist único: `[ ]` pendiente, `[x]` terminada con evidencia.
Mapa maestro y ownership: hoja de ruta del Corte 2 (archivada fuera del árbol de entrega).

## Integración de jugabilidad — 2026-09-26

- [x] R1: modelo zombie de seis partes, UV y textura ON/OFF; capturas OpenGL.
- [x] R2: estamina pura, sprint, cámara y barra; pruebas unitarias y smoke gráfico.
- [x] R3: GameSession y GameHud extraídos; UI, reglas y render separados.
- [x] R4: F fullscreen/ventana; probado junto a resize 640×480 y 1280×720.
- [x] R5/R6: renderer efectivo y preferencia próximo arranque; Intel y NVIDIA probadas.
- [x] R7: crear/listar/guardar/reiniciar/cargar y archivos inválidos; JSON compatible.
- [x] R8: menú gráfico, ESC pausa, opciones y confirmaciones de salida/eliminación.
- [x] R9: separación física local; spawn ocupado, pared/corredor y 20 zombies probados.
- [x] R10: velocidad real mayor que caminar y menor que sprint; spawn seguro y detección.
- [x] Suite de la primera integración: 238/238; build, PIT seleccionado y cinco escenarios de carga.
- [x] Preservación: diff previo, copia fuente y 14 mundos con SHA256.
- [ ] Playtest humano prolongado: diversión, hordas, bob, melee y refugio en estructuras.
- [ ] Prueba física AMD y sistemas Windows/macOS; endurance gráfico prolongado.

Evidencia y límites: [integración](docs/corte2/testing/integration-20260926.md).
Las secciones siguientes conservan el historial de trabajo por estudiante;
sus conteos y opciones de consola son históricos si contradicen esta sección.

# CORTE 1 — TERMINADO

Se conserva el historial del MVP. Huecos de automatización pasan a calidad del Corte 2,
sin reabrir el alcance funcional del Corte 1.

## Decisiones compartidas antes de programar

- [x] Completar los nombres de los tres integrantes en `README.md`.
- [x] Acordar ancho, alto y profundidad de cada chunk: `16 × 64 × 16`.
- [x] Definir cómo una posición del mundo se convierte en coordenada de chunk.
- [x] Acordar el esquema JSON definitivo para mundo, chunks y bloques.
- [x] Elegir la tecnología gráfica mínima compatible con Java e IntelliJ: LibGDX + LWJGL3.
- [x] Establecer criterios de terminado y casos de prueba del MVP.

Las decisiones, límites, esquema y pruebas acordados están en [docs/decisiones-compartidas.md](docs/decisiones-compartidas.md).

## Estudiante 1 — mundo y generación

- [x] Implementar la generación pseudoaleatoria sencilla.
- [x] Crear uno o pocos chunks usando `BlockFactory`.
- [x] Distribuir césped, tierra, piedra, arena, grava, madera y hojas.
- [x] Definir límites y validaciones de `Chunk`.
- [x] Resolver el chunk correspondiente a una `Position`.
- [x] Probar creación, consulta, colocación y eliminación de bloques.

## Estudiante 2 — jugador e interacción

- [x] Definir orientación y estado mínimo adicional del jugador.
- [x] Implementar adelante, atrás, izquierda y derecha.
- [x] Implementar salto y gravedad.
- [x] Añadir colisiones básicas con bloques sólidos.
- [x] Implementar selección de bloque mediante clic (raycast en `PlayerInteractionService`, ya conectado al clic real del ratón desde `presentation.game.GameInput`).
- [x] Conectar colocar y eliminar con `World.placeBlock` y `World.removeBlock`.
- [x] Suscribir un componente visual a `BlockChange`: `VoxelGame` es observador y reconstruye la malla del chunk afectado al colocar o eliminar un bloque.

## Estudiante 3 — presentación y persistencia

- [x] Diseñar la interfaz visual del menú principal sin lógica de negocio (`MainMenu` enruta y `ConsoleIO` concentra la entrada/salida; el menú gráfico con LibGDX corresponde a la integración del equipo).
- [x] Conectar las pantallas de crear, listar, cargar, guardar y eliminar.
- [x] Completar la escritura JSON de chunks y bloques en `JsonWorldStorage` (`WorldJsonCodec.write`).
- [x] Completar la reconstrucción del mundo desde JSON (`WorldJsonCodec.read`, con `JsonParser` propio).
- [x] Manejar archivos dañados, nombres duplicados y errores de lectura/escritura (`InvalidWorldFileException`, escritura atómica; CT-09 cubre 26 documentos inválidos en `persistence.InvalidWorldFileTest`).
- [x] Añadir confirmación antes de eliminar un mundo (solo una respuesta afirmativa explícita borra).
- [x] Probar el ciclo crear → guardar → cerrar → cargar → eliminar (`application.WorldLifecycleTest` y [docs/evidencias/ct-11-flujo-mvp.md](docs/evidencias/ct-11-flujo-mvp.md)).

## Integración del equipo

- [x] Renderizar uno o pocos chunks (`presentation.game`, cuadrícula 2×2 con descarte de caras ocultas: se dibuja el 6,6 % de la geometría). Evidencia en [docs/evidencias/ct-10-render.md](docs/evidencias/ct-10-render.md).
- [x] Asignar una representación visual a los ocho tipos de bloque (`BlockAppearance`, con sombreado por orientación de cara; el aire no se dibuja).
- [x] Conectar teclado y ratón con los casos de uso correspondientes (`GameInput`: WASD, salto, mirar, y clic para colocar y eliminar).
- [x] Separar archivos de pantallas: CRUD pasa por WorldApplicationService.
  Aclaración: MainMenu sí importa World; la vista consulta dominio y GameInput coordina física.
- [x] Verificar que `WorldManager` siga siendo el único Singleton: es la única clase con instancia global; los demás constructores privados son de clases de utilidad estáticas.
- [x] Verificar que no se hayan introducido patrones no permitidos: las únicas menciones a DAO, Command o Strategy son comentarios que explican por qué **no** se usan.
- [x] Revisar dirección entre capas: dominio no importa capas superiores.
  Existe ciclo interno domain.world ↔ domain.player; no se afirma ausencia total de ciclos.
- [ ] Histórico trasladado a Corte 2: ampliar JUnit de dominio/patrones.
  Persistencia/flujo ya automatizados; manualtest no cubre exhaustivamente CT-02 a CT-07.
- [ ] Actualizar README y documentación después de cada decisión de diseño.

## Restricciones históricas del Corte 1

Contexto histórico: en Corte 2 sí se autorizan enemigos, FSM y A*.
Se mantienen sin red, base de datos, nuevos singletons o frameworks innecesarios.

- [ ] No implementar multijugador, red, crafting, enemigos o inventario avanzado.
- [ ] No añadir base de datos.
- [ ] No introducir DAO, Strategy, Builder, Prototype, CQRS ni Event Sourcing.
- [ ] No convertir nuevas clases en Singleton.
- [ ] No agregar frameworks sin una necesidad acordada por el equipo.

# CORTE 2 — CERRADO

Preparación aprobada; frentes del Corte 2 en desarrollo. Estado de cada tarea abajo.
Baseline previo: mvn clean test, 41 exitosas, 0 fallos/errores/ignoradas.

## Preparación común — C2-00

- [x] Verificar Git, actualizar main y registrar baseline.
- [x] Crear roadmap, ownership y documentos iniciales sin resultados inventados.
- [x] Crear vocabulario mínimo ZombieState, BiomeType y Difficulty.
- [x] Revisión humana y aprobación de la base de Corte 2.
- [x] Crear ramas de trabajo individuales desde la base aprobada.

## Estudiante 1 — Generación procedural

- [x] BiomeType: PLAINS, DESERT, MOUNTAINS; solo vocabulario.
- [x] C2-02: BiomeResolver regional determinista por seed + coordenadas.
- [x] Escala regional 18: demostrable con seed 17 en el mundo finito 2×2; no garantizada para toda seed.
- [x] C2-06: PLAINS suave, GRASS/DIRT/STONE y árboles ocasionales.
- [x] C2-06: DESERT plano relativo, SAND predominante y sin árboles.
- [x] C2-06: MOUNTAINS con altura espacialmente coherente y límites válidos.
- [x] C2-06: VILLAGE/estructura sencilla; ubicación y plantilla separadas, contenida en chunk.
- [x] Determinismo por seed, negativos y orden independiente de generación.
- [x] Tests unitarios de generación, capas y límites.
- [x] Tests de integración entre chunks vecinos, límites de estructura y JSON existente.
- [x] Documentación de generación en docs/corte2/terrain-generation.md.
- [x] Revisión visual de biomas/relieve y spawn en mundos medianos/grandes.
- [x] Captura visual del overlay MORISTE con instancia temporal bajo el vacío.
- [x] Revisión visual de estructura con seed 17 y cámara temporal frente a la casa.
- [x] Añadir huecos de ventanas a la casa y probar la plantilla.
- [x] Shift para correr con prueba de velocidad y controles documentados.
- [x] Impedir colocar un bloque dentro del jugador; recuperar posiciones incrustadas.
- [x] Invalidar mallas vecinas al editar un bloque en borde de chunk.
- [x] Listar mundos desde ruta estable aunque el JAR se ejecute fuera del proyecto.
- [ ] Verificación interactiva de J/K/R con teclado real.

## Extensión coordinada — tamaños finitos, FPS y muerte por vacío

- [x] Elegir Pequeño 2×2, Mediano 10×10 o Grande 16×16 al crear mundo.
- [x] Indexar chunks en World y limitar búsqueda de spawn al centro.
- [x] Crear/liberar mallas cercanas con radio J/K y mostrar FPS en HUD.
- [x] Muerte de sesión por caída y entrada reutilizable para futura muerte por enemigo.
- [x] Tests de tamaños, índice, radio y vida; último gate 63/63.
- [x] Crear, guardar, cargar y abrir Grande con heap de 2 GB; límite de 1 GB documentado.
- [ ] Verificación manual de J/K, caída real y R en una sesión interactiva del equipo.

Detalle técnico y mediciones: [world-size-render-and-life.md](docs/corte2/world-size-render-and-life.md).

## Estudiante 2 — Enemy AI

FSM = decide qué hacer. A* = decide por dónde desplazarse. Son conceptos diferentes.
Estado: implementado y probado headless (110/110); smoke manual con ratón pendiente.
Detalle: [docs/corte2/enemy-ai.md](docs/corte2/enemy-ai.md), reporte en
[docs/corte2/REPORTE_ENEMY_AI_JASUB.md](docs/corte2/REPORTE_ENEMY_AI_JASUB.md).

- [x] ZombieState: IDLE, CHASE, ATTACK, DEAD; solo vocabulario.
- [x] Zombie sin heredar de Player, sin LibGDX y como estado de sesión (`domain.enemy.Zombie`; `ZombieTest` verifica ambas restricciones).
- [x] Acordar API pura de ZombieStateMachine antes de escribir comportamiento: `next(ZombieState, ZombiePerception)`; reglas fijadas por escrito en `docs/corte2/testing/tdd-zombie-fsm.md` antes de RED.
- [x] C2-03 TDD RED: pruebas primero y evidencia de fallo real pertinente (13 ejecutadas, 8 fallos de aserción con stub sin transiciones).
- [x] TDD GREEN: mínimo comportamiento y evidencia verde (13/13).
- [x] TDD REFACTOR: simplificar conservando suite verde y registrar diff (ternarios → `fromIdle/fromChase/fromAttack`).
- [x] NavigationNode: X/Z + groundY, significado acordado (`groundY` = bloque de apoyo; pies en `groundY + 1`).
- [x] NavigationGrid: World actual, apoyo, cuerpo, desnivel y chunk existente (sin caché; subir 1, bajar 3; vecinos 4).
- [x] C2-05 AStarPathfinder: open/closed, g/h/f, parent y reconstrucción (presupuesto 2 000 expansiones; sin ruta = `Optional.empty()`).
- [x] Path y movimiento siguiendo waypoint sin atravesar obstáculos (`ZombieMovement` con auto-step; bloqueo → repath).
- [x] Detección del jugador e IDLE (distancia 3D, 12 bloques; sin línea de visión, documentado).
- [x] CHASE con ruta A*, no búsqueda por frame (probado: 120 frames → 1–3 búsquedas).
- [x] ATTACK con alcance, cooldown y efecto mínimo acordados (1.6 bloques, 1 s incluido el primer golpe, `PlayerLife.die(ENEMY)`).
- [x] DEAD por salud <= 0 y retirada según regla acordada (despawn a 0.8 s; la colección no crece tras 10×20 muertes).
- [x] Repath por objetivo movido, ruta terminada/inválida o intervalo; evitar busy retry (1.5 bloques / 1.5 s / espera 1 s sin ruta).
- [x] C2-07 integración EnemyUpdateService: FSM, percepción, navegación y movimiento (`application.EnemyUpdateService`; ON/OFF y registro para HordeManager).
- [x] C2-08 integración visual mínima y VoxelGame en ventana coordinada (`ZombieRenderer`; capturas en `docs/corte2/evidencias/`; avisar al equipo del cambio en el hotspot).
- [x] Clic izquierdo sobre zombi en alcance: golpe cuerpo a cuerpo antes de borrar bloque; si no hay zombi, conservar eliminación de bloques (`ZombieMeleeService`, ambos caminos probados en JUnit; clic real pendiente de smoke manual).
- [ ] Muerte de zombi: efecto visual breve de «explosión» sin romper terreno ni producir daño de área; retirar recursos visuales al despawn. Implementado en `ZombieRenderer` (solo visual; instancias liberadas con `retainAll`); falta verlo en pantalla con un golpe real.
- [x] Ataque letal del zombi: conectar con `PlayerLife.die(ENEMY)` sin persistir vida de sesión (captura `enemy-ai-death.png`).
- [x] Tests unitarios FSM: transiciones, límites y prioridades (13).
- [x] Tests unitarios A*: recto, obstáculo/rodeo, imposible, origen=destino y desnivel (8).
- [x] Tests de integración Enemy + World con bloques modificados (`EnemyUpdateServiceTest`: pared construida durante la persecución, jugador encerrado).
- [x] Documentación IA y evidencia TDD en docs/corte2/ (`enemy-ai.md`, `testing/tdd-zombie-fsm.md`).
- [ ] Smoke manual con ratón: golpe, explosión, picar sin zombi, R y J/K durante oleadas. La integración retiró el spawn de prueba por Z y mc2.zombies.

## Estudiante 3 — Hordas, configuración y calidad

- [x] C2-04 GameSettings: enemigos ON, texturas de enemigos ON y NORMAL por defecto; solo sesión, sin persistir (`application.GameSettings`, `GameSettingsTest`).
- [x] Enemigos ON/OFF: OFF impide spawn y actualización. Conectado desde GameSettings al loop de VoxelGame y HordeManager.
- [x] Enemy textures ON/OFF independiente de texturas del mundo; GameSettings llega a VoxelGame y ZombieRenderer aplica ZombieSkin o color plano.
- [x] Difficulty: NORMAL y VERY_HARD como vocabulario, sin parámetros.
- [x] NORMAL con parámetros centralizados y probados (`Difficulty` = `ZombieParameters.defaults()` + `WaveRules`).
- [x] VERY_HARD con mismos algoritmos y parámetros distintos probados (`DifficultyTest`: detecta, corre, mata y aguanta más).
- [x] HordeManager separado de movimiento individual (`application.HordeManager`; solo usa `spawnAt`/`zombies`/`isEnabled`).
- [x] Oleadas: inicio, cantidad, intervalo, final y siguiente oleada deterministas (seed del mundo, tiempo explícito).
- [x] Integración local de HordeManager con spawnAt, zombies e isEnabled de EnemyUpdateService; los zombis siguen en estado de sesión.
- [x] C2-08 configuración desde menú: opción 8 con submenú de enemigos, texturas y dificultad (`MainMenuSettingsTest`).
- [x] Pruebas unitarias de HordeManager con tiempo explícito (`HordeManagerTest`, 16 casos).
- [x] Pruebas unitarias de Difficulty/configuración, no getters triviales (`DifficultyTest`, `WaveRulesTest`, `GameSettingsTest`).
- [x] Huecos baratos Corte 1: límites Chunk, todos los tipos Factory y misma seed (`ChunkBoundsTest`, `BlockFactoryTest`, `SameSeedTest`).
- [x] C2-09 configuración PIT en pom.xml y scope pequeño de lógica pura (PIT 1.30.0, fuera del gate).
- [x] Mutation testing: 172 generated, 153 killed, 1 timed out, 17 survived, 1 no coverage; score 89.5 % ([mutation-testing.md](docs/corte2/testing/mutation-testing.md)).
- [x] Análisis de mutantes sobrevivientes y tests de comportamiento, sin maquillar scope (81 % → 89.5 %; supervivientes ajenos reportados).
- [x] C2-10 load test harness reproducible, headless (`test/loadtest/EnemyLoadHarness.java`).
- [x] BASELINE: referencia finita con NORMAL (3 zombis, p95 < 0.05 ms).
- [x] PEAK: carga alta y repaths concurrentes medidos (25 zombis, p95 3.7 ms, 268 A*/s).
- [x] STRESS: aumentar hasta degradación observada, sin prometer 100 enemigos (p95 > 16.7 ms con 320).
- [x] ENDURANCE/RESISTANCE: ciclo prolongado spawn/update/death/oleadas (30 min, 4 096 oleadas, heap estable, 0 errores).
- [x] Documentación de resultados, hardware/JVM, duración y limitaciones ([load-testing.md](docs/corte2/testing/load-testing.md)).

## Dependencias de integración

| Puede avanzar independientemente | Dependencia posterior |
| --- | --- |
| Estudiante 1: BiomeResolver | Acordar escala; luego terreno/estructuras y pruebas vecinas |
| Estudiante 2: FSM pura y TDD | Percepción/EnemyUpdateService; no requiere renderer |
| Estudiante 3: GameSettings y scheduling HordeManager | Parámetros y registro para spawn real |
| A* sobre fixtures | Consulta de caminabilidad acordada del World actual |
| EnemyUpdateService | Zombie + FSM + navegación + movimiento |
| HordeManager con spawn real | Creación/registro de zombies y lifecycle de colección |
| Rendering | Zombie con posición y actualización funcional |
| Load testing de IA | EnemyUpdateService y A* integrados e instrumentados |

No editar archivos de otro owner sin aviso. POM solo Estudiante 3.

## Integración y cierre — C2-09 a C2-11

- [ ] Pruebas nuevas verdes; 41 casos existentes preservados.
- [ ] Verificación visual spawn/chase/obstáculos/attack/death/oleadas/opciones.
- [ ] Regresión Corte 1: CRUD, jugar, movimiento, clics, guardar/cargar y texturas.
- [ ] PIT con objetivo aproximado 80% en scope justificado, no global.
- [ ] Cuatro escenarios medidos; no optimizar antes de medir.
- [ ] README y docs/uml.md actualizados solo en integración final.
- [ ] Quality gate aprobado antes de declarar Corte 2 terminado.

Decisiones fijas: zombies no persistidos; JSON sin cambios para enemigos; FSM + A*
sin ML/LLM/Behavior Tree; 2.5D desde World actual; sin streaming.
Documentación de pruebas canónica: docs/corte2/testing/, sin duplicar docs/testing/.
