# Evidencia local de preparación de fase F

Esta nota documenta la primera corrida baseline de ZAP y propuestas para su tratamiento. Es evidencia local/preparatoria: no es una ejecución de ZAP Actions, una revisión externa, una aceptación del equipo ni cierre de F04. El último CD consultado (44005663312, main9d44297) falló en SLO y el fragmento que Ethian debe incorporar sigue pendiente.

## Corrida y alcance

El script `security/zap/scan.py` usó la imagen ZAP fijada por digest y dos destinos distintos de Compose:

| Etiqueta | URL objetivo | Origen del informe | Conteo de grupos |
| --- | --- | --- | ---: |
| server | `http://mc2-server:8080/health` | `http://mc2-server:8080` | 5 |
| front | `http://mc2-front:6080/vnc.html` | `http://mc2-front:6080` | 11 |

Los informes incluyen otras rutas del mismo origen descubiertas durante el baseline. No son alias entre sí. El análisis es HTTP pasivo baseline sobre los servicios locales; no cubre TCP 7777, un proxy/TLS de producción, ni otra URL o entorno. No se debe asociar ninguna de estas alertas con un CVE.

Los artefactos completos preservados como respaldo local están en `evidencias/fase-f/zap/`, fuera del repositorio `fase-f-trabajo/Minecraft2`: JSON, XML, HTML, Markdown, logs, manifest y verdict. Esa ruta de respaldo no es un enlace navegable del repositorio. La revisión del manifest identifica proyecto `mc2-fase-f`, revisión `839aa764ec4d7f2adf6d36c8664b26eb43257678` e imagen ZAP `ghcr.io/zaproxy/zaproxy@sha256:9624669db72f54d86c4b46a7064518bd9bf79d8c61a4c31befa9923489f328af`. `front_loopback_only` figura verdadero; el manifest aportado no incluye un campo equivalente de loopback para server, así que no se atribuye aquí esa propiedad al server.

## Resultado inicial

La corrida terminó con `verdict.success=false`; ambos baseline devolvieron código 2. En esa corrida, `policy.json` estaba vacía y el evaluador bloqueó los 5 grupos del server y 11 del front. Este resultado conserva los hallazgos originales y confirma el bloqueo por falta de tratamiento. Luego se preparó una política poblada como propuesta para revisión; no se afirma que se haya ejecutado otra corrida ni que ZAP Actions exista o haya pasado.

## Server: 5 grupos del primer informe

| AlertRef | Hallazgo / riesgo ZAP | Lectura y tratamiento propuesto |
| --- | --- | --- |
| `10038-1` | CSP ausente — Medium | En `/`, `/robots.txt` y `/sitemap.xml`; ZAP encontró respuestas del contexto raíz. Se corrige con 404 de texto plano y CSP restrictiva; corregido en b7765f5 y confirmado en el reescaneo local. |
| `90004-1` | CORP ausente/inválida — Low | Instancia en `/health`. Configurar `Cross-Origin-Resource-Policy: same-origin`. |
| `10063-1` | Permissions Policy ausente — Low | En rutas raíz. Añadir política restrictiva explícita. |
| `10049-1` | Non-Storable Content — Informational | `/health` ya clasificado como no almacenable; observación informativa aceptable para esta preparación. |
| `10049-3` | Storable and Cacheable Content — Informational | Raíz y rutas auxiliares; revisar conjuntamente la respuesta 404 y aplicar `Cache-Control: no-store`. Observación INFO, sin CVE. |

La corrección de servidor que lidera el equipo es: raíz con 404 plain text; `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`; `X-Frame-Options: DENY`; CORP `same-origin`; Permissions-Policy restrictiva y `Cache-Control: no-store`. El reescaneo local posterior confirmó estas cabeceras y la retirada de los cuatro grupos correspondientes.

## Front: 11 grupos del primer informe

El front es noVNC servido en `mc2-front:6080`. Las alertas CSP y anti-clickjacking representan riesgos reales para la interfaz, aunque el binding observado esté limitado a loopback en esta preparación. Su alcance temporal local no las convierte en falsos positivos ni justifica ignorarlas. Deben resolverse en la configuración del servidor frontal y verificarse con el front funcional.

