# Seguridad de Minecraft2

Actualización de Persona 2, 16/11/2026. Base revisada `522bd4c`, sobre main `e618961`; E se fusionó mediante PR #15 en main `9d44297`; F añade el baseline y la corrección HTTP en un incremento posterior. Responsables: Jasub (modelo, mitigaciones y registro), Thomas (scripts/workflow y revisión), Ethian (contenedores/CD/runner). [ADR-003](adr/ADR-003-seguridad.md) explica decisiones y alternativas.

**Estado:** servidor y cliente implementados, amenazas contrastadas con código y pruebas. Los cuatro gates ya funcionan. Hay riesgos residuales y vulnerabilidades no críticas en la imagen: «cinco checks verdes» no significa «cero hallazgos». La evidencia negativa de secretos ya existe en el PR #5 cerrado sin merge y se revalida en esta fase; no se publica otra llave ficticia. La revisión cruzada de este documento/ADR y la validación del despliegue siguen a cargo del equipo. No se presentan como realizadas.

## 1. Componentes y fronteras de confianza

Se parte de [contenedores de C2](diagramas/recuperacion-c2/02-contenedores.md) y de [ADR-002](adr/ADR-002-despliegue.md), actualizados a la implementación B/C/D. El binario actual tiene dos modos: cliente gráfico y servidor headless, no una API web de negocio.

```text
Jugador/navegador -> noVNC (front de Ethian, PR #2 pendiente)
                         -> cliente LibGDX
Jugador/cliente TCP ------> network.GameServer :7777
                              | conexión -> identidad asignada
                              | input validado -> cola acotada
                              v
                         hilo de simulación -> GameSession -> dominio
                              |                                  |
                              +-- state/ack por conexión         +-> JsonWorldStorage -> worlds/server.json
Operador/monitor ----------> HTTP :8080 /health y /metrics (lectura)
Código/PR -> CI hospedado -> gates -> main -> CD/GHCR/runner (integración de P3 pendiente)
```

TCP/HTTP enlazan todas las interfaces mediante InetSocketAddress(port): su acceso debe restringirse mediante red/firewall/publicación de puertos, no mediante MC2_SERVER_HOST (destino del cliente). No se ha comprobado una regla de firewall del equipo en E. El cliente y los sockets son entradas no confiables. Los JSON locales y MC2_WORLD_DIR los administra el operador, no el jugador. GitHub y el equipo del runner son otra frontera, con identidades distintas de las del juego.

| Superficie | Estado real y límite |
| --- | --- |
| TCP7777 / HTTP8080 | Implementados en main; puertos configurables. HTTP solo GET exacto a health/metrics; no tiene carga/guardado administrativo ni autenticación. |
| Estado compartido | IDs, nombres, skins y mundo de los jugadores se difunden como parte de la partida; no son identidades autenticadas. |
| JSON local | Solo mundo `server` elegido en ServerSimulation; sin nombre de archivo recibido por TCP. Validación de ID en JsonWorldStorage. No hay límite general de bytes del archivo ni rechazo de symlinks: R-FILE. |
| Front noVNC, Compose y CD | Propuesta de Ethian en PR #2; no inferir que está desplegada por haber probado contenedores en C. Contraseña, interfaces y TLS del entorno final requieren verificación en F/G. |
| CI | ci.yml ejecuta PR/main/manual en GitHub-hosted; contents:read, sin secretos del despliegue, checkout sin persistir credenciales. Dependabot semanal ya existe. |

## 2. Modelo de amenazas STRIDE

Una mitigación de código no demuestra por sí sola que la red del despliegue esté restringida. Los riesgos residuales remiten a decisiones de la sección4.

