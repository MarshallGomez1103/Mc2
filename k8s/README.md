# Kubernetes (adicional C3-07)

Despliegue alternativo de `mc2-server` y `mc2-front` en un clúster local de [kind](https://kind.sigs.k8s.io/). No reemplaza el despliegue principal con Compose ([devops.md](../Minecraft2/docs/devops.md#6-despliegue)); usa las mismas imágenes de GHCR y las mismas variables del contrato común.

| Archivo | Contenido |
| --- | --- |
| `namespace.yaml` | Espacio de nombres `mc2` con Pod Security Admission `restricted`. |
| `configmap.yaml` | `mc2-config`: las variables `MC2_*` del contrato. |
| `secret.example.yaml` | Plantilla **sin credenciales** del Secret de descarga de GHCR; solo se crea si las imágenes son privadas y no está en `kustomization.yaml`. |
| `server.yaml` | `ServiceAccount`, `PersistentVolumeClaim` de mundos, `Deployment` (1 réplica, estrategia `Recreate`) y `Service` (7777 y 8080) de `mc2-server`. |
| `front.yaml` | `ServiceAccount`, `Deployment` y `Service` (6080) de `mc2-front`. |
| `networkpolicy.yaml` | Entrada denegada por defecto y abierta solo donde hace falta. |
| `rbac.yaml` | `Role` y `RoleBinding` del desplegador, limitados a `mc2`. |
| `kustomization.yaml` | Une todo y fija la etiqueta de las imágenes. |
| `kind.yaml` | Clúster de un nodo. |

## Controles

- **Probes.** `mc2-server`: `startupProbe`, `readinessProbe` y `livenessProbe` sobre `GET /health`. `mc2-front`: readiness sobre noVNC (`/vnc.html`) y liveness que exige que el proceso del juego esté vivo (el mismo criterio que el healthcheck de Compose).
- **Recursos.** Peticiones y límites de CPU y memoria en los dos contenedores. El servidor pide 250m/512Mi con límite 2 CPU/1Gi; el front, que dibuja con OpenGL por software, 500m/768Mi con límite 2 CPU/1,5Gi.
- **Contexto de seguridad.** Usuario 10001 sin root, sin escalada de privilegios, sin capacidades, `seccomp` `RuntimeDefault` y raíz de solo lectura; solo `/tmp`, `/home/app`, `/dev/shm` y los volúmenes de datos son escribibles. Ninguna cuenta de servicio monta su token.
- **NetworkPolicy (justificación).** El puerto de juego no autentica a quien se conecta ([seguridad.md](../Minecraft2/docs/seguridad.md#21-autenticación-y-autorización)): sin política, cualquier pod del clúster podría enviar entradas al servidor o leer sus métricas. `denegar-entrada` niega toda entrada en `mc2`; `mc2-server-entrada` abre 7777 y 8080 solo a pods con `mc2/cliente=true` (el front y el cliente de carga) y 8080 a Prometheus (`mc2/metricas=true` o el espacio de nombres `monitoring`); `mc2-front-entrada` deja el 6080 solo dentro del espacio de nombres. Las sondas del kubelet no pasan por la política.
- **RBAC (justificación).** Quien despliega no necesita ser administrador del clúster: `mc2-desplegador` solo puede crear y actualizar los recursos de la aplicación en `mc2` y leer pods, registros y eventos. No puede leer Secrets, crear pods sueltos, ejecutar comandos en los contenedores ni tocar otros espacios de nombres.

## Paso a paso en kind

Requisitos: Docker, `kubectl` y `kind` (v0.27.0). Desde la raíz del repositorio, con las imágenes construidas localmente (`docker compose build`) o descargadas de GHCR:

```bash
kind create cluster --name mc2 --config k8s/kind.yaml
kind load docker-image --name mc2 ghcr.io/marshallgomez1103/mc2-server:local ghcr.io/marshallgomez1103/mc2-front:local
kubectl apply -k k8s/
kubectl -n mc2 rollout status deployment/mc2-server --timeout=180s
kubectl -n mc2 rollout status deployment/mc2-front --timeout=240s
kubectl -n mc2 get deploy,po,svc,pvc,networkpolicy,role,rolebinding
```

Acceso: `kubectl -n mc2 port-forward svc/mc2-front 6080:6080` y abrir `http://localhost:6080/vnc.html`; `kubectl -n mc2 port-forward svc/mc2-server 8080:8080 7777:7777` para `/health`, `/metrics` y el JAR del cliente.

Verificación de la política: un pod sin la etiqueta `mc2/cliente=true` no alcanza `mc2-server:7777` y uno con la etiqueta sí. El job `kubernetes-kind` de [`.github/workflows/adicionales.yml`](../.github/workflows/adicionales.yml) hace todo lo anterior con una etiqueta publicada en GHCR (Actions → `adicionales` → *Run workflow*), comprueba la política con dos pods `busybox` y deja el estado de los recursos en el resumen de la ejecución.

Limpieza: `kind delete cluster --name mc2`.

## Evidencia

Se registra aquí solo con ejecuciones reales (enlace a la ejecución de `adicionales.yml` o comandos con su salida).

| Fecha | Ejecución | Etiqueta | Recursos listos | NetworkPolicy verificada |
| --- | --- | --- | --- | --- |
