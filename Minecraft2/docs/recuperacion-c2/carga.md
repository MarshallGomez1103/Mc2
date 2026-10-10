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

**Herramienta.** k6, JMeter y Gatling generan peticiones HTTP. Minecraft2 es un proceso de escritorio sin servidor ni red, y la operación que el reto pone bajo carga es el tick de IA dentro de la JVM. Por eso el equivalente es un harness Java versionado que llama a las clases reales, mide cada tick con `System.nanoTime` y se lanza con los scripts de `perf/`.

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

Hubo dos mediciones con el mismo protocolo, JDK, heap y equipo. La primera, sobre el candidato integrado, se perfiló después con JFR y reveló un defecto en el índice de chunks (§7.1). Se corrigió con una prueba de regresión y se repitieron baseline y estrés. **El resultado que se reporta y contra el que se evalúa el SLO es la medición final.** La primera se conserva como el «antes» de la corrección.

| Campo | Medición final (candidato entregado) | Medición previa (antes de la corrección) |
| --- | --- | --- |
| Commit medido | `c2e03e7`: corrige la clave del índice de chunks en `World` y añade la regresión en `WorldChunkIndexTest` | `d180eb9`, mismo árbol de fuentes que `620f901` |
| Corridas | [`rec_baseline-20261010-003702`](../../perf/results/rec_baseline-20261010-003702/), [`rec_stress-20261010-003715`](../../perf/results/rec_stress-20261010-003715/) | [`rec_baseline-20261009-232756`](../../perf/results/rec_baseline-20261009-232756/), [`rec_stress-20261009-232820`](../../perf/results/rec_stress-20261009-232820/) |
| Fecha (−05:00) | 10 de octubre de 2026, 00:37–00:38 | 9 de octubre de 2026, 23:27–23:30 |
| Duración real | Baseline 9 s; estrés 1 min 27 s | Baseline 13 s; estrés 2 min 6 s |

Comunes a ambas: Temurin 17.0.20.1+1 con `-Xms1g -Xmx1g`; AMD Ryzen 5 4500U (6 CPU lógicas), 7,4 GB de RAM, Windows 11 Pro 10.0.26200; `perf/run-load.sh baseline` y `perf/run-load.sh stress` desde `Minecraft2/` con Git Bash (el script PowerShell no se usó en estas corridas). Los manifiestos registran `uncommitted_files_src_test_perf=0` y el mismo SHA-256 del harness y de los scripts. El protocolo de §3 y el SLO de §2 no cambiaron. No se alcanzó el límite de 20 min ni se ejecutó resistencia.

### 6.2 Resultados finales por nivel

Rangos sobre las tres repeticiones; latencias en ms por tick de IA. Todos los niveles tuvieron **0 ticks fallidos (error_rate 0)**, **0 ticks con déficit de población** y `min_active` igual a la población objetivo. El heap usado tras GC fue de 71–72 MB en todos.

| Zombis | p95 rep 1 / 2 / 3 | Mediana p95 | Media | Máximo | Throughput real (ticks/s) | Zombis actualizados/s | A\* por corrida (por s simulado) | Expansiones A\* (por búsqueda) | Muertes del jugador |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 3 (baseline) | 0,050 / 0,044 / 0,023 | 0,044 | 0,029–0,059 | 2,2–6,6 | 16 115–33 511 | 48 344–100 534 | 361 (12,03) | 9 213 (25,5) | 7 |
| 20 | 1,137 / 1,388 / 0,650 | 1,137 | 0,244–0,479 | 51,0–72,9 | 2 061–4 079 | 41 221–81 585 | 1 384 (46,13) | 96 432 (69,7) | 13 |
| 40 | 1,034 / 1,066 / 1,109 | 1,066 | 0,434–0,483 | 48,7–56,9 | 2 064–2 296 | 82 575–91 847 | 2 716 (90,53) | 229 802 (84,6) | 27 |
| **80** | **1,759 / 1,926 / 1,679** | **1,759** | 0,982–1,033 | 72,6–86,8 | 965–1 016 | 77 232–81 275 | 5 602 (186,73) | 520 175 (92,9) | 55 |
| 160 | 13,253 / 13,270 / 13,044 | 13,253 | 2,438–2,469 | 104,4–114,3 | 404–410 | 64 705–65 549 | 11 990 (399,67) | 1 174 588 (98,0) | 69 |
| 320 | 50,085 / 50,395 / 50,341 | 50,341 | 7,475–7,704 | 190,6–212,2 | 130–134 | 41 489–42 764 | 29 289 (976,30) | 3 348 358 (114,3) | 144 |

