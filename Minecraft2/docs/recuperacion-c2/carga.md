# Carga de la IA de enemigos — recuperación del Corte 2

Correcciones REC-08 y REC-E4 a E9 del [plan de recuperación](plan-equipo.md). Responsable: Ethian Daniel White Ortiz.

Relación con la arquitectura: [ADR-001](../adr/ADR-001-estilo-recuperacion.md), criterio K6 y exigencia 3 del reto («coste de actualización»).

Los resultados anteriores, del 25 de septiembre, están en [corte2/testing/load-testing.md](../corte2/testing/load-testing.md). Son **históricos**: otro protocolo, una sola repetición y otro commit. No se mezclan con los de este documento.

## 1. Qué se mide y qué no

**Unidad medida:** el *tick* de `EnemyUpdateService.update` con población fija. REC_BASELINE/REC_STRESS crean la sesión con `withHorde=false`: no ejecutan HordeManager ni el tick completo de GameSession. Incluyen FSM, percepción, A\*, movimiento, física/separación, ataques y despawn. El escenario histórico ENDURANCE sí usa HordeManager, pero no forma parte de estas corridas.

El harness es `test/loadtest/EnemyLoadHarness.java`, headless y sin LibGDX. Usa las clases reales de dominio y aplicación sobre un mundo generado con `ChunkGenerationService`.

**No mide:**

- El render, los FPS ni la GPU.
- El control del jugador: movimiento, física, colisión y estamina, que la recuperación traslada a aplicación (REC-T1).
- `PlayerLife` ni `PistolService` dentro de `GameSession`.

Por eso **ningún resultado de aquí es una garantía de FPS gráficos**. Tampoco mide el efecto del traslado de capas: el harness no pasa por `GameInput` ni por `PlayerControlService` (ver §6).

## 2. SLO (fijado antes de ejecutar)

> **SLO-CARGA-01.** Con la **población objetivo de 80 zombis activos** en el escenario de §3, el tiempo de actualización de IA por tick cumple **p95 ≤ 16,67 ms** y **0 ticks fallidos**, sosteniendo al menos 80 zombis vivos al inicio de cada tick, en cada una de las tres repeticiones.

- **16,67 ms** es el presupuesto de un fotograma a 60 FPS. Es una referencia: la IA comparte ese presupuesto con el render, que aquí no se mide. Cumplir el SLO es una condición necesaria para jugar a 60 FPS, no suficiente.
- **Por qué 80.** Es un objetivo experimental de carga fijado por el protocolo, entre las oleadas de 64 y 128 enemigos de VERY_HARD. No se presenta como una población deducida de oleadas superpuestas: HordeManager espera que la oleada anterior termine y haga despawn. El harness repone zombis vivos para aislar el coste por población. No simula una partida completa.
- **Ticks fallidos.** Se cuenta un tick fallido cuando `EnemyUpdateService.update` lanza una `RuntimeException`. El harness la cuenta y sigue simulando.

## 3. Protocolo (fijado antes de ejecutar)

| Parámetro | Valor |
| --- | --- |
| Mundo | 10 × 10 chunks (16 × 64 × 16 cada uno), generado con seed `20260925`; el mismo en todas las corridas |
| Dificultad | `VERY_HARD` en todas las corridas, para usar los mismos parámetros de IA en baseline y estrés |
| Tick simulado | 1/60 s. El harness simula los ticks tan rápido como puede; no espera tiempo real |
| Jugador | Recorre un círculo de radio 6 a 2 bloques/s alrededor del centro y reaparece al morir (se cuenta la muerte) |
| Población | Objetivo de zombis vivos al inicio del tick. Aparecen a 8–18 bloques y se reponen los muertos; se registra cualquier déficit |
| JVM | JDK 17, `-Xms1g -Xmx1g`, sin más flags. Mismo heap en todas las corridas |
| Calentamiento | 10 s simulados por nivel, con la misma configuración, **excluidos** del muestreo |
| Muestreo | Un tiempo por tick (`System.nanoTime`) |
| Repeticiones | 3 por nivel, de 30 s simulados (1 800 ticks) cada una |
| **Baseline** (`REC_BASELINE`) | 3 zombis |
| **Estrés** (`REC_STRESS`) | Escalonado: 20, 40, 80, 160 y 320 zombis |
| Parada del estrés | Se termina el nivel en curso (sus 3 repeticiones) y no se sube más si la **mediana** del p95 de esas repeticiones supera 16,67 ms o si hubo algún tick fallido o déficit de población |
| Límite real | 20 min de reloj por escenario (configurable). Si se alcanza, se registra y se detiene tras la repetición en curso |
| Resistencia | No se ejecuta en esta recuperación |

