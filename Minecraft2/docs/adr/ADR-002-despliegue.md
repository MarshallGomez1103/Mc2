# ADR-002 — Estrategia de despliegue del Corte 3

| Campo | Valor |
| --- | --- |
| Estado | **Propuesto; acordado de palabra con el profesor, confirmación escrita pendiente.** El alcance de la sección 5 debe confirmarse por escrito antes de depender de él. |
| Fecha | Previsto — responsable: Persona 1: fecha de aprobación |
| Responsable | Persona 3, con Persona 1 |
| Corrección | C3-00 del [plan del Corte 3](../../README.md#corte-3) |
| Decisión anterior que revisa | [ADR-001](ADR-001-estilo-recuperacion.md), sección 6 «Cuándo revisar esta decisión» |

> Este ADR es un esqueleto con la decisión propuesta. Las afirmaciones sobre rendimiento, costo y esfuerzo que no tienen una medición se marcan como tales. Los elementos «Previsto — responsable: Persona N» se completan con la evidencia real y el marcador se retira antes de la entrega.

## 1. Contexto

Minecraft2 es un juego de escritorio Java 17 que se ejecuta en un solo proceso, con LibGDX/LWJGL3 y mundos JSON locales ([ADR-001](ADR-001-estilo-recuperacion.md#1-contexto)). No tiene API, servidor ni base de datos. El enunciado del Corte 3 establece: «Si el sistema es una aplicación de escritorio o un juego, se contenerizan y se despliegan los componentes de servidor (API, servidor multijugador, base de datos, broker). […] La prueba de humo y la prueba de carga se dirigen al componente de servidor. Si el sistema no tiene ningún componente de servidor, el grupo acuerda con el profesor qué se despliega y cómo se verifica el SLO antes de empezar». También pide, para el despliegue, un registro de imágenes, un entorno accesible, una prueba de humo y una reversión probada, y no pide funcionalidades de negocio nuevas.

Hay tres exigencias que dirigen la decisión:

1. **Qué se despliega.** Debe existir un componente que pueda ejecutarse en un contenedor, responder a una comprobación de salud y recibir la prueba de humo y la de carga.
2. **Qué SLO se verifica.** El [SLO-CARGA-01](../recuperacion-c2/carga.md#2-slo-fijado-antes-de-ejecutar) del Corte 2 se midió con un harness dentro de la JVM, sin red. Debe conservar su significado al verificarse contra el entorno desplegado.
3. **Dónde se despliega.** El entorno debe ser accesible para quien califica y no depender de pasos manuales.

### Alcance propuesto por el equipo

El equipo propone el siguiente alcance, para confirmación del profesor:

- el sistema se separa en dos contenedores: `mc2-server` (el servidor) y `mc2-front` (el cliente gráfico);
- el SLO que se verifica es la comunicación del front hacia el servidor;
- la ejecución es local (no en la nube) y el multijugador se da en la misma red, compartiendo el contenedor del front; cada jugador elige entre tres apariencias (`skinId` 0–2).

## 2. Criterios de decisión

| # | Criterio | Pregunta concreta |
| --- | --- | --- |
| D1 | Servidor desplegable | ¿Existe un componente de servidor al que dirigir el humo y la carga, como pide el enunciado? |
| D2 | Significado del SLO | ¿Se conserva el SLO del Corte 2 y se añade la medición front → servidor? |
| D3 | Cambio de dominio mínimo | ¿Cuánto del dominio y de la aplicación cambia, dado que «no se piden funcionalidades de negocio nuevas»? |
| D4 | Juego verificable por un revisor | ¿Puede quien califica jugar y ver el sistema funcionando sin instalar nada más que Docker? |
| D5 | Objetivo HTTP | ¿Hay un endpoint HTTP real para el humo y, si se hace, para el análisis dinámico (DAST), sin crear uno solo para escanearlo? |
| D6 | Proporción | ¿Cabe en el plazo del corte para tres personas, sin reescribir paquetes estables? |
| D7 | Riesgo operativo | ¿Qué infraestructura propia o de terceros hay que mantener y asegurar? |

## 3. Opciones consideradas

### Opción A — Servidor headless más front gráfico en el navegador (noVNC)

`mc2-server` es el JAR en modo headless con una sesión autoritativa de ticks fijos, un protocolo TCP de líneas JSON (puerto 7777) y HTTP `/health` y `/metrics` (puerto 8080). `mc2-front` es el cliente LibGDX dentro de un contenedor con Xvfb y noVNC, jugable desde el navegador (puerto 6080); además, el JAR cliente se publica en GitHub Releases. Varios jugadores comparten el mundo del servidor.

### Opción B — Servidor headless y solo un cliente de consola o headless

El servidor es el mismo que en la opción A. Del lado cliente solo hay un cliente de consola o uno headless que se usa para el humo y la carga. No hay un front gráfico desplegado.

### Opción C — Front web nuevo

El servidor expone un protocolo web y el front es una aplicación nueva para el navegador que reemplaza al cliente LibGDX.

### Opción D — Mantener todo local, sin contenedor de servidor

Se contenedoriza solo la aplicación actual (la consola del Corte 2 que ya empaqueta el [`Dockerfile`](../../Dockerfile)), sin red ni multijugador. El SLO sigue midiéndose dentro de la JVM, como en el Corte 2.

### Dónde se despliega

| Opción de entorno | Descripción |
| --- | --- |
| E1 — Docker Compose en un runner autohospedado del equipo | El job de despliegue de `main` corre en un equipo del grupo, que levanta los servicios con Compose. |
| E2 — Servicio en la nube | Un proveedor (PaaS o máquina virtual) ejecuta los contenedores. |
| E3 — Clúster de Kubernetes | Despliegue con manifiestos en kind o en la nube. Es una bonificación, no la ruta principal. |

## 4. Comparación

| Criterio | A: servidor + front noVNC | B: servidor + cliente de consola | C: front web nuevo | D: todo local |
| --- | --- | --- | --- | --- |
| D1 Servidor desplegable | Cumple. | Cumple. | Cumple. | No: no hay componente de servidor. Exigiría el acuerdo explícito del profesor. |
| D2 SLO | Conserva SLO-CARGA-01 y añade la medición front → servidor (SLO-RED-01, propuesta). El primero se mide hoy dentro de la JVM; medirlo contra el servidor es una adaptación del protocolo por documentar y confirmar. | Igual que A. | Igual que A, con un cliente que habría que escribir de nuevo. | Se conserva tal cual, pero solo dentro de la JVM: no es «contra el entorno desplegado». |
| D3 Cambio de dominio | Alto: `World` pasa de un `Player` a varios y los servicios reciben el jugador explícito; paquete nuevo de red. Mantener mínimo: sin cuentas ni chat. | Igual que A: el multijugador y la red son los mismos. | Igual que A, más una reescritura del cliente. | Ninguno. |
| D4 Verificable por un revisor | Sí: el revisor juega desde el navegador con `docker compose up`. Rendimiento del OpenGL por software: sin medir. | Parcial: el revisor ve el servidor y el humo, pero no juega. | Sí, pero con un cliente nuevo que hay que construir y probar. | Parcial: solo consola. |
| D5 Objetivo HTTP | `/health`, `/metrics` y la interfaz noVNC son objetivos HTTP reales. | `/health` y `/metrics`. Sin interfaz que escanear. | Sí. | Ninguno. |
| D6 Proporción | Media: la mayor parte del trabajo es el servidor y su protocolo; el front es el cliente existente más una imagen con Xvfb y noVNC. El riesgo del front está en el OpenGL por software (se prueba antes de depender de él). | Menor que A en el front; igual en el servidor. | Alta: cliente nuevo completo. | Mínima. |
| D7 Riesgo operativo | Dos imágenes y un runner. | Una imagen de servidor y un runner. | Igual que A y más superficie web. | Una imagen. |

**Resumen.** D y B se descartan por razones distintas: D no cumple el enunciado sin un acuerdo adicional y B no deja verificar el juego de forma interactiva. C se descarta por el esfuerzo de un cliente nuevo en el plazo. A y B comparten el servidor, el multijugador y su riesgo; se separan en D4 y en el costo del front gráfico. B es el respaldo de A: si el OpenGL por software en el contenedor no funciona o es demasiado lento, el humo y la carga ya no dependen del front gráfico.

| Criterio de entorno | E1: runner autohospedado y Compose | E2: nube | E3: Kubernetes |
| --- | --- | --- | --- |
| Costo monetario | Sin cuota de proveedor; usa un equipo del grupo. Costo en disponibilidad del equipo. | Depende del plan del proveedor. No evaluado. | Depende de dónde corra. |
| Ajuste al alcance | Cumple «ejecución local». | No cumple «ejecución local» del alcance propuesto; además requiere que el proveedor admita el puerto TCP 7777. No verificado con un proveedor concreto. | Bonificación; no sustituye el despliegue principal. |
| Seguridad | Un runner autohospedado ejecuta código del repositorio en un equipo del grupo: riesgo A8 de [seguridad.md](../seguridad.md#2-modelo-de-amenazas-stride). | Las credenciales del proveedor se guardan como secretos. | NetworkPolicy o RBAC justificado (bonificación). |
| Disponibilidad | La del equipo del grupo; el entorno debe estar activo al calificar o se acepta la ejecución del despliegue en Actions y la demostración (rúbrica, condiciones mínimas de entrega). | La del proveedor. | La del clúster. |

## 5. Decisión

Se propone la **opción A** (servidor headless más front gráfico con noVNC) con el entorno **E1** (Docker Compose en un runner autohospedado). La opción B queda como respaldo del front para el humo y la carga si el front gráfico no resulta viable.

Contrato de despliegue que fija la decisión (los nombres no cambian sin avisar al equipo):

| Elemento | Valor |
| --- | --- |
| Servicios de Compose | `mc2-server` y `mc2-front` |
| Puertos | 7777 (TCP, protocolo v1), 8080 (HTTP: `/health` y `/metrics`) y 6080 (noVNC) |
| SLO | SLO-CARGA-01 conservado y SLO-RED-01 (propuesta: p95 ≤ 50 ms, 0 errores) |
| Entornos | Pull request: Compose efímero en un runner hospedado, con humo y carga corta. `main`: publicación en GHCR con SHA y versión, despliegue con Compose en el runner autohospedado, humo y carga, y reversión por la etiqueta anterior. |

Todo el alcance depende de la confirmación del profesor. Si el profesor decide otra modalidad, se revisa este ADR.

## 6. Consecuencias

### Ganancias

- Existe un componente de servidor al que dirigir el humo y la carga, como pide el enunciado, y un healthcheck real para Compose y el despliegue.
- El SLO del Corte 2 se conserva y se añade una medición de la comunicación front → servidor.
- `/health` y `/metrics` ofrecen objetivos HTTP reales para el humo, la observabilidad (bonificación) y, si procede, el análisis dinámico.
- El cliente se publica como artefacto versionado en Releases y puede conectarse al servidor desplegado.

### Sacrificios y riesgos aceptados

- **Cambio de dominio.** `World` admite varios `Player` y los servicios de aplicación reciben el jugador de forma explícita (hoy `GameSession`, `EnemyUpdateService`, `PistolService` y `PlayerControlService` consultan el único jugador del mundo). Esto convive con el enunciado («no se piden funcionalidades de negocio nuevas»): se mantiene mínimo, sin cuentas ni chat, y se documenta aquí como un cambio de arquitectura permitido por el enunciado.
- **Paquete nuevo de red.** Aparece una frontera nueva (`network`) que las reglas de arquitectura deben proteger. Previsto — responsable: Persona 2.
- **Revisión de ADR-001.** El [ADR-001](ADR-001-estilo-recuperacion.md#cuándo-revisar-esta-decisión) previó reevaluar la opción de puertos y adaptadores si aparecía entrada de red o multijugador. Si `WorldStorage` se traslada a `application` como puerto de salida es una decisión abierta que este ADR no resuelve. Previsto — responsable: Persona 1.
- **Costo del OpenGL por software.** El front dibuja con renderizado por software en un contenedor sin GPU. El rendimiento no se ha medido; se comprueba en una prueba de viabilidad antes de depender de él. Previsto — responsable: Persona 3.
- **Runner autohospedado.** Agrega un componente que debe asegurarse (amenaza A8) y que depende de la disponibilidad de un equipo del grupo.
- **SLO doble.** Se mantienen dos mediciones. SLO-RED-01 es una propuesta no medida. La medición de SLO-CARGA-01 contra el servidor cambia de lugar respecto al Corte 2 y debe documentarse. Previsto — responsable: Persona 3.
- **Sin autenticación de jugador.** El protocolo y noVNC no autentican a quien se conecta; el alcance excluye cuentas ([seguridad.md](../seguridad.md#21-autenticación-y-autorización)).
- **Multijugador solo en la misma red.** No se prueba ni se afirma nada fuera de ella.

### Cuándo revisar esta decisión

Se reevaluará si el profesor confirma otra modalidad, si el front gráfico con noVNC no resulta viable (se adopta la opción B para el humo y la carga) o si aparece una necesidad real de desplegar en la nube.

## 7. Evidencia y enlaces

- Contexto previo: [ADR-001](ADR-001-estilo-recuperacion.md), [arquitectura](../arquitectura.md) y [carga del Corte 2](../recuperacion-c2/carga.md).
- Vista de despliegue: [arquitectura §8](../arquitectura.md#8-vista-de-despliegue).
- Pipeline y reversión: [devops.md](../devops.md). Amenazas del despliegue: [seguridad.md](../seguridad.md).
- Evidencia de la viabilidad del front (OpenGL por software en el contenedor): Previsto — responsable: Persona 3.
- Confirmación del profesor: Previsto — responsable: Persona 1.
