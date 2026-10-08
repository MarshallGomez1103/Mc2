# perf/ — carga headless de la IA de enemigos

Scripts reproducibles del protocolo de carga de la recuperación. El SLO, el protocolo, las métricas y el análisis están en [docs/recuperacion-c2/carga.md](../docs/recuperacion-c2/carga.md).

| Comando (desde `Minecraft2/`) | Escenario |
| --- | --- |
| `perf/run-load.sh baseline` / `.\perf\run-load.ps1 baseline` | `REC_BASELINE`: 3 zombis, 3 × 30 s simulados |
| `perf/run-load.sh stress` / `.\perf\run-load.ps1 stress` | `REC_STRESS`: 20, 40, 80, 160 y 320 zombis, con parada por presupuesto |

El segundo argumento opcional es el límite de tiempo real en minutos. Por defecto son 20.

Requisitos: JDK 17 y Maven en el `PATH`, y `git` para el manifiesto. No hace falta GPU ni ventana.

Cada corrida escribe `target/load-results/<escenario>-<fecha>/` con `results.csv`, `console.txt` y `manifest.txt`. Como `mvn clean` borra `target/`, las corridas que se publican se revisan y se copian a [`results/`](results/).
