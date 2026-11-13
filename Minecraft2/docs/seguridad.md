# Seguridad de Minecraft2

Este documento recoge el modelo de amenazas del sistema, los cuatro controles de seguridad del pipeline con sus umbrales, el registro de hallazgos y la evidencia de que los controles detienen un cambio inseguro. Lo lee quien revisa la seguridad del proyecto o decide si un riesgo se acepta. La justificación de las herramientas y de los umbrales está en el [ADR-003](adr/ADR-003-seguridad.md); el pipeline, en [devops.md](devops.md).

> **Estado del documento: parcial.** Los cuatro controles de la sección 3 ya corren en el pipeline y su primera ejecución no reportó hallazgos, de modo que no hay ninguno registrado; el resto sigue como esqueleto. Cada elemento marcado «Previsto — responsable: Persona N» se completa con ejecuciones reales y el marcador se retira antes de la entrega. Las amenazas de la sección 2 son **candidatas por evaluar**: no son vulnerabilidades confirmadas.

Responsable: Persona 2, con Persona 1 (cableado de los controles en el workflow).

## 1. Componentes y flujos analizados

El modelo parte del diagrama de contenedores del Corte 2 ([contenedores](diagramas/recuperacion-c2/02-contenedores.md)), un proceso de escritorio con almacenamiento local en JSON, y lo extiende con los componentes que introduce el Corte 3 según [ADR-002](adr/ADR-002-despliegue.md). Lo que el Corte 3 añade se marca como previsto hasta que exista en el código; este análisis se actualiza con lo realmente desplegado.

| Componente o flujo | Descripción | Existe hoy |
| --- | --- | --- |
| Almacenamiento de mundos (`JsonWorldStorage`) | Lee y escribe los mundos como JSON v1 en un directorio local. | Sí: [`JsonWorldStorage`](../src/persistence/JsonWorldStorage.java) |
| `mc2-server` | Servidor headless: protocolo TCP de líneas JSON en el puerto 7777; HTTP `/health` y `/metrics` en el 8080. | No. Previsto — responsable: Persona 2 |
| `mc2-front` | Cliente LibGDX con Xvfb y noVNC, accesible desde el navegador en el puerto 6080. | No. Previsto — responsable: Persona 3 |
| Flujo front → servidor (TCP 7777) | Entrada del jugador hacia el servidor y estado de vuelta. | No. Previsto — responsable: Persona 2 |
| Flujo navegador → noVNC (6080) | Pantalla y entrada del jugador a través del navegador. | No. Previsto — responsable: Persona 3 |
| Cadena de entrega | Repositorio, GitHub Actions, GitHub Container Registry, Releases y runner autohospedado. | No. Previsto — responsable: Persona 3 |

## 2. Modelo de amenazas (STRIDE)

Cada fila nombra un componente o flujo del sistema. «Candidata por evaluar» significa que la amenaza es plausible por el diseño, pero aún no se ha confirmado ni descartado con una prueba o un control en ejecución. Las mitigaciones que ya existen en el código se indican con su fuente; las demás son Previsto.

