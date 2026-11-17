# perf/ — carga headless de la IA de enemigos

Scripts reproducibles del protocolo de carga de la recuperación. El SLO, el protocolo, las métricas y el análisis están en [docs/recuperacion-c2/carga.md](../docs/recuperacion-c2/carga.md).

| Comando (desde `Minecraft2/`) | Escenario |
| --- | --- |
| `perf/run-load.sh baseline` / `.\perf\run-load.ps1 baseline` | `REC_BASELINE`: 3 zombis, 3 × 30 s simulados |
| `perf/run-load.sh stress` / `.\perf\run-load.ps1 stress` | `REC_STRESS`: 20, 40, 80, 160 y 320 zombis, con parada por presupuesto |

El segundo argumento opcional es el límite de tiempo real en minutos. Por defecto son 20.

Requisitos: JDK 17 y Maven en el `PATH`, y `git` para el manifiesto. No hace falta GPU ni ventana.

Cada corrida escribe `target/load-results/<escenario>-<fecha>/` con `results.csv`, `console.txt` y `manifest.txt`. Como `mvn clean` borra `target/`, las corridas que se publican se revisan y se copian a [`results/`](results/).

## Carga contra el servidor desplegado (Corte 3)

`perf/run-slo.sh` mide SLO-CARGA-01 y SLO-RED-01 contra un `mc2-server` en ejecución (por defecto `localhost:7777` y `localhost:8080`): conecta clientes del protocolo v1, mide la ida y vuelta entrada → estado y lee `/metrics` antes y después de cada repetición. Sale con código distinto de cero si falta un resultado, no se alcanza la población o se incumple un umbral.

| Comando (desde `Minecraft2/`) | Escenario |
| --- | --- |
| `perf/run-slo.sh corta` | 3 repeticiones de 30 s; la ejecuta `cd.yml` después de cada despliegue |
| `perf/run-slo.sh larga` | 3 repeticiones de 300 s; la ejecuta `load.yml` bajo demanda |

El servidor debe estar levantado con la población de carga, por ejemplo `MC2_LOAD_ZOMBIES=80 docker compose up --build --wait` desde la raíz del repositorio. Cada corrida escribe `target/load-results/slo-<escenario>-<fecha>/` con `manifest.txt`, `console.txt`, `results.csv` y `verdict.txt`. Umbrales, variables y forma de romper un umbral a propósito: [devops.md](../docs/devops.md#umbrales-del-slo-en-el-despliegue).
