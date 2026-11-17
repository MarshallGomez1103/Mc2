# Observabilidad (adicional C3-08)

Prometheus y Grafana sobre `/metrics` de `mc2-server`. Las unidades son **ticks del servidor**: duración del tick, ticks por segundo y ticks fallidos; no hay latencia HTTP ni FPS.

| Archivo | Contenido |
| --- | --- |
| `compose.yml` | Servicios `prometheus` (`prom/prometheus:v3.15.0`, `127.0.0.1:9090`) y `grafana` (`grafana/grafana:13.2.3`, `127.0.0.1:3000`), que se suman al `docker-compose.yml` de la raíz. |
| `prometheus.yml` | Recolección de `mc2-server:8080/metrics` cada 5 s. |
| `reglas.yml` | Reglas de registro (p95 del tick, throughput y tasa de error) y tres alertas. |
| `grafana/` | Fuente de datos y tablero «Minecraft2 — SLO del servidor» aprovisionados. |

## Arranque

Desde la raíz del repositorio:

```bash
cp .env.example .env
docker compose -f docker-compose.yml -f observabilidad/compose.yml up --build --wait
```

Grafana abre directamente el tablero, en solo lectura y sin iniciar sesión (la cuenta de administración no se usa, así que no hay contraseña que versionar). Los dos puertos se publican solo en `localhost`.

## Tablero

| Panel | Consulta | Relación con el SLO |
| --- | --- | --- |
| p95 del tick | `mc2:tick_duration_seconds:p95_5m` = `histogram_quantile(0.95, sum by (le) (rate(mc2_tick_duration_seconds_bucket[5m])))`, con línea en 16,67 ms | SLO-CARGA-01 |
| Tasa de error | `mc2:tick_failures:ratio_5m` y ticks con déficit de población sobre ticks totales | SLO-CARGA-01 (0 fallidos, 0 con déficit) |
| Throughput | `mc2:ticks:rate_5m` (20 esperados con `MC2_TICK_HZ=20`) | Capacidad del servidor |
| Métrica de negocio | `mc2_players_connected` y `mc2_zombies_active` | Jugadores y zombis activos |
| Servidor alcanzable | `up{job="mc2-server"}` | Disponibilidad |
| Alertas | Estado de las reglas de alerta | — |

## Alertas

| Alerta | Condición | Severidad |
| --- | --- | --- |
| `Mc2TickP95SobreElSLO` | p95 del tick en 5 min > 16,67 ms durante 2 min | aviso |
| `Mc2TicksFallidos` | algún tick fallido en 5 min | crítica |
| `Mc2ServidorSinMetricas` | Prometheus no lee `/metrics` durante 1 min | crítica |

## Verificación

- En cada pull request que toca esta carpeta, el job `observabilidad-config` de [`.github/workflows/adicionales.yml`](../.github/workflows/adicionales.yml) ejecuta `promtool check config` y `promtool check rules`, valida el tablero y el Compose combinado.
- Con datos reales: el job `observabilidad-datos` (Actions → `adicionales` → *Run workflow* con una etiqueta publicada) levanta el juego con 80 zombis, Prometheus y Grafana, genera tráfico con el cliente de carga y deja en el resumen de la ejecución el resultado de cada consulta, las alertas cargadas y el tablero aprovisionado.

## Evidencia

Se registra aquí solo con ejecuciones reales.

| Fecha | Ejecución | Etiqueta | p95 del tick | Ticks/s | Zombis / jugadores | Alertas |
| --- | --- | --- | --- | --- | --- | --- |