Si el estrés llega a 320 sin degradarse, se informará el **rango ensayado** sin afirmar que se encontró el límite del sistema.

## 4. Métricas (CSV)

Hay una fila por repetición:

| Columna | Definición |
| --- | --- |
| `zombies`, `repetition` | Población objetivo y número de repetición |
| `sim_seconds`, `frames` | Tiempo simulado y ticks intentados (sim = frames × 1/60 s) |
| `wall_seconds` | Tiempo real de la repetición, incluida la sobrecarga del harness (reponer zombis, mover jugador) |
| `update_mean_ms`, `update_p95_ms`, `update_max_ms` | Latencia del tick de IA, **solo ticks correctos**. El p95 es el percentil de rango más cercano, sobre los ticks correctos (1 800 muestras si no hay errores) |
| `ticks_per_wall_s` | Throughput real: `frames / wall_seconds` |
| `update_capacity_ticks_per_s` | Ticks de IA por segundo si solo corriera la IA: `ticks correctos / Σ tiempo de update` |
| `zombie_updates_per_wall_s` | `Σ zombis vivos al inicio de cada tick / wall_seconds` |
| `astar_calls`, `astar_calls_per_sim_s`, `astar_expansions` | Búsquedas A\* y nodos expandidos en la repetición |
| `avg_active`, `min_active` | Media y mínimo de zombis vivos al inicio de cada tick, después de reponer |
| `population_shortfall_ticks` | Ticks que no alcanzaron la población objetivo; invalidan la comprobación del SLO |
| `heap_mb` | Heap Java usado tras `System.gc()` al final de la repetición |
| `player_deaths` | Muertes del jugador simulado |
| `failed_ticks`, `error_rate` | Ticks con excepción y `failed_ticks / frames` |

**Tratamiento de muestras fallidas.** El tiempo de un tick fallido no entra en la media, el p95 ni el máximo: ese tick no terminó su trabajo y su tiempo sería engañosamente bajo. Se cuenta en `failed_ticks` y en el denominador de `error_rate` (todos los ticks intentados). Cualquier tick fallido incumple el SLO, sea cual sea su latencia. Un déficit de población impide declarar el SLO comprobado con 80 enemigos. Si el estrés se detiene antes de llegar a 80, ese objetivo queda sin comprobar; no se extrapola desde 20 o 40.

## 5. Cómo ejecutar

Desde `Minecraft2/`, con JDK 17 y Maven en el `PATH`:

```bash
perf/run-load.sh baseline
perf/run-load.sh stress
```

```powershell
.\perf\run-load.ps1 baseline
.\perf\run-load.ps1 stress
```

Cada ejecución compila con `mvn -q test-compile` y crea `target/load-results/<escenario>-<fecha>/` con tres archivos:

- `results.csv`: métricas de §4.
- `console.txt`: salida del harness.
- `manifest.txt`: commit, estado de fuentes/POM/protocolo, JDK, CPU, núcleos, RAM, SO, parámetros y SHA-256 del harness y de los scripts. No incluye rutas personales.

`mvn clean` borra `target/`. Las corridas seleccionadas se copian, tras revisarlas, a [`perf/results/`](../../perf/results/) (REC-E8).

## 6. Resultados

### 6.1 Condiciones (REC-E7)

| Campo | Valor |
| --- | --- |
| Commit medido | `d180eb9` (rama `feature/c2-Ethian`). Su árbol es idéntico al de `origin/main` `620f901`, candidato integrado de los tres frentes; `git diff 620f901 d180eb9` está vacío |
| Estado de fuentes | `uncommitted_files_src_test_perf=0` en ambos manifiestos: fuentes, pruebas, `perf/`, POM y este protocolo sin cambios locales |
| JDK | Temurin 17.0.20.1+1, `-Xms1g -Xmx1g`, sin más flags |
| Equipo | AMD Ryzen 5 4500U (6 CPU lógicas), 7,4 GB de RAM, Windows 11 Pro 10.0.26200 |
| Ejecución | `perf/run-load.sh baseline` y `perf/run-load.sh stress` desde `Minecraft2/` (Git Bash), consecutivas, el 9 de octubre de 2026 entre 23:27 y 23:30 (−05:00). El script PowerShell no se usó en estas corridas |
| Protocolo | §3 sin cambios. Límite real de 20 min por escenario, no alcanzado (baseline 13 s, estrés 2 min 6 s). Resistencia no ejecutada |
| Suite del mismo commit | `mvn clean verify`: 198 Surefire + 148 Failsafe, 0 fallos, errores u omitidas |

