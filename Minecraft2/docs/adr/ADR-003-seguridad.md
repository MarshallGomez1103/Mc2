# ADR-003 — Estrategia de seguridad del pipeline

| Campo | Valor |
| --- | --- |
| Estado | Implementado y comprobado; revisión cruzada de Thomas/Ethian pendiente. No equivale a aceptación de producción. |
| Fecha | 2026-11-16 |
| Responsable | Jasub (Persona2); Thomas mantiene scripts/ci.yml; Ethian integra imagen/despliegue/runner. |
| Alcance | C3-05: controles bloqueantes, amenazas y tratamiento de hallazgos. |
| Relacionado | [ADR-001](ADR-001-estilo-recuperacion.md), [ADR-002](ADR-002-despliegue.md), [ADR-004](ADR-004-frontera-red.md), [seguridad](../seguridad.md), [devops](../devops.md). |

## 1. Contexto

Java17/Maven, cliente LibGDX, servidor TCP JSON-lines y HTTP operativo, persistencia JSON local. El enunciado exige cuatro gates que fallen al superar umbral, no solo reportes, y un PR inseguro cerrado sin fusionar. Los scripts/workflow ya están implementados por Thomas; E los revisa y registra evidencia, sin sustituirlos ni cambiar sus umbrales.

La autenticación de jugadores no existe: name/skin son presentación. La autorización sí limita cada conexión al ID asignado por el servidor. Se conserva ese alcance de partida académica en red restringida; R-AUTH en seguridad.md explica por qué no es apto para exposición pública y qué hace falta para producción. CI/CD usa identidades y permisos GitHub diferentes de las del juego. ZAP corresponde a F, no se declara ejecutado.

## 2. Alternativas consideradas

| Área | Elegida y razón | Alternativas y costo que se evita |
| --- | --- | --- |
| SAST | CodeQL Java, compilación manual y SARIF como artefacto; integración existente con GitHub. Script propio aplica umbral numérico porque publicar SARIF no garantiza fallo. | Semgrep reduce configuración/tiempo, pero exige validar reglas Java y mantenerlas. SonarCloud añade cuenta/token y servicio externo. No se comparó experimentalmente su tiempo. |
| SCA | Trivy fs sobre Maven, reporte completo y segundo comando bloqueante. Una herramienta también sirve para imagen. | Dependency-Check añade otra base/proceso; no se midió. dependency-review-action solo revisa cambios y no reemplaza escaneo del árbol vigente. Dependabot semanal ayuda mantenimiento, no sustituye SCA. |
| Secretos | Gitleaks git, referencias descargadas completas y redactado de valores. No verifica credenciales contra servicios externos. | TruffleHog con verificación puede enviar valores a servicios externos; no se necesita para demostrar este gate. |
| Imagen | Trivy image del tar exportado de Docker; evita montar docker.sock en un contenedor de escaneo. | Grype añade otra herramienta/base. No se afirma inferioridad de detección; se prioriza reutilización de Trivy y configuración existente. |
| Runner | CI en GitHub-hosted; CD debe reservar runner autohospedado a despliegue confiable main/tags y Environment. | Ejecutar PR en el host aumenta superficie. SSH desde GitHub implica exponer host/llave; efímero añade mantenimiento. La configuración real CD debe validarse con Ethian al integrar #2. |

## 3. Decisión implementada y umbrales

| Control | Herramienta fijada | Gate obligatorio | Evidencia de aplicación |
| --- | --- | --- | --- |
| sast | codeql-action v4.39.3, CLI2.27.4 según configuración | Ningún resultado con security-severity>=7.0 sin tratamiento documentado; script umbral-sast.sh lee SARIF. | Compilación real, guardia de archivos Java extraídos, SARIF con runs obligatorio. Cinco fixtures locales del script real comprueban 6.9/7.0/9.5/sin-runs/vacío. |
| sca | Trivy0.76.2 fs | HIGH/CRITICAL con corrección bloquean: --ignore-unfixed --severity HIGH,CRITICAL --exit-code1. | Reporte completo; guardia rechaza escaneo sin resultado pom; scanner/descarga fallidos también fallan el job. |
| secretos | Gitleaks8.30.1 git | Cero hallazgos en historial de todas las referencias descargadas: --log-opts=--all --exit-code1 --redact. | checkout fetch-depth0; rechaza clon superficial/sin commits; PR5 realmente bloqueado y cerrado sin merge. |
| imagen | Trivy0.76.2 image | CRITICAL con corrección bloquean: --ignore-unfixed --severity CRITICAL --exit-code1. | Construcción Docker y export; reporte de todas severidades antes del gate. HIGH de SO no supera este umbral, se registra igualmente. |

Los umbrales son los mínimos del enunciado; no se relajan ni se crean excepciones en E. «Con corrección» aplica a SCA/imagen, no a secretos. Los reportes completos conservan también hallazgos que no bloquean. SAST requiere revisión de excepciones por par regla-archivo y mención documental; hoy no hay excepciones. Este mecanismo es amplio y puede ocultar futuros resultados del mismo par: no es una aprobación automática de seguridad.

