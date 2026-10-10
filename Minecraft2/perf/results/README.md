# perf/results/

Aquí van las corridas de carga **seleccionadas y revisadas**, copiadas desde `target/load-results/`. Cada corrida conserva su carpeta con `results.csv`, `console.txt` y `manifest.txt`.

Antes de copiar una corrida hay que revisar que:

- `manifest.txt` indica el commit medido y `uncommitted_files_src_test_perf=0`. Si no es así, se explica en [carga.md](../../docs/recuperacion-c2/carga.md).
- `console.txt` no contiene rutas personales del equipo ni trazas ajenas a la corrida.
- La corrida siguió el protocolo de carga.md §3.

## Corridas publicadas

| Carpeta | Escenario | Commit medido | Niveles | Resultado |
| --- | --- | --- | --- | --- |
| [rec_baseline-20261009-232756](rec_baseline-20261009-232756/) | `REC_BASELINE` | `d180eb9` | 3 zombis × 3 rep | Mediana p95 0,185 ms; 0 fallidos; sin déficit |
| [rec_stress-20261009-232820](rec_stress-20261009-232820/) | `REC_STRESS` | `d180eb9` | 20, 40, 80, 160 × 3 rep | 80: p95 ≤ 9,857 ms en las tres; parada en 160 (mediana p95 59,893 ms); 0 fallidos; sin déficit |

Ambas cumplen las tres comprobaciones anteriores: mismo commit, JDK 17, heap de 1 GB y equipo; manifiestos con `uncommitted_files_src_test_perf=0`; consola sin rutas personales. Resultados y análisis: [carga.md §6–§7](../../docs/recuperacion-c2/carga.md#6-resultados).