| ID | Amenaza específica | STRIDE | Componente/flujo | Mitigación real y residual | Cómo se verifica |
| --- | --- | --- | --- | --- | --- |
| A1 | Conectarse usando el nombre de otro jugador, observar o suplantar la sesión desde la LAN. | Suplantación | hello TCP7777, cliente/noVNC | El servidor asigna ID y skin0–2 validada. El nombre no autentica; TCP no tiene TLS ni credencial: R-AUTH. Restringir puertos es condición operativa pendiente, no propiedad garantizada por Java. | ProtocolCodecTest; GameServerIT/MultiplayerServerIT. Verificar controles de exposición con Ethian en F/G. |
| A2 | Enviar playerId ajeno o repetir una acción para mover otro jugador o duplicar efectos. | Elevación de privilegios / manipulación | input -> Connection -> GameSession | Esquema rechaza playerId y campos desconocidos; conexión determina el ID, seq creciente por conexión, ack solo después de aplicar, un hilo muta el mundo. No hay roles administrativos del juego. | ProtocolCodecTest; GameServerIT helloThenInputAppliesExactlyOnceAndAckDoesNotRegress y prueba de identidad falsa; MultiplayerServerIT acks aislados. |
| A3 | Ocho pares lentos o líneas enormes agotan lectores/memoria y bloquean nuevos jugadores. | Denegación de servicio | Socket7777, colas input/output | Máx8 conexiones/lectores/escritores; línea4096 bytes, input32, output4, HTTP2 trabajadores/cola16. Timeout socket5s handshake y30s posterior. Son tiempos de inactividad por lectura, no plazos absolutos: goteo lento y reconexión siguen como R-DOS. | ProtocolCodecTest límites UTF8; GameServerIT octavo/noveno y línea4097; métricas tick_failures=0 y dueño sigue respondiendo. No es ensayo de DoS distribuido ni prueba de todas las colas. |
| A4 | Ángulos infinitos, tipos falsos, JSON duplicado/profundo o materiales inválidos alteran el tick. | Manipulación | ProtocolCodec/JsonParser -> PlayerFrameInput | Long para seq1..2^53−1, booleanos estrictos, ángulos finitos/acotados, AIR rechazado, parser profundidad64 y duplicados inválidos. El fallo cierra al par sin modificar otra sesión. | ProtocolCodecTest; JsonParserTest; GameServerIT entradas abusivas y salud posterior. |
| A5 | Un nombre de campo JSON con salto de línea falsifica un evento del log. | Repudio / manipulación | error del parser -> GameServer.Connection.read | Hallazgo E-LOG confirmado con prueba roja. Se elimina failure.getMessage de la salida del lector; queda evento fijo, ID interno y clase de excepción. No hay auditoría criptográfica ni atribución personal: R-AUDIT. | GameServerIT malformedMessagesCannotForgeLogEntries falla antes y pasa después; log/XML conservados. Commit de corrección en sección4. |
| A6 | Leer rutas arbitrarias por nombre de mundo o alterar JSON local para forzar consumo excesivo. | Divulgación / manipulación / DoS | JsonWorldStorage y volumen worlds | Red no permite seleccionar rutas; ID local restringido a [a-zA-Z0-9_-]+ y errores TCP se cierran sin devolver trazas. Archivo local confiado al operador; tamaño/symlinks pendientes R-FILE. | JsonWorldStorageTest rechazaIdentificadoresInvalidos, InvalidWorldFileTest y humo JAR; revisión ServerSimulation usa nombre constante server. No se afirma un endpoint de archivos por red. |
| A7 | Métricas o errores HTTP/TCP revelan nombres, rutas o detalles internos. | Divulgación | health/metrics/logs y state | HTTP devuelve salud y agregados, nosniff/no-store; rutas extra404 y métodos distintosGET405. Sin nombres en métricas; error TCP cierra sin traceback. State sí publica nombres/estado del mundo por diseño; R-AUTH cubre acceso no autorizado. | GameServerIT privacidad de métricas y cabeceras; revisión ServerMetrics/respond. ZAP posterior solo analiza HTTP. |
| A8 | Dependencia o base vulnerable llega al JAR/imagen; credencial en historial o acción móvil altera la entrega. | Manipulación / divulgación | Maven, Dockerfile, CI y Git | Gates CodeQL+umbral, Trivy SCA/imagen y Gitleaks historial completo. Acciones por SHA; herramientas con checksums. Imagen con versiones, no digest; riesgos reales en tabla R-IMG y confianza en mismo origen de checksums. | Reportes completos de Actions; PR5 bloqueado; fixtures SARIF prueban salida no cero con alto/crítico. No se afirma ensayo vulnerable real para SCA/imagen. |
| A9 | Código de PR accede a la máquina del runner o a secretos de publicación. | Elevación de privilegios | CI -> runner/CD/GHCR | CI corre en GitHub-hosted, contents:read y sin pull_request_target. Para CD exigir main/tags confiables, Environment y permisos mínimos; revisar definición final P3: R-RUNNER. | Revisión ci.yml actual; PR2/registro runner y políticas del Environment pendientes F/G. No se certifica CD antes de integrarlo. |

### 2.1 Autenticación y autorización

**Autenticación de jugador:** no implementada; name/skinId son presentación. Un ID asignado por el servidor identifica una conexión, no prueba quién es la persona. TCP y HTTP no cifran; noVNC del entorno final no se certifica aquí. Se conserva el alcance académico de juego en red restringida de ADR002 y se registra R-AUTH, sin describirlo como seguro para Internet. Añadir cuentas/TLS/proxy o VPN autenticada requiere diseño y coordinación; no se inventa un login.