| AlertRef | Hallazgo / riesgo ZAP | Lectura y propuesta pendiente |
| --- | --- | --- |
| `10038-1` | CSP ausente — Medium | Riesgo real para el front. Definir CSP compatible con noVNC, probar el flujo y corregir antes de integración final. No reutilizar a ciegas la CSP `default-src 'none'` del endpoint Java. |
| `10020-1` | Anti-clickjacking ausente — Medium | Riesgo real de que `vnc.html` se enmarque. Establecer protección compatible (CSP `frame-ancestors` y/o XFO) y verificar la interfaz. |
| `90004-2` | COEP ausente/inválida — Low | Trabajo temporal con Ethian; decidir y probar la política compatible con recursos noVNC. |
| `90004-3` | COOP ausente/inválida — Low | Trabajo temporal con Ethian; decidir y probar aislamiento compatible con el flujo. |
| `90004-1` | CORP ausente/inválida — Low | Trabajo temporal con Ethian; revisar carga de imágenes y recursos antes de elegir `same-origin` u otra política. |
| `10063-1` | Permissions Policy ausente — Low | Trabajo temporal con Ethian; configurar capacidades del navegador según las necesidades de noVNC. |
| `10036-2` | Versión expuesta en cabecera `Server` — Low | Trabajo temporal con Ethian; quitar o generalizar identificación/versionado del servidor. |
| `10021` | `X-Content-Type-Options` ausente — Low | Trabajo temporal con Ethian; establecer `nosniff` y confirmar MIME correctos para todos los recursos. |
| `10027` | Comentarios sospechosos — Informational | Puede resultar falso positivo según inspección del contenido exacto; requiere comprobar los 12 casos (raíz, `app/error-handler.js`, `app/ui.js`, `vnc.html`) en HTML/JS antes de decidir. No se ha clasificado aún como falso positivo. |
| `10109` | Modern Web Application — Informational | Observación descriptiva de ZAP, aceptable como INFO; sin acción de seguridad identificada por sí sola. |
| `10049-3` | Storable and Cacheable Content — Informational | Observación INFO de recursos/front; documentar la política de caché deseada con Ethian y conservar solo si es deliberada. |

`90004` es el plugin que identifica varias familias; sus referencias `90004-1`, `90004-2` y `90004-3` corresponden a CORP, COEP y COOP respectivamente, según los reportes JSON originales. No combinar esas filas en una sola decisión ni reutilizar referencias entre server y front: la política del evaluador usa claves separadas por destino y `alertRef`.

## Siguiente evidencia necesaria

Ethian debe integrar el fragmento de CD, resolver el SLO que actualmente falla y coordinar con el líder los controles de cabecera del front. Después se requiere nueva corrida local con manifest/verdict y reportes completos, seguida de integración y revisión. La política poblada en `security/zap/policy.json` es una propuesta sin aprobación; la corrida final debe verificar las condiciones, sin convertir esta nota en aceptación del equipo ni crear excepciones.


## Reescaneo local después de corrección y tratamiento propuesto

Servercorregido en [b7765f5](https://github.com/MarshallGomez1103/Mc2/commit/b7765f556132baf87b44339647362edf839fb33b). MavenJava17 clean verify finalizó16/11/2026 22:53:30 UTC−05:00; la suite completa aprobó, con reportes13 fuera de target. Prueba nueva GameServerIT comprueba estado/cabeceras en health,metrics,raíz,robots y sitemap. La ejecución anterior11 quedó interrumpida al pausar y se conserva como incompleta; no se cuenta como aprobación.

El segundo baseline local finalizó22:55:58, con los dos servicioshealthy. `zap-final/manifest.json` declara el código b7765f5 y los hashes del script/policy ejecutados, comprobados contra los archivos preparados. `http_loopback_only` sí incluye ambos servicios y fue verdadero en los dos. Se preservan HTML/JSON/XML/Markdown/logs/verdict en `evidencias/fase-f/zap-final/`, separados del rojo inicial.

| Destino | Resultado de ZAP sin ocultar WARN | Evaluación de la propuesta |
| --- | --- | --- |
| server | baseline0;1INFO,0Low/Medium/High | INFO10049-1 documentado; cuatro grupos anteriores retirados por corrección. |
| front | baseline2;2Medium,6Low,3INFO |11grupos siguen presentes;10aceptaciones temporales/observaciones y1falso positivo limitado a12comentarios, todos sujetos a bindingloopback. |

`verdict.success=true` significa que cumple esa propuesta de tratamiento local, no ausencia de vulnerabilidades ni aceptación del equipo. La política mantiene los informes y bloquea alerta nueva, severidad cambiada, pérdida de binding exigido o cualquierHigh.16tests Python aprobados y revisión final del evaluador, política, fragmento y enlaces.

La integración en cd.yml y la ejecución de ZAP Actions siguen pendientes de coordinación con Ethian. No se declara F04 completo ni CD/SLO verde. El escaneo no cubre el protocoloTCP/WS ni el estado de una URL externa de producción.
