# Seguridad de Minecraft2

Este documento recoge el modelo de amenazas del sistema, los cuatro controles de seguridad del pipeline con sus umbrales, el registro de hallazgos y la evidencia de que los controles detienen un cambio inseguro. Lo lee quien revisa la seguridad del proyecto o decide si un riesgo se acepta. La justificación de las herramientas y de los umbrales está en el [ADR-003](adr/ADR-003-seguridad.md); el pipeline, en [devops.md](devops.md).

> **Estado del documento: esqueleto.** Los controles aún no corren en el pipeline y no existe ningún hallazgo registrado. Cada elemento marcado «Previsto — responsable: Persona N» se completa con ejecuciones reales y el marcador se retira antes de la entrega. Las amenazas de la sección 2 son **candidatas por evaluar**: no son vulnerabilidades confirmadas.

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

Los cuatro controles corren en el pipeline como quality gates. Los umbrales mínimos son los del enunciado (sección 4.2); cada grupo puede endurecerlos, nunca relajarlos. Las herramientas son una propuesta, pendiente de confirmar al probarlas, y su elección se justifica en el [ADR-003](adr/ADR-003-seguridad.md).

| Control | Qué detecta | Umbral mínimo (enunciado) | Umbral adoptado | Herramienta | Cómo detiene el pipeline |
| --- | --- | --- | --- | --- | --- |
| Análisis estático (SAST) | Vulnerabilidades en el código propio. | Ningún hallazgo alto o crítico sin tratar. | Igual al mínimo. | Previsto — responsable: Persona 2 (referencia del taller: CodeQL) | Previsto — responsable: Persona 1: paso que lee el SARIF y falla si hay un hallazgo alto o crítico sin tratar. |
| Análisis de dependencias (SCA) | Librerías con vulnerabilidades conocidas. | Ninguna vulnerabilidad alta o crítica con corrección disponible. | Igual al mínimo. | Previsto — responsable: Persona 2 (referencia del taller: Trivy fs) | Previsto — responsable: Persona 1: código de salida de la herramienta. |
| Detección de secretos | Contraseñas, tokens y llaves en el código y en el historial. | Cero secretos, incluido el historial. | Igual al mínimo. | Previsto — responsable: Persona 2 (referencia del taller: Gitleaks, con historial completo) | Previsto — responsable: Persona 1: código de salida de la herramienta. |
| Escaneo de la imagen | Vulnerabilidades del sistema operativo y de las librerías dentro del contenedor. | Ninguna vulnerabilidad crítica con corrección disponible. | Igual al mínimo. | Previsto — responsable: Persona 2 (referencia del taller: Trivy image) | Previsto — responsable: Persona 1: código de salida de la herramienta. |

Reglas:

- Los umbrales no se relajan. Deshabilitar una regla o bajar un umbral sin justificarlo cuenta como un control no implementado; toda excepción se registra en la sección 4 con su justificación.
- Cuando una herramienta solo reporta (por ejemplo, CodeQL en la pestaña Security, o Dependabot), un paso adicional interpreta el resultado y falla si se supera el umbral. Publicar alertas no sustituye el gate.
- Antes de bloquear el pipeline se prueban sobre el repositorio real el escaneo de secretos con el historial completo y el escaneo de la imagen base: Previsto — responsable: Persona 2.

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