La capacidad solo-IA (`update_capacity_ticks_per_s`) difiere del throughput real en menos de un 2 %: la sobrecarga del harness es pequeña frente al tick de IA.

**Parada.** El estrés recorrió todo el escalonado: 20 → 40 → 80 → 160 → 320. En 320 la mediana del p95 (50,341 ms) superó 16,67 ms; se completaron sus tres repeticiones. Era el último nivel del protocolo.

**SLO-CARGA-01: cumplido en este equipo.** Las **tres** repeticiones de 80 zombis tuvieron p95 ≤ 16,67 ms (el peor, 1,926 ms, usa el 12 % del presupuesto), 0 ticks fallidos y 80 zombis vivos al inicio de cada tick. Cada repetición lo cumple por separado. Con 160 zombis las tres repeticiones también quedan dentro del presupuesto (≤ 13,270 ms). La degradación aparece entre 160 y 320.

### 6.3 Antes y después de la corrección

Mismo escenario y misma carga simulada: las búsquedas A\*, las expansiones y las muertes del jugador son idénticas en ambas mediciones para cada nivel. La corrección no cambió la conducta de la IA, solo el coste de cada consulta al mundo.

| Zombis | p95 antes (`d180eb9`), rep 1 / 2 / 3 | p95 después (`c2e03e7`), rep 1 / 2 / 3 | Media antes → después | Throughput antes → después (ticks/s) |
| --- | --- | --- | --- | --- |
| 3 | 0,185 / 0,189 / 0,072 | 0,050 / 0,044 / 0,023 | 0,103–0,268 → 0,029–0,059 | 3 678–9 561 → 16 115–33 511 |
| 20 | 2,847 / 2,576 / 2,590 | 1,137 / 1,388 / 0,650 | 0,880–1,154 → 0,244–0,479 | 864–1 132 → 2 061–4 079 |
| 40 | 5,397 / 5,013 / 5,212 | 1,034 / 1,066 / 1,109 | 2,060–2,222 → 0,434–0,483 | 449–485 → 2 064–2 296 |
| 80 | 9,857 / 9,829 / 7,048 | 1,759 / 1,926 / 1,679 | 4,103–5,543 → 0,982–1,033 | 180–244 → 965–1 016 |
| 160 | 60,383 / 54,835 / 59,893 (parada) | 13,253 / 13,270 / 13,044 | 9,819–10,554 → 2,438–2,469 | 95–102 → 404–410 |
| 320 | no ejecutado (parada en 160) | 50,085 / 50,395 / 50,341 (parada) | — → 7,475–7,704 | — → 130–134 |

La medición previa ya cumplía el SLO con 80 zombis (p95 ≤ 9,857 ms). La corrección amplía el margen y mueve el punto de degradación de 160 a 320 zombis.

## 7. Análisis (REC-E9)

### 7.1 Cuello de botella identificado

Tras la primera medición se repitió el estrés con el perfilador incluido en el JDK (JFR, configuración `profile`), con el mismo commit, heap y escenario. Estas corridas perfiladas son diagnóstico: el perfilador añade coste y sus tiempos no se usan como resultado. El [método y el resumen](../../perf/results/diagnostico-jfr/README.md) están versionados.

Atribución de las muestras de CPU tomadas dentro de `EnemyUpdateService.update`, en el tramo de 160 zombis:

| Componente (inclusivo) | Antes (`d180eb9`) | Después (`c2e03e7`) |
| --- | --- | --- |
| A\* (`AStarPathfinder.findPath`) | 91,7 % | 83,4 % |
| … de ello, `World.findChunk` | 51,0 % | 5,8 % |
| … búsquedas en cubetas en árbol de `HashMap` | 43,1 % | 1,2 % |
| Lectura de bloques (`Chunk.getBlock`) | 22,3 % | 42,8 % |
| Separación (`ZombieSeparation.resolve` + `isOccupied`) | 4,5 % | 11,8 % |
| Movimiento sin A\* | 2,1 % | 3,3 % |