| ID | Amenaza | STRIDE | Componente o flujo | Mitigación | Cómo se verifica |
| --- | --- | --- | --- | --- | --- |
| A1 | Candidata: un archivo de mundo JSON alterado a mano (estructura inválida, valores fuera de rango o desmesurados) altera el estado del juego o bloquea la carga. | Manipulación | `JsonWorldStorage`, `JsonParser`, `WorldJsonCodec` | Existe: el parser rechaza documentos inválidos y el almacenamiento lanza `InvalidWorldFileException` sin sustituir el mundo activo. Previsto — responsable: Persona 2: límites de tamaño y de valores al cargar en el servidor. | Pruebas existentes: [`JsonParserTest`](../test/persistence/JsonParserTest.java), [`InvalidWorldFileTest`](../test/persistence/InvalidWorldFileTest.java) y el escenario de JSON inválido de [`GameFlowSmokeIT`](../test/presentation/GameFlowSmokeIT.java). Pruebas de los límites nuevos: Previsto — responsable: Persona 2. |
| A2 | Candidata: un nombre o identificador de mundo con separadores de ruta (`../`) lee o escribe archivos fuera del directorio de mundos. | Manipulación y divulgación de información | `JsonWorldStorage.worldFile`, ruta de mundos del servidor (`MC2_WORLD_DIR`) | Existe: el identificador solo admite letras, números, guion y guion bajo (`[a-zA-Z0-9_-]+`) y se rechaza cualquier otro. Previsto — responsable: Persona 2: aplicar la misma validación a cualquier identificador que llegue por la red. | Prueba existente: `rechazaIdentificadoresInvalidos` en [`JsonWorldStorageTest`](../test/persistence/JsonWorldStorageTest.java). Prueba con el identificador recibido por TCP: Previsto — responsable: Persona 2. |
| A3 | Candidata: muchas conexiones al puerto 7777 o líneas muy largas agotan la memoria o los hilos de `mc2-server`. | Denegación de servicio | `mc2-server`, flujo front → servidor (TCP 7777) | Previsto — responsable: Persona 2: límite de conexiones simultáneas, longitud máxima de línea y tiempo máximo de lectura; límites de CPU y memoria en el Compose (Persona 3). Contexto ya documentado: los mundos son finitos y viven en memoria ([arquitectura §7](arquitectura.md#7-límites-y-tercer-corte)). | Prueba de integración del servidor con línea sobredimensionada y conexiones en exceso, y la carga del SLO en el despliegue. Previsto — responsable: Persona 2. |
| A4 | Candidata: un cliente se hace pasar por otro jugador (declara su identificador o nombre) o abre varias conexiones para controlar más de un jugador. | Suplantación | Handshake y mensajes del protocolo TCP v1 | El juego no tiene cuentas (ver sección 2.1). Previsto — responsable: Persona 2: el servidor asigna el identificador del jugador al conectarse y no acepta el que declare el cliente; una conexión controla un solo jugador. | Prueba de integración del protocolo con un identificador falso. Previsto — responsable: Persona 2. |
| A5 | Candidata: un mensaje malformado o con valores no finitos (por ejemplo, ángulos o deltas inválidos) rompe el tick del servidor o mueve a un jugador de forma no permitida. | Manipulación | Mensajes de entrada del protocolo TCP v1, `GameSession` | Existe en el dominio: `GameSession` descarta deltas no finitos, negativos o cero y rechaza `AIR` como material ([arquitectura §4](arquitectura.md#4-arquitectura-inicial-y-evolucionada)). Previsto — responsable: Persona 2: el analizador del protocolo valida cada campo antes de construir `PlayerFrameInput` y responde con un código de error estable. | Pruebas existentes: [`GameSessionTest`](../test/application/GameSessionTest.java) y [`PlayerFrameInputTest`](../test/application/PlayerFrameInputTest.java). Pruebas del analizador: Previsto — responsable: Persona 2. |
| A6 | Candidata: un error o un log revela rutas del equipo anfitrión, trazas o datos de otros jugadores. | Divulgación de información | Respuestas de error del protocolo, logs de `mc2-server`, `/metrics` y `/health` | Previsto — responsable: Persona 2: errores con código estable y sin rutas; `/metrics` expone solo contadores y duraciones agregados, sin nombres de jugador. | Prueba que provoca un error y comprueba que el mensaje no contiene rutas; revisión del contenido de `/metrics`. Previsto — responsable: Persona 2. |
| A7 | Candidata: se despliega una imagen o un JAR que no construyó el pipeline (subido a mano al registro o alterado en Releases) o con una dependencia comprometida. | Manipulación | Imagen en GHCR, JAR del cliente en Releases, cadena de entrega | Previsto — responsable: Persona 3: la imagen solo se construye y se publica desde `cd.yml` en `main`; se despliega por etiqueta de SHA o versión, nunca `latest`; bases de versión fija (el [`Dockerfile`](../Dockerfile) actual ya fija las versiones de sus dos imágenes base); acciones fijadas por SHA; SCA e imagen escaneadas; Dependabot. | Job de escaneo de la imagen, el SCA y la revisión de los permisos de `cd.yml`. Previsto — responsable: Persona 2 y Persona 3. |
| A8 | Candidata: código de un pull request se ejecuta en el equipo del runner autohospedado y accede a secretos o al sistema anfitrión. | Elevación de privilegios | Runner autohospedado, `cd.yml` | Previsto — responsable: Persona 3: el runner autohospedado atiende solo el job de despliegue de `main` o de tags, con un Environment de GitHub; los pull requests corren en runners hospedados; `cd.yml` no se dispara con `pull_request`; permisos mínimos por job; aprobación obligatoria para workflows de colaboradores externos. | Revisión de `cd.yml` (`runs-on` del runner autohospedado solo en el job de despliegue y ningún disparador `pull_request`). Previsto — responsable: Persona 3. |
| A9 | Candidata: cualquier persona de la red alcanza el puerto 6080 y controla el cliente de otro jugador, o accede al contenedor desde fuera de la red del equipo. | Suplantación y elevación de privilegios | Flujo navegador → noVNC (6080), `mc2-front` | Existe: el [`Dockerfile`](../Dockerfile) actual ejecuta el proceso con la cuenta `app`, sin privilegios de root. Previsto — responsable: Persona 3: decidir qué interfaz publica el puerto 6080 (red local, no pública) y si noVNC exige contraseña; no se agregan cuentas al juego. | `docker compose exec mc2-front id` (cuenta distinta de root) y `docker compose ps` con los puertos publicados. Previsto — responsable: Persona 3. |

### 2.1 Autenticación y autorización

- **Jugadores.** El juego no tiene cuentas ni roles: el Corte 2 es una aplicación de un solo jugador local y el alcance del Corte 3 excluye funcionalidades de negocio nuevas. Por eso no hay autenticación ni autorización de jugador que verificar, y no se inventa un inicio de sesión. La consecuencia, aceptada por alcance, es que el protocolo TCP (7777) y noVNC (6080) no autentican a quien se conecta; la mitigación es limitar su exposición a la red del equipo y que el servidor asigne los identificadores (A4, A9). Decisión registrada en el [ADR-002](adr/ADR-002-despliegue.md). Previsto — responsable: Persona 2: confirmar el texto cuando el protocolo esté implementado.
- **Identidades que sí existen.** Cuentas de GitHub de los integrantes (aprobación de pull requests y protección de `main`), el token del workflow que publica en GitHub Container Registry, el registro del runner autohospedado y los Environments con sus secretos. Su autorización es: quién puede fusionar a `main`, qué job puede escribir paquetes y qué job puede usar el runner.

## 3. Controles y umbrales

Los cuatro controles corren en el pipeline [`ci.yml`](../../.github/workflows/ci.yml) como quality gates. Los umbrales mínimos son los del enunciado (sección 4.2); cada grupo puede endurecerlos, nunca relajarlos. Las herramientas son las propuestas del [ADR-003](adr/ADR-003-seguridad.md), que sigue en estado propuesto: la primera ejecución sobre el repositorio real (sección 3.2) las pone a prueba, pero no sustituye la demostración de que cada gate bloquea un cambio inseguro.

> **Estado de esta sección.** Los cuatro jobs existen y se ejecutaron por primera vez en el pull request #1 ([ejecución](https://github.com/MarshallGomez1103/Mc2/actions/runs/43487176016), commit `b9f06d3`, 2026-11-13): los cinco jobs terminaron en verde y los cuatro controles no reportaron hallazgos. Siguen como Previsto la demostración de que cada gate falla ante un cambio inseguro ([sección 5](#5-evidencia-pull-request-bloqueado)) y que el ruleset de `main` exija estos checks: Previsto — responsable: Persona 1 (ruleset) y Persona 2 (demostraciones).

El workflow se dispara con `pull_request` hacia `main`, con `push` a `main` y con `workflow_dispatch`. Los jobs se llaman `build-pruebas`, `sast`, `sca`, `secretos` e `imagen`; el id de cada job es el nombre de su check y no se renombra sin actualizar el ruleset de `main` que lo referencie. Que ese ruleset exista y marque los checks como obligatorios no está evidenciado todavía: Previsto — responsable: Persona 1.

| Control (job) | Qué detecta | Umbral mínimo (enunciado) | Umbral adoptado | Herramienta | Cómo detiene el pipeline |
| --- | --- | --- | --- | --- | --- |
| Análisis estático (`sast`) | Vulnerabilidades en el código propio. | Ningún hallazgo alto o crítico sin tratar. | Ningún resultado con `security-severity` de 7.0 o más (bandas alta y crítica de GitHub) sin una excepción registrada. | CodeQL para Java mediante `github/codeql-action` v4.39.3 (CLI 2.27.4 incluida en la acción), con compilación manual de `Minecraft2/`. | CodeQL solo escribe el SARIF; [`scripts/umbral-sast.sh`](../../scripts/umbral-sast.sh) lo lee y termina con error si queda un resultado de 7.0 o más sin excepción. |
| Análisis de dependencias (`sca`) | Librerías con vulnerabilidades conocidas. | Ninguna vulnerabilidad alta o crítica con corrección disponible. | Igual al mínimo: `HIGH` y `CRITICAL` con corrección disponible (`--ignore-unfixed`). | Trivy 0.76.2 en modo sistema de archivos (`trivy fs`) sobre `Minecraft2/`. | [`scripts/sca.sh`](../../scripts/sca.sh): código de salida 1 de Trivy (`--exit-code 1`); además falla si Trivy no analizó ningún `pom.xml`. |
| Detección de secretos (`secretos`) | Contraseñas, tokens y llaves en el código y en el historial. | Cero secretos, incluido el historial. | Igual al mínimo: cero hallazgos en todas las referencias descargadas (`--log-opts=--all`, checkout con `fetch-depth: 0`). | Gitleaks 8.30.1 (`gitleaks git`). | [`scripts/secretos.sh`](../../scripts/secretos.sh): código de salida 1 de Gitleaks (`--exit-code 1`); además se niega a ejecutarse sobre un clon superficial o sin commits. |
| Escaneo de la imagen (`imagen`) | Vulnerabilidades del sistema operativo y de las librerías dentro del contenedor. | Ninguna vulnerabilidad crítica con corrección disponible. | Igual al mínimo: `CRITICAL` con corrección disponible (`--ignore-unfixed`). | Trivy 0.76.2 sobre el archivo `docker save` de la imagen construida con el [`Dockerfile`](../Dockerfile). | [`scripts/escaneo-imagen.sh`](../../scripts/escaneo-imagen.sh): código de salida 1 de Trivy (`--exit-code 1`). |

El job `build-pruebas` no es un control de seguridad, pero comparte el workflow: ejecuta `mvn -B -ntp verify` (pruebas unitarias, de integración y de arquitectura, y el umbral de cobertura de líneas del dominio) y falla si la comprobación de JaCoCo no se ejecutó, porque esa comprobación se omite en silencio cuando falta el archivo de datos de las pruebas unitarias. Su detalle está en [devops.md](devops.md).

Reglas:

- Los umbrales no se relajan. Deshabilitar una regla o bajar un umbral sin justificarlo cuenta como un control no implementado; toda excepción se registra en la [sección 4](#4-registro-de-hallazgos) con su justificación.
- Cuando una herramienta solo reporta (por ejemplo, CodeQL, que aquí no publica en la pestaña Security), un paso adicional interpreta el resultado y falla si se supera el umbral. Publicar alertas no sustituye el gate.
- Ningún job usa `continue-on-error`: un control que no puede fallar no es un control.
- Los controles de dependencias, secretos e imagen son scripts del repositorio (`scripts/`) que descargan su herramienta fijada. Los de dependencias y secretos se niegan a correr fuera de GitHub Actions o de un contenedor, para no instalar nada en el equipo de un integrante.

### 3.1 Qué bloquea cada control y qué no

| Job | Bloquea cuando | No bloquea (límite conocido) |
| --- | --- | --- |
| `sast` | Hay al menos un resultado de CodeQL con `security-severity` de 7.0 o más sin excepción registrada; el SARIF falta, no se puede leer o no trae ninguna ejecución; o CodeQL no extrajo ningún archivo Java. | Los resultados de severidad menor a 7.0. Los resultados cuya regla no declara `security-severity` cuentan como 0. Solo se analiza `Minecraft2/src/`: los archivos de `Minecraft2/test/` no se compilan para CodeQL. Se usa la suite de consultas por defecto de la acción, sin `security-extended`. |
| `sca` | Una dependencia de `Minecraft2/pom.xml`, directa o transitiva, tiene una vulnerabilidad `HIGH` o `CRITICAL` con versión corregida; Trivy falla; o el reporte no contiene ningún resultado de tipo `pom`. | Las vulnerabilidades sin corrección publicada. Solo se analiza `pom.xml`: no se revisan el JAR final ni otros ecosistemas. Trivy consulta su base de vulnerabilidades en cada ejecución, de modo que una vulnerabilidad publicada después de un job en verde solo aparece en la ejecución siguiente. |
| `secretos` | Gitleaks encuentra una coincidencia en cualquier commit de las referencias descargadas, o el checkout es superficial o sin commits. | Lo que no sea un secreto (datos personales, volcados de datos), y los secretos que las reglas por defecto de Gitleaks no reconozcan: no hay `.gitleaks.toml` propio. Las referencias que existan solo en un clon local. |
| `imagen` | La imagen construida contiene una vulnerabilidad `CRITICAL` con corrección disponible. | Las críticas sin corrección en la imagen base, y las de gravedad `HIGH` o inferior. Se analiza la imagen final (sistema operativo y JAR de la aplicación) con el escáner de vulnerabilidades: no revisa secretos ni configuración. La imagen que se escanea es la construida en CI; aún no existe una imagen publicada. |

Los cuatro jobs corren solo ante pull requests, `push` a `main` y ejecución manual: no hay una ejecución programada, de modo que una imagen o una dependencia no se reescanean mientras no haya cambios.

El umbral de `sast` usa la puntuación `security-severity` que CodeQL escribe en el SARIF; el script busca los metadatos de las reglas en `tool.driver.rules` y en `tool.extensions[].rules`. La primera ejecución no produjo ningún resultado, de modo que esa lectura todavía no se ha comprobado con un hallazgo real: Previsto — responsable: Persona 1 (la comprobación se hace con la demostración de la sección 5 o con una consulta de CodeQL que dispare a propósito).

### 3.2 Primera ejecución y estado del historial

Resultados de la [ejecución](https://github.com/MarshallGomez1103/Mc2/actions/runs/43487176016) del pull request #1 (commit `b9f06d3`, evento `pull_request`). El pipeline completo duró alrededor de 2 minutos, muy por debajo de los 10 minutos que fija [devops.md](devops.md).

| Job | Duración | Observado |
| --- | --- | --- |
| `build-pruebas` | 39 s | 218 pruebas unitarias y 148 de integración sin fallos; `All coverage checks have been met`; `ArchitectureBoundaryTest`: 14 pruebas. |
| `sast` | 1 min 57 s | CodeQL extrajo 90 archivos Java (los de `Minecraft2/src/`; la acción informa «90 out of 164», los 74 restantes son pruebas); el umbral informó 0 hallazgos altos o críticos y 0 sin tratar. |
| `sca` | 28 s | Trivy 0.76.2 analizó un `pom.xml`; mensaje final `SCA gate PASSED: no HIGH/CRITICAL vulnerability with a fix available`. |
| `secretos` | 6 s | 61 commits alcanzables desde todas las referencias descargadas; Gitleaks 8.30.1: `no leaks found`. |
| `imagen` | 1 min 42 s | Imagen `mc2-server:ci` construida (la construcción ejecuta las pruebas unitarias del proyecto); Trivy analizó Ubuntu 24.04 (136 paquetes) y `app/app.jar`: 0 vulnerabilidades críticas con corrección. |

Todos los jobs terminaron en verde sin necesitar excepciones. Los hallazgos registrados de esta ejecución son **cero**, por lo que la [sección 4](#4-registro-de-hallazgos) sigue vacía.

Antes de esta ejecución, el escaneo de secretos se probó en local con Gitleaks sobre el historial completo, con cero coincidencias; el job de GitHub confirma el resultado sobre las referencias que el checkout descarga. Gitleaks no está diseñado para señalar datos personales: los nombres y las direcciones de correo de los autores de los commits están en el historial, y el reporte del job (que solo se publica si falla) los incluye. La decisión sobre esa exposición, que no implica rotar credenciales, no se ha tomado: Previsto — responsable: Persona 2. Reescribir el historial no se plantea sin una decisión explícita del equipo, tal como establece el [ADR-003](adr/ADR-003-seguridad.md).

### 3.3 Excepciones del análisis estático

Una excepción del SAST se registra en dos lugares y el script exige los dos:

1. Una línea `<id de la regla> <ruta del archivo>` en `.github/codeql/excepciones-sast.txt`, con la ruta tal como aparece en el SARIF (relativa a la raíz del repositorio, por ejemplo `Minecraft2/src/...`). El archivo no existe: hoy no hay excepciones.
2. Una mención de la regla y de la ruta en este documento; la fila del [registro de hallazgos](#4-registro-de-hallazgos) debe incluirlas, con la decisión (falso positivo o riesgo aceptado) y su justificación.

Si falta cualquiera de los dos, el hallazgo bloquea. Límites del mecanismo: la excepción vale para el par regla-archivo, no para una línea concreta, y silencia los hallazgos futuros de esa regla en ese archivo, por lo que cada registro debe decir qué código se revisó; y el script solo comprueba que la regla y la ruta aparezcan como texto en este documento, no que estén en la tabla de la sección 4, así que el contenido del registro sigue dependiendo de la revisión. Para Gitleaks y Trivy no hay excepciones: Gitleaks lee un `.gitleaksignore` y los scripts de Trivy un `.trivyignore` en la raíz solo si existen, y hoy no existe ninguno. Cualquier excepción de esos escáneres se crea en el mismo cambio que su fila del registro.

### 3.4 Versiones de las herramientas y verificación de las descargas

| Pieza | Versión fijada | Cómo se verifica |
| --- | --- | --- |
| Trivy | 0.76.2 | `sca.sh` descarga el archivo y el `trivy_0.76.2_checksums.txt` de la misma publicación oficial, exige una única línea para el archivo y la comprueba con `sha256sum -c --strict`. `escaneo-imagen.sh` compara con un SHA-256 escrito en el propio script. |
| Gitleaks | 8.30.1 | `secretos.sh` aplica la misma verificación con `gitleaks_8.30.1_checksums.txt` de la publicación. |
| Acciones de GitHub | `actions/checkout` v7.0.1, `actions/setup-java` v6.2.0, `actions/upload-artifact` v7.0.2, `github/codeql-action` v4.39.3 | Cada `uses:` apunta a un SHA completo de commit; la etiqueta queda como comentario. |
| Imágenes base del `Dockerfile` | `maven:3.9.16-eclipse-temurin-17-noble` y `eclipse-temurin:17.0.20.1_1-jre-noble` | Versión fija en el [`Dockerfile`](../Dockerfile). Sin resumen (digest) fijado: Previsto — responsable: Persona 3. |

La suma de comprobación detecta un archivo corrupto o cambiado respecto de la publicación, pero el archivo de sumas viene de la misma publicación que la herramienta, por lo que no prueba el origen de la versión; la verificación de firma con las firmas sigstore que publica Trivy no se ha hecho: Previsto — responsable: Persona 1. El runner es `ubuntu-latest`, sin versión fija. Las versiones se actualizan a mano, cambiando versión y hash en el mismo commit; la automatización con Dependabot queda Previsto — responsable: Persona 1.

### 3.5 Reglas de seguridad del propio workflow

- **Permisos mínimos.** El workflow declara `permissions: contents: read` y ningún job agrega permisos. El job `sast` no publica en la pestaña Security: guarda el SARIF como artefacto (`upload: never`), de modo que no necesita `security-events: write` y los pull requests de forks pueden ejecutarlo. Si el equipo decide publicar en Security, solo ese job añade ese permiso. Decisión: Previsto — responsable: Persona 1.
- **Acciones fijadas por SHA** completo, con la etiqueta como comentario.
- **Sin `pull_request_target`.** Código de un pull request nunca se ejecuta con los secretos ni con el token del repositorio con escritura.
- **Sin `docker.sock`.** La imagen se exporta con `docker save` y Trivy lee el archivo; no se monta `/var/run/docker.sock` en ningún contenedor, porque el socket equivale a root en el equipo anfitrión.
- **Credenciales fuera del repositorio.** Todos los checkouts usan `persist-credentials: false`. El workflow no lee secretos de GitHub.
- **Tiempos máximos y concurrencia.** Cada job tiene `timeout-minutes` (10 o 15), y una ejecución nueva de un pull request cancela la anterior de la misma referencia.
- **Reportes.** Los reportes de pruebas, SARIF, SCA e imagen se publican como artefactos durante 14 días, también si el job falla. El reporte de Gitleaks incluye autor y correo de cada commit señalado; se publica solo si el job falla y se conserva 7 días. Con `--redact` el valor del secreto no aparece, pero los metadatos del commit sí.
- **Alcance del control.** Los jobs corren en runners hospedados por GitHub. El runner autohospedado del Corte 3 no ejecuta ningún job de este workflow (A8).

### 3.6 Demostración: la credencial es siempre falsa

Para demostrar que el gate de secretos detiene un cambio inseguro ([sección 5](#5-evidencia-pull-request-bloqueado)) se usa una credencial **inventada**, sin relación con ningún sistema real. Nunca se usa un secreto real, ni siquiera uno ya revocado, porque lo que entra en un pull request queda en el historial público. Reglas de la demostración, todas Previsto — responsable: Persona 2:

- Antes de abrir el pull request se comprueba en local, con Gitleaks 8.30.1, que la credencial falsa es detectada: las reglas de Gitleaks ignoran valores con apariencia de ejemplo, de modo que no basta con escribir cualquier cadena.
- El pull request se cierra sin fusionar y **la rama de la demostración se borra del remoto**. El job `secretos` descarga todas las ramas y escanea todas las referencias descargadas: mientras la rama exista, el check `secretos` falla también en `main`.
- Como la credencial falsa deja de estar accesible desde `main` al borrar la rama, no hace falta una excepción de Gitleaks. Si se prefiriera conservar la rama como evidencia, habría que decidir primero cómo evitar que bloquee `main`; el enlace al pull request y la captura del check rojo bastan como evidencia.
- Las demostraciones de los otros tres gates (un umbral más estricto o una dependencia o imagen con una vulnerabilidad conocida) también quedan Previsto: la ejecución del pull request #1 solo prueba que los controles pasan con el código actual, no que fallen ante un problema.

## 4. Registro de hallazgos

Cada hallazgo se registra con una de tres decisiones: **corregido** (con enlace al commit), **falso positivo** (con la justificación técnica) o **riesgo aceptado** (con la justificación y lo que haría falta para corregirlo).

| ID | Herramienta | Hallazgo | Decisión (corregido, falso positivo o riesgo aceptado) | Justificación o commit |
| --- | --- | --- | --- | --- |

La tabla está vacía a propósito: se llena solo con los hallazgos de ejecuciones reales de los controles. Previsto — responsable: Persona 2.

## 5. Evidencia: pull request bloqueado

Al menos un pull request introduce a propósito un problema de seguridad (por ejemplo, una llave falsa en un archivo de configuración o una dependencia con una vulnerabilidad conocida), el pipeline lo bloquea y se cierra sin fusionar.

| Enlace al pull request | Qué introduce | Check que lo bloquea | Estado |
| --- | --- | --- | --- |
| Previsto — responsable: Persona 2 | Previsto — responsable: Persona 2 | Previsto — responsable: Persona 2 | Cerrado sin fusionar: Previsto — responsable: Persona 2 |

## 6. Riesgos aceptados

Un riesgo se acepta solo con decisión registrada en la sección 4. Hasta tener ejecuciones reales no hay ninguno decidido. Candidatos que deben evaluarse y, si se aceptan, documentarse con lo que haría falta para corregirlos: ausencia de autenticación en el puerto 7777 y en noVNC (sección 2.1), disponibilidad y exposición del runner autohospedado (A8) y rendimiento del front con OpenGL por software. Previsto — responsable: Persona 2.

| Riesgo | Hallazgo o amenaza | Justificación | Qué haría falta para corregirlo |
| --- | --- | --- | --- |

Los riesgos aceptados que sigan abiertos se repiten en las recomendaciones para producción de la [guía de implementación](implementacion.md#9-recomendaciones-para-producción).

## 7. Análisis dinámico (bonificación)

OWASP ZAP (baseline) contra el entorno desplegado, con objetivos HTTP reales: la interfaz noVNC (6080) y `/health` (8080). Sus hallazgos se tratan igual que los de la sección 4. Previsto — responsable: Persona 2.
