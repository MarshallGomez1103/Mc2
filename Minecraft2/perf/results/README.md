# perf/results/

Aquí van las corridas de carga **seleccionadas y revisadas**, copiadas desde `target/load-results/`. Cada corrida conserva su carpeta con `results.csv`, `console.txt` y `manifest.txt`.

Antes de copiar una corrida hay que revisar que:

- `manifest.txt` indica el commit medido y `uncommitted_files_src_test_perf=0`. Si no es así, se explica en [carga.md](../../docs/recuperacion-c2/carga.md).
- `console.txt` no contiene rutas personales del equipo ni trazas ajenas a la corrida.
- La corrida siguió el protocolo de carga.md §3.

## Corridas publicadas

| Carpeta | Escenario | Commit medido | Niveles | Resultado |
| --- | --- | --- | --- | --- |
| [rec_baseline-20261010-003702](rec_baseline-20261010-003702/) | `REC_BASELINE`, **final** | `c2e03e7` | 3 zombis × 3 rep | Mediana p95 0,044 ms; 0 fallidos; sin déficit |
| [rec_stress-20261010-003715](rec_stress-20261010-003715/) | `REC_STRESS`, **final** | `c2e03e7` | 20, 40, 80, 160, 320 × 3 rep | 80: p95 ≤ 1,926 ms en las tres; 160: ≤ 13,270 ms; parada en 320 (mediana p95 50,341 ms); 0 fallidos; sin déficit |
| [rec_baseline-20261009-232756](rec_baseline-20261009-232756/) | `REC_BASELINE`, previa | `d180eb9` | 3 zombis × 3 rep | Mediana p95 0,185 ms; 0 fallidos; sin déficit |
| [rec_stress-20261009-232820](rec_stress-20261009-232820/) | `REC_STRESS`, previa | `d180eb9` | 20, 40, 80, 160 × 3 rep | 80: p95 ≤ 9,857 ms en las tres; parada en 160 (mediana p95 59,893 ms); 0 fallidos; sin déficit |

Las cuatro cumplen las comprobaciones anteriores: JDK 17, heap de 1 GB y mismo equipo, manifiestos con `uncommitted_files_src_test_perf=0` y consola sin rutas personales. Las corridas previas se conservan como el «antes» de la corrección del índice de chunks. El [diagnóstico con JFR](diagnostico-jfr/README.md) explica qué la motivó. Resultados y análisis: [carga.md §6–§7](../../docs/recuperacion-c2/carga.md#6-resultados).