**Causa demostrada.** El índice de chunks de `World` usaba como clave un `long` `(x << 32) ^ z`, cuyo `hashCode` es `x ^ z`. En el mundo de 10 × 10 chunks del protocolo solo había 16 hashes distintos para 100 chunks; en 32 × 32 coordenadas, 32 hashes para 1 024 claves. El `HashMap` convertía esas cubetas en árboles y cada consulta pasaba por comparaciones y reflexión (`comparableClassFor`). A\* consulta el mundo en cada nodo que expande, así que ese coste se multiplicaba por las expansiones.

**Corrección.** `World.ChunkKey` es un record con `hashCode` explícito (`x · 1 000 003 + z`), sin colisiones en el rango de los mundos. La regresión `WorldChunkIndexTest.chunkKeysOfTheLargestWorldHaveDistinctHashCodes` falló con la clave anterior (1 024 claves, 32 hashes) y pasa con la nueva. La regla de dominio no cambió: `findChunk` sigue devolviendo el mismo chunk para cada coordenada, como comprueba la prueba existente del índice.

### 7.2 Qué limita ahora

En la corrida perfilada posterior a la corrección, con 320 zombis: A\* 75,0 %, separación 20,9 % y `World.findChunk` 3,9 % del tiempo de IA. El coste restante de A\* está en leer bloques del mundo en cada nodo que expande (`Chunk.getBlock`, 39,4 %).

- **A\*.** Las búsquedas crecen en proporción a la población: unas 2,3–3,1 por zombi y segundo simulado, porque la política de repath acota la frecuencia; no hay búsqueda por tick. Las expansiones por búsqueda suben de 69,7 (20) a 114,3 (320): con más zombis rodeando al jugador, las rutas son más largas o rodean más.
- **Separación.** Es cuadrática en la población: hasta 32 pasadas sobre todos los pares y una comprobación `isOccupied` por cada zombi que persigue. Pasa del 11,8 % (160) al 20,9 % (320) del tiempo de IA. Es el componente que más crece al subir la población.
- **Cola.** Con 320 la relación p95/media es de 6,5–6,7: la degradación es sobre todo de ticks caros (coinciden muchos repaths), no un encarecimiento uniforme.
- **Máximos.** Hay ticks aislados muy por encima del presupuesto en todos los niveles: 2,2–6,6 ms con 3 zombis y 72,6–86,8 ms con 80. Con 3 zombis el trabajo de IA es mínimo, así que parte de esos picos se debe al entorno (JIT, SO). El SLO usa el p95: cumplirlo no garantiza que ningún tick supere 16,67 ms.
- **Variación entre repeticiones.** La carga simulada es idéntica en cada repetición, así que las diferencias de tiempo (por ejemplo, 20 zombis, rep 3) proceden de la ejecución en el equipo (JIT, frecuencia de un procesador portátil). Por eso el SLO se evalúa en cada repetición.

### 7.3 Relación con la arquitectura

La carga valida la exigencia 3 del reto y el criterio K6 del [ADR-001](../adr/ADR-001-estilo-recuperacion.md) para la IA. La separación en capas permitió localizar y corregir el cuello de botella en un solo archivo de dominio (`World`), sin tocar aplicación, presentación ni persistencia, y volver a medir con el mismo harness. Los siguientes pasos posibles también quedan en el dominio, detrás de `EnemyUpdateService`:

- cachear la caminabilidad de las celdas consultadas durante una búsqueda A\*;
- escalonar los repaths entre ticks o compartir rutas entre zombis cercanos;
- indexar los zombis por celda para que la separación no recorra todos los pares.

No se han aplicado: cada uno exigiría repetir baseline y estrés.

**Límites.**

- Headless: no mide render, GPU ni FPS. El SLO es una condición necesaria para 60 FPS, no suficiente; ningún resultado de aquí se atribuye a los FPS gráficos.
- No ejecuta `GameInput`, `PlayerControlService`, `HordeManager`, `PistolService` ni el tick completo de `GameSession`. La mejora medida se debe a la corrección del índice, no al traslado entre capas; el coste por tick de `PlayerFrameInput` sigue sin medirse.
- Población fija con reposición, no oleadas reales: 80 es un objetivo experimental, no una situación de juego típica.
- Un solo equipo (portátil Ryzen 5 4500U, Windows 11). Otro hardware puede dar cifras distintas. No son comparables con las cifras históricas del 25 de septiembre (otro protocolo, una repetición y otro commit).
- Rango ensayado 3–320 zombis; no se ejecutó resistencia prolongada.
