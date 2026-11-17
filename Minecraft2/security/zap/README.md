# ZAP baseline HTTP

`scan.py` ejecuta ZAP Baseline contra los dos servicios Compose después de readiness y humo. Solo comprueba HTTP: `mc2-server:8080/health` y `mc2-front:6080/vnc.html`. No analiza el protocolo de juego TCP/7777 ni WebSocket. No usa escaneo activo, reglas `IGNORE` ni la opción `-I`.

La imagen oficial ZAP 2.17.0 está fijada por digest en `scan.py`; el digest y el identificador de imagen descargada quedan registrados en `manifest.json`. El script y sus pruebas usan únicamente Python estándar. Para reproducir las pruebas:

```sh
python3 -m unittest discover -s Minecraft2/security/zap -v
```

El escáner falla ante error de ZAP, reporte ausente o vacío, origen inesperado, datos inválidos, hallazgo no tratado y cualquier hallazgo alto. Las decisiones se asocian al `alertRef` del grupo de hallazgos, separado por destino. `policy.json` permanece vacía hasta que se acuerde una propuesta. La propuesta de tratamiento temporal local contempla explícitamente los 11 grupos observados del front solo bajo su binding loopback, y los grupos INFO del server; aún requiere revisión de Ethian. No se presenta como política final ni como aceptación para producción. El requisito loopback se evalúa por separado para cada puerto HTTP; los hallazgos altos siempre bloquean.

El directorio `--out` conserva `server` y `front` en formatos `.html`, `.json`, `.xml`, `.md` y `.log`, además de `manifest.json` y `verdict.json`. El fragmento de CD añade `tests.log`, `image-pull.log` y `runner.log`. El manifiesto liga la corrida al SHA, imagen, red, contenedores, hashes del script y de la política; el veredicto registra éxito o error. El artefacto se publica con `always()` para conservar evidencia aun si el gate falla.

## Estado de integración

La primera corrida aislada real en Windows con Docker, y el reescaneo posterior del servidor corregido, se conservan fuera del árbol Minecraft2, en `evidencias/fase-f/zap/`, con `server.*`, `front.*`, `manifest.json` y `verdict.json`. El manifiesto histórico registra `front_loopback_only` y no permite inferir el binding loopback del server. La versión actual del script emite `http_loopback_only` como mapa por destino y comprueba cada puerto HTTP por separado; no atribuir ese esquema nuevo al manifiesto histórico. Los `alertRef` `90004-2` y `90004-3` son grupos separados (COEP y COOP). La ejecución de ZAP en GitHub Actions sigue pendiente de integración por Ethian; por tanto, el gate aún no está acreditado en Actions. El último CD consultado (44005663312, main9d44297) presentó un fallo de SLO: no declarar verde la fase ni atribuir al baseline ZAP la validación del SLO. El fragmento `docs/fragmentos/zap-cd.yml` indica los dos puntos de inserción en el workflow existente.


Reescaneo local22:55:58 del16/11/2026: servidor solo1INFO; front conserva2medias/6bajas/3INFO. El evaluador pasa con la propuesta explícita y loopback comprobado, sin ocultar baseline2 del front. Su aprobación e integración en Actions siguen pendientes. El escáner HTTP no prueba ausencia de riesgos en noVNC ni permite publicar el front fuera de loopback con esas decisiones.