Corridas publicadas (REC-E8), con `results.csv`, `console.txt` y `manifest.txt` revisados: [`rec_baseline-20261009-232756`](../../perf/results/rec_baseline-20261009-232756/) y [`rec_stress-20261009-232820`](../../perf/results/rec_stress-20261009-232820/). Los manifiestos registran también los SHA-256 del harness y de los dos scripts.

### 6.2 Resultados por nivel

Rangos sobre las tres repeticiones de cada nivel. Latencias en ms por tick de IA. Todos los niveles tuvieron **0 ticks fallidos (error_rate 0)**, **0 ticks con déficit de población** y `min_active` igual a la población objetivo.

| Zombis | p95 rep 1 / 2 / 3 | Mediana p95 | Media | Máximo | Throughput real (ticks/s) | Zombis actualizados/s | A\* por corrida (por s simulado) | Expansiones A\* (por búsqueda) | Muertes del jugador | Heap |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 3 (baseline) | 0,185 / 0,189 / 0,072 | 0,185 | 0,103–0,268 | 13,0–56,8 | 3 678–9 561 | 11 035–28 682 | 361 (12,03) | 9 213 (25,5) | 7 | 71 MB |
| 20 | 2,847 / 2,576 / 2,590 | 2,590 | 0,880–1,154 | 88,8–205,6 | 864–1 132 | 17 275–22 644 | 1 384 (46,13) | 96 432 (69,7) | 13 | 71 MB |
| 40 | 5,397 / 5,013 / 5,212 | 5,212 | 2,060–2,222 | 168,2–182,7 | 449–485 | 17 978–19 385 | 2 716 (90,53) | 229 802 (84,6) | 27 | 71–72 MB |
| **80** | **9,857 / 9,829 / 7,048** | **9,829** | 4,103–5,543 | 311,4–544,5 | 180–244 | 14 415–19 483 | 5 602 (186,73) | 520 175 (92,9) | 55 | 71–72 MB |
| 160 | 60,383 / 54,835 / 59,893 | 59,893 | 9,819–10,554 | 373,8–516,3 | 95–102 | 15 149–16 284 | 11 990 (399,67) | 1 174 588 (98,0) | 69 | 72 MB |

La capacidad solo-IA (`update_capacity_ticks_per_s`) difiere del throughput real en menos de un 2 % en todos los niveles: la sobrecarga del harness es pequeña frente al tick de IA.

**Parada.** El estrés subió 20 → 40 → 80 → 160. En 160 la mediana del p95 (59,893 ms) superó 16,67 ms; se completaron sus tres repeticiones y no se ejecutó 320, según §3. No hubo fallos ni déficit que adelantaran la parada.

**SLO-CARGA-01: cumplido en este equipo.** Las **tres** repeticiones de 80 zombis tuvieron p95 ≤ 16,67 ms (el peor, 9,857 ms, usa el 59 % del presupuesto), 0 ticks fallidos y 80 zombis vivos al inicio de cada tick. El cumplimiento no se deduce de la mediana: cada repetición lo cumple por separado.

## 7. Análisis (REC-E9)

**Determinismo de la carga.** Las búsquedas A\*, las expansiones y las muertes del jugador son idénticas en las tres repeticiones de cada nivel. El trabajo simulado se repite exactamente; la variación entre repeticiones (por ejemplo, 80 rep 3 con p95 7,0 ms frente a 9,8 ms, o baseline rep 3 con 0,07 ms) procede de la ejecución en el equipo (compilación JIT, frecuencia de un procesador portátil, planificación del SO), no de una carga distinta. Por eso el protocolo exige tres repeticiones y el SLO se evalúa en cada una.

**Escalado observado.**

