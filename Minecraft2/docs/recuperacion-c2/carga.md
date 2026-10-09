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

**Pendiente de ejecución por Ethian sobre el candidato integrado.** El SLO y los ajustes de conteo se fijan antes de medir. Se requieren baseline y estrés con el mismo commit, JDK 17 y heap, sin fuentes/POM/protocolo modificados. Copiar a perf/results/ los CSV, consola y manifiestos revisados antes de limpiar target/. Informar las tres repeticiones de 80, o declarar que ese nivel no se alcanzó; no marcar cumplimiento por la mediana si una repetición incumplió.

## 7. Análisis

**Pendiente de Ethian.** Relacionar p95, throughput, errores, población realmente sostenida y búsquedas/expansiones A*; distinguir correlación de un cuello de botella demostrado. Explicar el rango ensayado, la parada y lo que la arquitectura permite cambiar. No se atribuirá ninguna mejora de rendimiento al traslado entre capas sin una medición comparable: el harness no ejecuta la ruta de control del jugador.
