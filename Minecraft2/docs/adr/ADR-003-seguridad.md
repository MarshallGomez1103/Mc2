# ADR-003 — Estrategia de seguridad del pipeline

| Campo | Valor |
| --- | --- |
| Estado | **Propuesto.** Las herramientas se confirman al probarlas sobre el repositorio real, antes de convertirlas en bloqueantes. |
| Fecha | Previsto — responsable: Persona 2: fecha de aprobación |
| Responsable | Persona 2, con Persona 1 (cableado en los workflows) y Persona 3 (imagen y runner) |
| Corrección | C3-05 del [plan del Corte 3](../../README.md#corte-3) |
| Relacionado | [ADR-002](ADR-002-despliegue.md), [seguridad.md](../seguridad.md), [devops.md](../devops.md) |

> Este ADR es un esqueleto con la decisión propuesta. No contiene resultados de ejecuciones: el costo en tiempo de cada herramienta y los hallazgos se agregan cuando existan. Los elementos «Previsto — responsable: Persona N» se completan y el marcador se retira antes de la entrega.

## 1. Contexto

El enunciado del Corte 3 exige cuatro controles de seguridad que detengan el pipeline cuando superan un umbral: análisis estático (SAST), análisis de dependencias (SCA), detección de secretos y escaneo de la imagen. Cada grupo puede endurecer los umbrales, no relajarlos, y debe incorporar un paso que aplique el umbral cuando una herramienta solo reporta. Los hallazgos se registran con una decisión y se demuestra, con un pull request bloqueado, que los controles funcionan.

El sistema tiene estas características que condicionan la elección:

- Código propio en Java 17 con Maven y sin servidor web de terceros ([ADR-001](ADR-001-estilo-recuperacion.md#1-contexto)); con el Corte 3 se añaden un servidor TCP/HTTP, una imagen de contenedor y un runner autohospedado ([ADR-002](ADR-002-despliegue.md)).
- El repositorio tiene historial del Corte 1 y del Corte 2 que debe revisarse completo: la rúbrica califica como Insuficiente un secreto real en el repositorio o su historial.
- El pipeline de integración debe durar menos de 10 minutos, de modo que el costo en tiempo de cada control importa.
- Las amenazas candidatas están en [seguridad.md](../seguridad.md#2-modelo-de-amenazas-stride).

## 2. Criterios de decisión

| # | Criterio | Pregunta concreta |
| --- | --- | --- |
| S1 | Bloqueo | ¿Puede fallar el job, o se puede añadir un paso que lea el resultado y lo haga fallar? |
| S2 | Umbral | ¿Permite expresar el umbral del enunciado sin relajarlo? |
| S3 | Ajuste al sistema | ¿Analiza Java 17 con Maven, el historial de Git y una imagen Docker? |
| S4 | Costo en tiempo | ¿Cabe en el presupuesto de 10 minutos con caché? Se mide con ejecuciones reales. |
| S5 | Costo de operación | ¿Requiere cuentas, tokens o servicios externos adicionales? |
| S6 | Mantenimiento | ¿Cuántos falsos positivos y actualizaciones de versión exige? |

## 3. Opciones consideradas

### 3.1 SAST

| Opción | Costos y limitaciones |
| --- | --- |
| **CodeQL** (propuesta) | Se integra con GitHub y publica en la pestaña Security. Solo reporta: exige un paso que lea el SARIF y falle por umbral. Puede requerir compilar el proyecto y suma tiempo al pipeline; el costo no está medido. |
| Semgrep | Reglas más ligeras y fáciles de ajustar. La cobertura de reglas de Java debe comprobarse y cada regla propia es mantenimiento del equipo. Alternativa si CodeQL no cabe en el tiempo. |
| SonarCloud | Panel completo de calidad, pero requiere una cuenta y un token externos (S5) y su gate depende de un servicio de terceros. |

### 3.2 SCA

| Opción | Costos y limitaciones |
| --- | --- |
| **Trivy en modo sistema de archivos** (propuesta) | Una sola herramienta sirve para dependencias, imagen y archivos de configuración; usa código de salida por umbral. Su base de datos de vulnerabilidades se descarga en cada ejecución salvo que se almacene en caché. |
| OWASP Dependency-Check | Específica de dependencias. Su base de datos es pesada y puede alargar el pipeline; no está medido. |
| `dependency-review-action` | Revisa solo las dependencias que cambia un pull request, no el árbol completo. No cubre un cambio que ya llegó a `main`. |

Dependabot se usa para mantener las dependencias y las acciones actualizadas, pero no sustituye el control SCA.

### 3.3 Detección de secretos

| Opción | Costos y limitaciones |
| --- | --- |
| **Gitleaks con el historial completo** (propuesta) | Código de salida por hallazgo y soporte de historial (checkout sin recorte). Una coincidencia en un commit antiguo no se corrige borrando el archivo: si es un secreto real debe rotarse. Puede generar falsos positivos que se registran. |
| TruffleHog | Verifica algunos secretos contra el servicio de origen, lo que implica enviar el valor al exterior (S5); sin esa verificación, el volumen de coincidencias por revisar aumenta. |

### 3.4 Escaneo de la imagen

| Opción | Costos y limitaciones |
| --- | --- |
| **Trivy sobre la imagen** (propuesta) | Misma herramienta que el SCA, y por tanto una sola versión que mantener. Las vulnerabilidades de la imagen base sin corrección no deben bloquear (umbral: solo las críticas con corrección disponible). |
| Grype | Alternativa equivalente. Añadiría una segunda herramienta y una segunda base de datos de vulnerabilidades. |

### 3.5 Manejo del runner autohospedado

| Opción | Costos y limitaciones |
| --- | --- |
| **Runner autohospedado solo para el job de despliegue de `main` o de tags, con un Environment** (propuesta) | Cumple «ejecución local» ([ADR-002](ADR-002-despliegue.md)). El equipo del runner ejecuta código del repositorio: queda como riesgo residual (A8). Los pull requests no corren en él. |
| Despliegue por SSH desde un runner hospedado | El runner hospedado queda aislado, pero hay que exponer un equipo del grupo a Internet y guardar una llave SSH como secreto. Es más superficie de ataque. |
| Runner efímero en un contenedor | Reduce la persistencia en el equipo anfitrión. Añade configuración y mantenimiento que el plazo del corte no permite asumir de entrada. |

## 4. Decisión

Se propone:

| Control | Umbral mínimo (enunciado) | Umbral adoptado | Herramienta propuesta |
| --- | --- | --- | --- |
| SAST | Ningún hallazgo alto o crítico sin tratar. | Igual al mínimo. | CodeQL (alternativa: Semgrep). Previsto — responsable: Persona 2 |
| SCA | Ninguna vulnerabilidad alta o crítica con corrección disponible. | Igual al mínimo. | Trivy en modo sistema de archivos. Previsto — responsable: Persona 2 |
| Secretos | Cero secretos, incluido el historial. | Igual al mínimo. | Gitleaks con historial completo. Previsto — responsable: Persona 2 |
| Imagen | Ninguna vulnerabilidad crítica con corrección disponible. | Igual al mínimo. | Trivy sobre la imagen. Previsto — responsable: Persona 2 |

Reglas de la decisión:

- **Umbrales.** Son los mínimos del enunciado y no se relajan. Endurecerlos (por ejemplo, incluir las altas de la imagen o no limitar el SAST a hallazgos sin tratar) se evalúa con los resultados reales de las primeras ejecuciones y, si se hace, se registra en este ADR. Previsto — responsable: Persona 2.
- **Paso de aplicación del umbral.** Cuando una herramienta solo reporta (CodeQL con SARIF), un paso del workflow lee el resultado y falla si hay un hallazgo por encima del umbral. Previsto — responsable: Persona 1.
- **Excepciones.** Una regla desactivada o un hallazgo descartado se registra con su justificación en el [registro de hallazgos](../seguridad.md#4-registro-de-hallazgos). No hay excepciones sin registro.
- **Versiones.** Las herramientas y las acciones del pipeline se fijan a una versión o a un SHA (no a una etiqueta móvil) y se actualizan con Dependabot. Previsto — responsable: Persona 1.
- **Prueba previa al bloqueo.** El escaneo de secretos sobre el historial completo y el de la imagen base se ejecutan primero en modo informativo para conocer el estado inicial y tratar lo que aparezca. Después se activan como bloqueantes. Previsto — responsable: Persona 2.
- **Runner.** Opción de la sección 3.5 propuesta, con los controles descritos en [seguridad.md](../seguridad.md) (A8).

## 5. Consecuencias

### Ganancias

- Los cuatro controles detienen el pipeline con los umbrales del enunciado y cada excepción deja un registro.
- Trivy cubre SCA e imagen con una sola herramienta y una sola versión que mantener.
- Cada cambio pasa por los mismos controles, sin importar quién lo escribió.

### Sacrificios y riesgos aceptados

- **Tiempo.** Cada control suma tiempo al pipeline y la meta es menos de 10 minutos con caché. El costo real de cada herramienta no está medido; se mide y se registra. Si el presupuesto no alcanza, se evalúa Semgrep como alternativa del SAST o ejecutar un control en paralelo. Previsto — responsable: Persona 1.
- **Falsos positivos.** Los controles pueden bloquear un cambio legítimo; cada caso exige un registro y una decisión explícita.
- **Mantenimiento.** Versiones fijas y Dependabot implican revisar actualizaciones de las herramientas y de las acciones.
- **Gitleaks sobre el historial.** Puede señalar elementos de commits antiguos. Un secreto real debe rotarse; borrar el archivo no es suficiente. Reescribir el historial no se plantea sin una decisión explícita del equipo.
- **Runner autohospedado.** Queda un riesgo residual: un equipo del grupo ejecuta código del repositorio. Se mitiga, no se elimina.
- **Alcance.** Los cuatro controles no cubren el protocolo TCP propio (7777): su validación se verifica con pruebas del servidor, no con estas herramientas.
- **Sin análisis dinámico en el mínimo.** DAST con OWASP ZAP es una bonificación; si se hace, se rige por el mismo tratamiento de hallazgos.

### Cuándo revisar esta decisión

Se reevaluará si el tiempo del pipeline supera el presupuesto, si una herramienta deja de mantenerse o si aparece una fuente de falsos positivos que haga inviable el bloqueo.

## 6. Evidencia y enlaces

- [Modelo de amenazas, controles y registro de hallazgos](../seguridad.md) y [pipeline](../devops.md).
- Configuración de los workflows (`.github/workflows/ci.yml`) y reportes publicados como artefactos: Previsto — responsable: Persona 1 y Persona 2.
- Pull request bloqueado: Previsto — responsable: Persona 2.