No se introduce fase informativa ni se apagan controles: ya son bloqueantes. Sin reporte/descarga o con error de herramienta, no se presume éxito. Un HTTP429 de Maven Central interrumpió SCA en D antes del escaneo; se conservó y reintentó solo ese job, sin modificar umbrales.

## 4. Versiones, permisos y cadena de entrega

Acciones por SHA completo: checkout v7.0.1, setup-java v6.2.0, upload-artifact v7.0.2 y CodeQL v4.39.3. Trivy/Gitleaks se descargan en directorio temporal y se comprueba SHA256; el checksum de SCA/Gitleaks viene de la misma release, por lo que no equivale a verificación independiente de firma. La imagen fija tags de Maven/Temurin, todavía sin digest. Dependabot ya configura revisión semanal de Maven, acciones y Docker; las actualizaciones requieren revisión, no se fusionan automáticamente.

ci.yml: contents:read, sin pull_request_target, GitHub-hosted, checkout persist-credentials:false. No se usa token/secret de despliegue para escanear PR. Cada job tiene timeout10/15min y concurrencia que cancela la ejecución vieja del mismo PR. Se conservan artefactos14d; secretos redactados7d, publicados solo si falla, con metadatos de commit todavía visibles. El runner ubuntu-latest no fija SO exacto; las bases de vulnerabilidades también cambian.

El equipo del runner, MC2_DEPLOY_RUNNER, permisos del Environment y exposición de puertos finales no se certifican en E. Ethian debe confirmarlos y Thomas revisarlos al integrar; no es una mitigación demostrada por el CI hospedado.

## 5. Consecuencias, costos y límites

- Los cuatro gates producen fallo por umbral y no se limitan a adjuntar un reporte. El PR5 demuestra un bloqueo real de seguridad; no hace falta contaminar otra rama con una credencial falsa para repetirlo.
- CodeQL/Trivy no reemplazan las pruebas del protocolo. E reproduce una inyección de log, la corrige y comprueba rechazo de identidad falsa/líneas enormes, límite8 pares, continuidad de un dueño y métricas sin nombres.
- Un timeout SO_TIMEOUT es por lectura inactiva, no plazo absoluto: el goteo lento ocupa plazas. No hay cuotas por origen/rate limit de logs ni autenticación/TLS. Se registran con trabajo pendiente; no se promete resistencia completa a DoS.
- Persistencia confía en el operador del volumen: nombres filtrados y server fijo no limitan bytes ni symlinks. La autorización de red no evita sabotaje local con escritura.
- Cinco checks verdes no significan cero CVE: imagen de D presenta59 resultados (2HIGH/53MEDIUM/4LOW), todos registrados. CeroCRITICAL con corrección cumple el mínimo; actualizar imagen sigue siendo trabajo de Ethian, no excepción del scanner.
- La prueba SARIF es sintética y local, no una vulnerabilidad real generada por CodeQL. No se demostraron negativos reales de SCA/imagen. La obligación de al menos un PR inseguro bloqueado está satisfecha por #5; no atribuir fallos de cobertura a vulnerabilidades de imagen.

Costo observado en [Actions43978487608](https://github.com/MarshallGomez1103/Mc2/actions/runs/43978487608), última ejecución disponible por job: sast100s, sca33s (reintento), secretos7s, imagen94s y build-pruebas44s. Corren en paralelo: no sumar como tiempo total del pipeline; esos tiempos no son una promesa con caché fría. El presupuesto/timeout y las descargas deben revisarse si producen fallos repetidos.

Revisar decisión cuando haya exposición externa, cuentas/roles, nuevos endpoints/dependencias, otros lenguajes, errores de herramientas frecuentes o un cambio del riesgo de base/runner. Las aceptaciones técnicas del prototipo requieren revisión cruzada y no permiten saltarse un gate obligatorio.

## 6. Evidencia y fuentes

- [Modelo, controles, registro individual y PR negativo](../seguridad.md), [evidencia E](../evidencias/fase-e/README.md), ci.yml y scripts/*.sh actuales.
- [PR5 cerrado sin merge](https://github.com/MarshallGomez1103/Mc2/pull/5), [Actions43736948906](https://github.com/MarshallGomez1103/Mc2/actions/runs/43736948906): secretos falla por una credencial inventada, demás checks pasan. Se verifican metadatos, log y reporte redactado; no se copia el valor.
- [GitHub: SARIF y security-severity](https://docs.github.com/en/code-security/reference/code-scanning/sarif-files/sarif-support), [Trivy: filtrado de resultados](https://trivy.dev/latest/docs/configuration/filtering/), [Gitleaks: git, log-opts y redact](https://github.com/gitleaks/gitleaks). Las versiones efectivamente usadas se comprueban en scripts/logs, no se infieren de documentación latest.