- Las búsquedas A\* crecen en proporción a la población: unas 2,3–2,5 por zombi y segundo simulado entre 20 y 160 (4,0 con 3 zombis). La política de repath acota la frecuencia por zombi; no hay búsqueda por tick.
- Las expansiones por búsqueda suben de 69,7 (20) a 98,0 (160): con más zombis rodeando al jugador, las rutas son más largas o rodean más.
- La media por tick crece algo más que linealmente: de unos 0,044–0,058 ms por zombi con 20 a 0,061–0,066 ms con 160.
- Entre 80 y 160 la población se duplica y la media también (×1,8–2,6), pero el p95 se multiplica por 5,6–8,6. La relación p95/media pasa de 1,7–1,9 con 80 a 5,5–6,1 con 160. La degradación que detiene el estrés es sobre todo de **cola**: más ticks caros, no un encarecimiento uniforme de todos.
- El throughput en zombis actualizados por segundo se mantiene en el mismo orden (14 000–23 000) de 20 a 160, mientras los ticks por segundo bajan casi en proporción inversa a la población.
- El heap usado tras GC se mantiene en 71–72 MB en todos los niveles; no se observa crecimiento de memoria en estas corridas de 30 s.

**Cuello de botella: correlación, no demostración.** El harness mide el tick completo de `EnemyUpdateService.update`, no cada componente, y no se ejecutó un perfilador. El código contiene dos candidatos cuyo coste crece con la población:

1. A\* (`AStarPathfinder`, hasta 2 000 expansiones por búsqueda): búsquedas lineales en la población y más expansiones por búsqueda. Correlaciona con el aumento de la media.
2. Separación entre zombis (`ZombieSeparation.resolve`, hasta 32 pasadas sobre todos los pares, y `isOccupied` para cada zombi que persigue): coste cuadrático en la población (los pares por pasada se cuadruplican al pasar de 80 a 160; las pasadas terminan antes si no hay solapes), compatible con el salto de la cola.

Ninguno de los dos queda demostrado como causa del p95 de 160. Para separarlos habría que medir por componente (tiempo de A\* frente a separación por tick) o perfilar ese nivel; queda como siguiente paso, no como resultado.

**Máximos.** Todos los niveles tienen ticks aislados muy por encima del presupuesto: 13–57 ms incluso con 3 zombis y 311–545 ms con 80. Con 3 zombis la carga de IA es mínima, por lo que al menos parte de esos picos se debe al entorno (JIT, SO); en niveles altos no se puede separar del trabajo propio sin la medición por componente. El SLO usa el p95, no el máximo: cumplirlo no garantiza que ningún tick supere 16,67 ms.

**Relación con la arquitectura.** La carga valida la exigencia 3 del reto y el criterio K6 del [ADR-001](../adr/ADR-001-estilo-recuperacion.md) solo para la IA: con 80 zombis la actualización cabe en el presupuesto con margen en este equipo. La separación en capas permite actuar sobre los dos candidatos sin tocar presentación: A\* y la separación están en `domain.enemy`, detrás de `EnemyUpdateService`. Escalonar repaths, reducir el presupuesto de expansiones (ya configurable en `AStarPathfinder`) o indexar vecinos para la separación son cambios de dominio/aplicación que el harness puede volver a medir con este mismo protocolo. Ninguno se ha aplicado: cualquier cambio exigiría repetir baseline y estrés.

**Límites.**

- Headless: no mide render, GPU ni FPS. El SLO es una condición necesaria para 60 FPS, no suficiente. Ningún resultado de aquí se atribuye a los FPS gráficos.
- No ejecuta `GameInput`, `PlayerControlService`, `HordeManager`, `PistolService` ni el tick completo de `GameSession`. No se atribuye ninguna mejora ni coste de rendimiento al traslado entre capas; el coste por tick de `PlayerFrameInput` sigue sin medirse.
- Población fija con reposición, no oleadas reales: 80 es un objetivo experimental, no una situación de juego típica.
- Un solo equipo (portátil Ryzen 5 4500U, Windows 11). Otro hardware o sistema puede dar cifras distintas; las cifras no son comparables con las históricas del 25 de septiembre (otro protocolo, una repetición y otro commit).
- Rango ensayado 3–160 zombis. No se ensayó 320 por la regla de parada, ni resistencia prolongada.