**Autorización que sí existe:** cada Connection controla exclusivamente el jugador que GameSession.join le asignó; no se acepta un ID del cliente ni una operación administrativa de archivos. join/leave/advanceAll y snapshots corren bajo el hilo autoritativo. No existen roles de jugador ni administración remota del servidor. GitHub sí tiene identidades y autorización separada (revisar PR, fusionar main, publicar paquetes, usar Environment/runner). La aprobación cruzada debe hacerla otro integrante; Jasub no se autoaprueba.

## 3. Controles y umbrales

Los mínimos no se relajan y el verde no equivale a ausencia de todas las vulnerabilidades.

| Job | Herramienta fijada en la base revisada | Umbral y salida que bloquea |
| --- | --- | --- |
| sast | CodeQL acción SHA33d7b85… (v4.39.3) con build manual Java; SARIF como artefacto, upload:never | scripts/umbral-sast.sh: security-severity >=7.0 sin tratamiento registrado => exit1. SARIF faltante/inválido/sin runs => fallo; workflow exige extracción Java no vacía. |
| sca | Trivy0.76.2 fs; precarga Maven y reporte completo | HIGH/CRITICAL con corrección, --ignore-unfixed --exit-code1. Falla si no analizó ningún pom; dependencias transitivas según resolución Maven. |
| secretos | Gitleaks8.30.1 git, --log-opts=--all y --redact | Cero hallazgos; exit1, rechaza clon superficial/sin commits. fetch-depth0. Incluye referencias descargadas, no objetos inalcanzables de GitHub. |
| imagen | Trivy0.76.2 sobre docker save de Minecraft2/Dockerfile | CRITICAL con corrección => exit1; reporte completo incluye LOW/MEDIUM/HIGH que deben registrarse. El job también puede fallar al compilar/probar; eso no demuestra una CVE. |

### 3.2 Evidencia actual y alcance

Base D522bd4c, [Actions43978487608](https://github.com/MarshallGomez1103/Mc2/actions/runs/43978487608), intento2: cinco checks verdes. SARIF con cero resultados de vulnerabilidad; SCA con cero vulnerabilidades reportadas; imagen **59 hallazgos (2 HIGH,53 MEDIUM,4 LOW), cero CRITICAL**. Se registran individualmente debajo. Secretos sin hallazgos según log; el artefacto de secretos se publica solo si falla. No se interpreta su ausencia como única prueba de éxito: se revisan job/log. Primer intento de SCA falló antes del escáner por HTTP429 de Maven Central; reintento aprobado, sin alterar gates. E ejecuta otra CI y añade sus resultados en [evidencia de la fase](evidencias/fase-e/README.md).

El escaneo corresponde al Dockerfile actual, no certifica Dockerfile.server/front del PR2 ni el equipo del runner. Las bases de CVE cambian; una ejecución pasada no certifica futuras imágenes. El código del control es revisable en scripts/ y ci.yml; no se modifican esos archivos de Thomas en E.

### 3.3 Excepciones del análisis estático

Una excepción del SAST se registra en dos lugares y el script exige los dos:

1. Una línea `<id de la regla> <ruta del archivo>` en `.github/codeql/excepciones-sast.txt`, con la ruta tal como aparece en el SARIF (relativa a la raíz del repositorio, por ejemplo `Minecraft2/src/...`). El archivo no existe: hoy no hay excepciones.
2. Una mención de la regla y de la ruta en este documento; la fila del [registro de hallazgos](#4-registro-de-hallazgos) debe incluirlas, con la decisión (falso positivo o riesgo aceptado) y su justificación.

Si falta cualquiera de los dos, el hallazgo bloquea. Límites del mecanismo: la excepción vale para el par regla-archivo, no para una línea concreta, y silencia los hallazgos futuros de esa regla en ese archivo, por lo que cada registro debe decir qué código se revisó; y el script solo comprueba que la regla y la ruta aparezcan como texto en este documento, no que estén en la tabla de la sección 4, así que el contenido del registro sigue dependiendo de la revisión. Para Gitleaks y Trivy no hay excepciones: Gitleaks lee un `.gitleaksignore` y los scripts de Trivy un `.trivyignore` en la raíz solo si existen, y no existen en la base revisada. Cualquier excepción de esos escáneres se crea en el mismo cambio que su fila del registro.

### 3.4 Versiones de las herramientas y verificación de las descargas

| Pieza | Versión fijada | Cómo se verifica |
| --- | --- | --- |
| Trivy | 0.76.2 | `sca.sh` descarga el archivo y el `trivy_0.76.2_checksums.txt` de la misma publicación oficial, exige una única línea para el archivo y la comprueba con `sha256sum -c --strict`. `escaneo-imagen.sh` compara con un SHA-256 escrito en el propio script. |
| Gitleaks | 8.30.1 | `secretos.sh` aplica la misma verificación con `gitleaks_8.30.1_checksums.txt` de la publicación. |
| Acciones de GitHub | `actions/checkout` v7.0.1, `actions/setup-java` v6.2.0, `actions/upload-artifact` v7.0.2, `github/codeql-action` v4.39.3 | Cada `uses:` apunta a un SHA completo de commit; la etiqueta queda como comentario. |
| Imágenes base del `Dockerfile` | `maven:3.9.16-eclipse-temurin-17-noble` y `eclipse-temurin:17.0.20.1_1-jre-noble` | Versión fija en el [`Dockerfile`](../Dockerfile). Sin resumen (digest) fijado: Previsto — responsable: Persona 3. |

La suma de comprobación detecta un archivo corrupto o cambiado respecto de la publicación, pero el archivo de sumas viene de la misma publicación que la herramienta, por lo que no prueba el origen de la versión; la verificación de firma con las firmas sigstore que publica Trivy no se ha hecho: Previsto — responsable: Persona 1. El runner es `ubuntu-latest`, sin versión fija. Las versiones se actualizan a mano, cambiando versión y hash en el mismo commit; Dependabot ya configura actualizaciones semanales de Maven, acciones y Docker.

### 3.5 Reglas de seguridad del propio workflow

- **Permisos mínimos.** El workflow declara `permissions: contents: read` y ningún job agrega permisos. El job `sast` no publica en la pestaña Security: guarda el SARIF como artefacto (`upload: never`), de modo que no necesita `security-events: write` y los pull requests de forks pueden ejecutarlo. Si el equipo decide publicar en Security, solo ese job añade ese permiso. Decisión: Previsto — responsable: Persona 1.
- **Acciones fijadas por SHA** completo, con la etiqueta como comentario.
- **Sin `pull_request_target`.** Código de un pull request nunca se ejecuta con los secretos ni con el token del repositorio con escritura.
- **Sin `docker.sock`.** La imagen se exporta con `docker save` y Trivy lee el archivo; no se monta `/var/run/docker.sock` en ningún contenedor, porque el socket equivale a root en el equipo anfitrión.
- **Credenciales fuera del repositorio.** Todos los checkouts usan `persist-credentials: false`. El workflow no lee secretos de GitHub.
- **Tiempos máximos y concurrencia.** Cada job tiene `timeout-minutes` (10 o 15), y una ejecución nueva de un pull request cancela la anterior de la misma referencia.
- **Reportes.** Los reportes de pruebas, SARIF, SCA e imagen se publican como artefactos durante 14 días, también si el job falla. El reporte de Gitleaks incluye autor y correo de cada commit señalado; se publica solo si el job falla y se conserva 7 días. Con `--redact` el valor del secreto no aparece, pero los metadatos del commit sí.
- **Alcance del control.** Los jobs corren en runners hospedados por GitHub. El runner autohospedado del Corte 3 no ejecuta ningún job de este workflow (A8).


## 4. Registro de hallazgos y decisiones

Responsable del registro: Jasub. Las decisiones residuales son para el prototipo académico y requieren revisión cruzada; no equivalen a aprobación de seguridad para producción. No se agrega ninguna excepción de CodeQL/Gitleaks/Trivy ni se baja un umbral.

| ID | Origen/hallazgo | Decisión | Justificación, cambio o trabajo necesario |
| --- | --- | --- | --- |
| E-LOG | Revisión/prueba: campo JSON duplicado con salto de línea entra en failure.getMessage y falsifica líneas del log del servidor. | Corregido | [acbff1c](https://github.com/MarshallGomez1103/Mc2/commit/acbff1ccb72d35735d246e28b562df2bbfecf665). Se conserva evento fijo/ID/clase; se elimina el texto no confiable. Rojo antes, verify verde después. |
| R-AUTH | Sin autenticación/cifrado TCP/HTTP; nombre suplantable y estado visible; noVNC sin validación final. | Riesgo aceptado en alcance restringido | Partida académica sin cuentas; no admite exposición pública. Ethian debe confirmar interfaces/firewall/proxy/acceso autenticado; para producción autenticación y TLS/VPN. Si no se restringe la red, la condición de aceptación no se cumple. |
| R-DOS | Timeout por inactividad reiniciable, goteo lento y reconexiones pueden ocupar8 plazas; logs no tienen rate limit. | Riesgo aceptado en alcance restringido | Colas/hilos/tamaño acotados protegen memoria, no garantizan disponibilidad ante atacantes. Añadir plazo absoluto de frame, cuota por origen y rate limit; firewall/VPN operativos antes de exposición. |
| R-FILE | JSON local sin máximo general de bytes ni rechazo explícito de symlink. | Riesgo aceptado para datos del operador | La red solo abre server y no recibe rutas. Un usuario con escritura en el volumen sigue pudiendo alterar datos. Restringir permisos/backup; coordinar límite de archivo y apertura sin seguir enlaces con propietario de persistencia. |
| R-AUDIT | No hay identidad personal autenticada ni auditoría inviolable de cada acción. | Riesgo aceptado en alcance académico | Log fijo mejora integridad de formato, no atribución. Centralizar logs con retención y autenticación de jugadores si se exige auditoría. |
| R-RUNNER | Acceso a host/Environment y aislamiento de noVNC/CD no verificados en main. | Riesgo aceptado solo como pendiente de integración | No declarar entorno apto para producción. P3 debe demostrar runner solo para despliegue de código confiable, permisos y puertos; revisión en F/G. |

### 4.1 Hallazgos de imagen, uno por paquete/CVE

Fuente: artefacto reporte-imagen de Actions43978487608, SHA522bd4c. Identificadores y versiones se transcriben del reporte del escáner; no se atribuye explotación demostrada. **R-IMG:** aceptación temporal por estar debajo del umbral obligatorio de imagen (CRITICAL con corrección), sin ignorar resultados. El proceso Java se ejecuta como app, no ejecuta apt ni herramientas de administración como parte del juego; esto reduce exposición, no prueba inexplotabilidad. Ethian debe actualizar las bases/paquetes con corrección disponible y volver a escanear las imágenes finales; sin corrección, seguir avisos y renovar base cuando exista. Revisión cruzada pendiente. No aplica el umbral SCA HIGH al SO de la imagen: son controles diferentes. Un futuro CRITICAL con corrección bloquea sin excepción.

| ID | CVE | Paquete | Instalada | Corrección reportada | Severidad | Decisión |
| --- | --- | --- | --- | --- | --- | --- |
| IMG-01 | CVE-2026-109518 | apt | 2.8.3 | 2.8.4ubuntu0.1 | HIGH | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-02 | CVE-2026-76642 | bsdutils | 1:2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-03 | CVE-2026-102473 | dash | 0.5.12-6ubuntu5 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-04 | CVE-2026-102474 | dash | 0.5.12-6ubuntu5 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-05 | CVE-2026-109518 | libapt-pkg6.0t64 | 2.8.3 | 2.8.4ubuntu0.1 | HIGH | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-06 | CVE-2026-76642 | libblkid1 | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-07 | CVE-2026-18374 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-08 | CVE-2026-84243 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-09 | CVE-2026-8674 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-10 | CVE-2026-86805 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-11 | CVE-2026-95818 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-12 | CVE-2026-97399 | libc-bin | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-13 | CVE-2026-18374 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-14 | CVE-2026-84243 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-15 | CVE-2026-8674 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-16 | CVE-2026-86805 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-17 | CVE-2026-95818 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-18 | CVE-2026-97399 | libc6 | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-19 | CVE-2025-66382 | libexpat1 | 2.6.1-2ubuntu0.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-20 | CVE-2026-102633 | libexpat1 | 2.6.1-2ubuntu0.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-21 | CVE-2026-67693 | libgnutls30t64 | 3.8.3-1.1ubuntu3.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-22 | CVE-2026-88647 | libgnutls30t64 | 3.8.3-1.1ubuntu3.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-23 | CVE-2026-88648 | libgnutls30t64 | 3.8.3-1.1ubuntu3.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-24 | CVE-2026-95184 | libgnutls30t64 | 3.8.3-1.1ubuntu3.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-25 | CVE-2026-95210 | libgnutls30t64 | 3.8.3-1.1ubuntu3.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-26 | CVE-2026-107778 | libgssapi-krb5-2 | 1.20.1-6ubuntu2.10 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-27 | CVE-2026-107778 | libk5crypto3 | 1.20.1-6ubuntu2.10 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-28 | CVE-2026-107778 | libkrb5-3 | 1.20.1-6ubuntu2.10 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-29 | CVE-2026-107778 | libkrb5support0 | 1.20.1-6ubuntu2.10 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-30 | CVE-2026-76642 | libmount1 | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-31 | CVE-2026-103111 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-32 | CVE-2026-86145 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-33 | CVE-2026-89156 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-34 | CVE-2026-89157 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-35 | CVE-2026-89158 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-36 | CVE-2026-89160 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-37 | CVE-2026-89161 | libpcre2-8-0 | 10.42-4ubuntu2.1 | 10.42-4ubuntu2.2 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-38 | CVE-2026-107161 | libsasl2-2 | 2.1.28+dfsg1-5ubuntu3.1 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-39 | CVE-2026-107161 | libsasl2-modules-db | 2.1.28+dfsg1-5ubuntu3.1 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-40 | CVE-2026-76642 | libsmartcols1 | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-41 | CVE-2026-121876 | libsystemd0 | 255.4-1ubuntu8.18 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-42 | CVE-2026-40228 | libsystemd0 | 255.4-1ubuntu8.18 | No disponible | LOW | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-43 | CVE-2026-121876 | libudev1 | 255.4-1ubuntu8.18 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-44 | CVE-2026-40228 | libudev1 | 255.4-1ubuntu8.18 | No disponible | LOW | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-45 | CVE-2026-76642 | libuuid1 | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-46 | CVE-2026-18374 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-47 | CVE-2026-84243 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-48 | CVE-2026-8674 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-49 | CVE-2026-86805 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-50 | CVE-2026-95818 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-51 | CVE-2026-97399 | locales | 2.39-0ubuntu8.9 | 2.39-0ubuntu8.12 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-52 | CVE-2024-56433 | login | 1:4.13+dfsg1-4ubuntu3.2 | No disponible | LOW | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-53 | CVE-2026-76642 | mount | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-54 | CVE-2024-56433 | passwd | 1:4.13+dfsg1-4ubuntu3.2 | No disponible | LOW | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-55 | CVE-2026-82560 | perl-base | 5.38.2-3.2ubuntu0.6 | 5.38.2-3.2ubuntu0.7 | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-56 | CVE-2026-18477 | tar | 1.35+dfsg-3ubuntu0.4 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-57 | CVE-2026-18508 | tar | 1.35+dfsg-3ubuntu0.4 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-58 | CVE-2026-76642 | util-linux | 2.39.3-9ubuntu6.6 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |
| IMG-59 | CVE-2021-31879 | wget | 1.21.4-1ubuntu4.5 | No disponible | MEDIUM | Riesgo aceptado temporal R-IMG; actualizar/seguir base (P3) |

## 5. Evidencia: pull request bloqueado

La evidencia requerida ya la preparó el equipo: [PR5](https://github.com/MarshallGomez1103/Mc2/pull/5), commit9130728, [Actions43736948906](https://github.com/MarshallGomez1103/Mc2/actions/runs/43736948906). Revalidado en E: cerrado, merged=false, secretos falla y los otros cuatro jobs pasan. Introdujo exclusivamente una credencial inventada en configuración; no se reproduce su valor. Se preservan metadatos, reporte redactado y logs. No es un secreto de un servicio real ni se registra como vulnerabilidad de main.

La rama se eliminó después del ensayo para no contaminar el escaneo --all de otras ramas; el PR/run conserva evidencia. No se crea otro PR ni se añade una excepción de Gitleaks. Una credencial real exigiría rotación; borrar un archivo no limpia el historial. El histórico [PR4](https://github.com/MarshallGomez1103/Mc2/pull/4) demostró cobertura, no un gate de vulnerabilidades de imagen; su fallo simultáneo de secretos provino de la rama5 todavía presente. Se conserva esa distinción.

Además E ejecutó el script real umbral-sast.sh con cinco reportes SARIF sintéticos:6.9 pasa;7.0 y9.5 fallan;sin runs falla;reporte válido sin resultados pasa. Es evidencia local del umbral y del tratamiento de un reporte, no un hallazgo real de CodeQL ni un PR SAST vulnerable. SCA/imagen se verifican por sus comandos bloqueantes, reportes y ejecución positiva; **no se afirma una demostración negativa real de esos dos escáneres**. La rúbrica pide al menos un PR inseguro bloqueado: lo satisface #5.

## 6. Riesgos aceptados y seguimiento

R-AUTH/R-DOS/R-FILE/R-AUDIT/R-RUNNER y los59 registros R-IMG están decididos con alcance y trabajo pendiente en sección4; no se usan para silenciar un gate obligatorio. Deben trasladarse a recomendaciones de producción de implementacion.md por su responsable (Thomas) al integrar. La revisión cruzada de PR15/ADR003 debe hacerla Thomas o Ethian; no se afirma aprobación obtenida. No se aprobó/fusionó PR ajeno desde esta fase.

## 7. Análisis dinámico (fase F, preparación local)

Se preparó un baseline ZAP local en contenedores sobre HTTP; no escanea el protocolo TCP7777 ni equivale a una prueba del despliegue real. Los objetivos internos son `http://mc2-server:8080/health` y `http://mc2-front:6080/vnc.html` (sus orígenes reportados son `mc2-server:8080` y `mc2-front:6080`). No intercambiar esos objetivos ni extrapolar sus resultados a otras URLs. El escáner está fijado por digest en `security/zap/scan.py`; no se ejecutó ZAP Actions. El detalle de los informes y las decisiones propuestas está en [evidencias locales de ZAP](evidencias/fase-f/README.md), que enlaza los artefactos preservados fuera de este árbol.

La primera corrida terminó bloqueada: `policy.json` estaba vacía, así que `verdict.json` registra `success:false` y deja sin tratar todos los 5 grupos históricos del servidor y 11 del front; los informes de ZAP tuvieron salida baseline 2. Esto es un bloqueo deliberado del evaluador, no evidencia de que el análisis haya sido aceptado ni de que no existan riesgos. Los hallazgos ZAP son alertas de cabeceras y contenido, no CVE. El escaneo final sigue pendiente; Docker fue restablecido para prepararlo, sin que eso implique que se haya ejecutado otra corrida.

La respuesta HTTP del servidor se implementó con contexto raíz 404 de texto plano; CSP `default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`; `X-Frame-Options: DENY`; `Cross-Origin-Resource-Policy: same-origin`; `Permissions-Policy` restrictiva; `Cache-Control: no-store`. Los cuatro grupos afectados se marcan corregidos en la implementación informada, sujetos a confirmación por el escaneo final. El fragmento requerido de CD y el SLO que falla en el CD actual siguen pendientes de Ethian. La exposición, configuración final y validación cruzada también siguen pendientes; F04 no se declara cerrado.

### 7.1 Registro de alertas del primer baseline y propuesta de tratamiento

Las 16 filas siguientes son el registro histórico del primer baseline, no resultados de una corrida final. Las decisiones para 12 grupos reflejan la propuesta aún pendiente de revisión en `security/zap/policy.json`; riesgo aceptado propuesto no significa aceptación del equipo. Los cuatro grupos restantes del server se registran como corregidos en [b7765f5](https://github.com/MarshallGomez1103/Mc2/commit/b7765f556132baf87b44339647362edf839fb33b), confirmados por el reescaneo local. Ninguna alerta es una CVE. Para el front, el loopback es condición temporal verificable de la propuesta y no convierte CSP ni anti-clickjacking en falsos positivos. Si el binding deja de ser loopback, la propuesta de riesgo aceptado no aplica. No hay ZAP Actions ejecutado. Sí hay una segunda corrida local, con los resultados y límites indicados a continuación.

| Target | AlertRef | Riesgo ZAP | Decisión registrada | Condición / seguimiento |
| --- | --- | --- | --- | --- |
| server | `10038-1` | Medium | Corregido; confirmado localmente en b7765f5 | Raíz responde 404 de texto plano y CSP `default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`; confirmado en el reescaneo local. |
| server | `90004-1` | Low | Corregido; confirmado localmente en b7765f5 | CORP `same-origin`; cabecera confirmada en el reescaneo local. |
| server | `10063-1` | Low | Corregido; confirmado localmente en b7765f5 | Permissions-Policy restrictiva; cabecera confirmada en el reescaneo local. |
| server | `10049-1` | Informational | Riesgo aceptado propuesto (`risk_accepted`) | `Cache-Control: no-store` deja health/métricas y errores no almacenables; propuesta limitada a HTTP server en loopback, evidencia preservada en el reescaneo local. |
| server | `10049-3` | Informational | Corregido; confirmado localmente en b7765f5 | Contexto raíz 404 de texto plano y `Cache-Control: no-store`; confirmado en el reescaneo local. |
| front | `10038-1` | Medium | Riesgo aceptado temporal propuesto (`risk_accepted`) | CSP ausente sigue siendo riesgo real; solo demo local con publicación HTTP loopback. Ethian debe definir CSP compatible con noVNC y probarla antes de ampliar exposición. |
| front | `10020-1` | Medium | Riesgo aceptado temporal propuesto (`risk_accepted`) | Clickjacking sigue siendo riesgo real incluso con binding loopback; limitar a demo local y añadir/probar `frame-ancestors` y/o XFO antes de exponer. |
| front | `90004-2` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | COEP ausente; loopback obligatorio. Ethian debe evaluar y probar compatibilidad de la política con recursos noVNC. |
| front | `90004-3` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | COOP ausente; loopback obligatorio. Ethian debe probar `same-origin` con el flujo noVNC. |
| front | `90004-1` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | CORP ausente en recursos; loopback obligatorio. Ethian debe probar `same-origin` y compatibilidad de carga. |
| front | `10063-1` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | Permissions-Policy ausente; loopback obligatorio. Ethian debe restringir capacidades sin romper controles/portapapeles noVNC. |
| front | `10036-2` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | Versión WebSockify/Python en cabecera Server; loopback obligatorio. Ethian debe generalizar cabecera y actualizar imagen. |
| front | `10021` | Low | Riesgo aceptado temporal propuesto (`risk_accepted`) | Falta `X-Content-Type-Options`; loopback obligatorio. Ethian debe añadir `nosniff` y verificar MIME y funcionamiento. |
| front | `10027` | Informational | Falso positivo propuesto (`false_positive`), alcance limitado | Solo se inspeccionaron los 12 fragmentos citados por el informe: comentarios de interfaz/parámetros sin secretos evidentes. Esto no garantiza que todo el contenido de noVNC esté libre de información sensible. Conservar informe/evidencia y revisar de nuevo si cambian contenido o exposición; loopback es requisito de esta propuesta. |
| front | `10109` | Informational | Observación aceptada propuesta (`risk_accepted`) | ZAP reconoce aplicación web moderna esperada; no demuestra que JavaScript esté libre de fallos. Conservar informe; no extiende decisión a reglas nuevas o riesgos mayores. |
| front | `10049-3` | Informational | Observación aceptada propuesta (`risk_accepted`) | Caché de recursos estáticos noVNC deliberada para demo local loopback; revisar si se añade contenido personalizado, autenticación o cambia la exposición. |

Los identificadores `90004-1/-2/-3` corresponden respectivamente a CORP, COEP y COOP en los JSON preservados. `10027` se evaluó solo sobre las 12 instancias observadas y no se extiende a todos los comentarios o recursos de noVNC. La segunda ejecución local confirmó la corrección del servidor y los bindings HTTP loopback de ambos servicios. La evaluación local pasó aplicando las decisiones explícitas propuestas: no equivale a aceptación del equipo ni a ausencia de hallazgos. La integración en cd.yml, aprobación de la propuesta y ejecución en Actions siguen pendientes de Ethian.


### 7.2 Reescaneo local y condición de bloqueo

Segundo baseline local16/11/2026, finalizado22:55:58 UTC−05:00, código del servidor b7765f5. Manifest liga imagen/digest, contenedores, revisión y hashes exactos del script/policy; `http_loopback_only` fue verdadero para server y front. Reportes en respaldo `evidencias/fase-f/zap-final/`, separados del primer rojo. Server: solo1grupo INFO (no almacenable), ceroLow/Medium/High, salida baseline0. Front:2Medium+6Low+3INFO, salida baseline2. El evaluador devuelve0 con esas12decisiones documentadas; los hallazgos del front siguen presentes y se aceptan solo como propuesta temporal de demostración loopback, no se presentan como corregidos ni aprobados por el equipo.

La política no usa reglas IGNORE ni -I, conserva alertas y salidas originales; falla con reporte ausente/inválido, origen inesperado, error/FAIL del baseline, alerta nueva, cambio de severidad, tratamiento incompleto o pérdida de loopback exigido. CualquierHigh bloquea incluso si se intenta registrar aceptación. Se comprobaron16casos Python, incluidos reportes/políticas malformados y condiciones de exposición. Las3alertas de cabecera del server y el grupoINFO de caché de raíz desaparecieron por corrección; la observación INFO no-store es esperada. CodeQL actual analiza Java; el wrapper Python se revisó y probó separadamente.

Último CD consultado: [44005663312](https://github.com/MarshallGomez1103/Mc2/actions/runs/44005663312), main9d44297, falló en cargaSLO tras desplegar. Ese fallo no es un resultado ZAP ni demuestra resultados de versiones posteriores. El fragmento propone ZAP después de readiness/humo y antes de carga para conservar su evidencia aunque la carga falle después. Su incorporación en cd.yml está reservada a Ethian por README§7; no se alteró ese workflow desde esta fase.
