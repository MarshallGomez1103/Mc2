# Load/performance testing — C2-10

Estado: harness construido y los cuatro escenarios ejecutados una vez. Owner: Estudiante 3.
Plan operativo: [PLAN_AGENTE_ETHIAN.md](../PLAN_AGENTE_ETHIAN.md).

## Harness

`test/loadtest/EnemyLoadHarness.java` es una clase con `main`, no un test JUnit, así que
`mvn clean test` no la ejecuta. Es headless: usa dominio y application, sin LibGDX.

```text
mvn test-compile
java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness BASELINE
java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness PEAK
java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness STRESS
java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness ENDURANCE 30
```

En Linux/macOS el separador del classpath es `:`. Añadir `-Dsun.stdout.encoding=UTF-8` si la
consola muestra mal las tildes.

**Qué hace:**

- Genera el terreno real con `ChunkGenerationService` y la seed fija `20260925`.
- Simula fotogramas de 1/60 s tan rápido como puede. El tiempo simulado y el tiempo real se
  registran por separado.
- Mide el wall-clock de `HordeManager.update + EnemyUpdateService.update` en cada fotograma.
- Cuenta las búsquedas y expansiones de A\* con `AStarPathfinder.getSearchCount/getTotalExpansions`.
- Mide el heap después de `System.gc()`, en MB.
- Cuenta los errores: una excepción en un fotograma se registra y la simulación sigue.

**Jugador:**

- Recorre un círculo de radio 6 a 2 bloques/s alrededor del centro del mundo, siempre sobre la
  superficie. Así se fuerzan repaths.
- Si un zombi lo mata, reaparece en el acto y la muerte se cuenta.

**Poblaciones y calentamiento:**

- BASELINE, PEAK y STRESS mantienen una población fija: los zombis aparecen en un anillo de
  8–18 bloques alrededor del jugador y se reponen si faltan.
- ENDURANCE usa las oleadas reales de `HordeManager`. Cada zombi muere a los 15 s simulados, así
  que el ciclo spawn → persecución → muerte → despawn → oleada nunca se detiene.
- Antes de cada población se hace un calentamiento del JIT de 10 s simulados, que no se registra.

Los CSV quedan en `target/load-results/<escenario>-<fecha>.csv`. `mvn clean` los borra: copiarlos
fuera antes de limpiar si se van a conservar.

## Entorno de la ejecución

| Dato | Valor |
| --- | --- |
| Fecha | 2026-09-25 |
| Código | rama `feature/c2-Ethian`, commit `0de00c7` + cambios de Estudiante 3 sin commitear |
| CPU | AMD Ryzen 5 4500U (6 núcleos), portátil |
| RAM | 7.4 GB |
| SO | Windows 11 Pro |
| JVM | Eclipse Adoptium 17.0.20.1, sin flags; heap máximo por defecto 1888 MB |
| Otras cargas | Ninguna prueba en paralelo; el equipo en uso normal de escritorio |

## Resultados

| Escenario | Configuración | Duración | AI update media / p95 / máx. | A\* | Heap | Errores |
| --- | --- | --- | --- | --- | --- | --- |
| BASELINE | 2×2 chunks, NORMAL, 3 zombis | 5 × 60 s simulados | 0.007–0.038 / 0.009–0.049 / 2.5–9.6 ms | 469 por repetición (7.8/s) | 4 MB | 0 |
| PEAK | 10×10 chunks, VERY_HARD, 25 zombis | 3 × 60 s simulados | 1.24–1.31 / 3.53–3.68 / 13–39 ms | 16 110 por repetición (268.5/s) | 71 MB | 0 |
| STRESS | 10×10 chunks, VERY_HARD, 10 → 320 zombis | 30 s simulados por escalón | ver tabla siguiente | ver tabla siguiente | 71–72 MB | 0 |
| ENDURANCE | 10×10 chunks, VERY_HARD, oleadas reales | 30 min reales = 128 249 s simulados (35.6 h) | media de las medias por minuto 0.23 / p95 1.05–1.50 / 111 ms | 7 139 816 (55.7/s) | 71 MB en los 30 muestreos | 0 |

**STRESS por escalón.** El umbral de degradación se fijó antes de ejecutar: p95 > 16.67 ms, que es
el presupuesto de un fotograma a 60 FPS.

| Zombis | Media | p95 | Máx. | A\* (por s simulado) |
| --- | --- | --- | --- | --- |
| 10 | 0.35 ms | 1.33 ms | 23.2 ms | 2 807 (93.6) |
| 20 | 0.58 ms | 1.94 ms | 28.4 ms | 6 081 (202.7) |
| 40 | 1.29 ms | 3.74 ms | 36.0 ms | 11 093 (369.8) |
| 80 | 2.56 ms | 5.67 ms | 88.9 ms | 24 186 (806.2) |
| 160 | 3.33 ms | 6.51 ms | 40.9 ms | 45 192 (1 506.4) |
| 320 | 8.82 ms | **20.04 ms** | 138.0 ms | 91 984 (3 066.1) |

Se observó degradación con **320 zombis**: el p95 de 20 ms supera el presupuesto y el escalón de
640 ya no se ejecutó. Con 160 zombis la IA todavía cabe con margen en un fotograma.

**ENDURANCE, más detalle:**

- 4 096 oleadas completas y 49 132 zombis generados; entre 5 y 12 activos en cada muestreo.
- El jugador murió 12 301 veces: la dificultad VERY_HARD en un bucle continuo.
- El heap, medido cada minuto tras `System.gc()`, dio 71 MB en las 30 muestras: no crece con los
  ciclos spawn/despawn. Esto es heap Java con resolución de 1 MB. **No** demuestra la ausencia de
  fugas nativas o de GPU.
- El p95 por minuto se mantuvo entre 1.05 y 1.50 ms, sin tendencia al alza.

## Análisis

1. **Cuello de botella: las búsquedas A\*.** Con 25 zombis en VERY_HARD hay unas 10.7 búsquedas
   por zombi y segundo, y con 160 zombis unas 9.4. La política de repath (intervalo 1 s, distancia
   1 bloque) debería dejarlo en 1–2, según el movimiento del jugador. BASELINE (NORMAL, 3 zombis)
   da 2.6 por zombi y segundo.

   La causa más probable es la señalada en la revisión de código: `NavigationGrid.nodeAt` acepta
   pasos que `ZombieMovement.follow` rechaza. `EnemyUpdateService` descarta entonces la ruta y
   vuelve a llamar a A\* en el fotograma siguiente. El terreno generado, con relieve, árboles y
   casas, provoca ese caso mucho más que los mundos planos de los tests.

   El código es de Estudiante 2: se reporta a Jasub con este dato y no se corrige aquí. Siguiendo
   la regla de no optimizar antes de medir, lo siguiente sería medir de nuevo después de su
   corrección.
2. **Picos de máximo** de 13 a 138 ms: aparecen como valores aislados, mientras el p95 se mantiene
   bajo. Encajan con pausas del GC o del JIT y con A\* que agotan su presupuesto de 2 000
   expansiones. No se aislaron por separado.
3. **Memoria:** el mundo de 100 chunks domina el heap (71 MB frente a 4 MB del de 4 chunks). Los
   zombis y sus rutas no se notan a esta resolución.

## Limitaciones

- Una sola ejecución por escenario, en un portátil de uso normal. Los valores absolutos varían
  entre máquinas; lo comparable son las proporciones entre escenarios.
- Headless: no mide FPS, tiempo de render, memoria nativa ni GPU. El renderizado de 320 zombis no
  se probó. Las mediciones gráficas del Corte 1 están en
  [world-size-render-and-life.md](../world-size-render-and-life.md).
- El jugador sigue una ruta fija y no hay colisiones del jugador en la simulación.
- ENDURANCE simula tiempo acelerado: 30 minutos reales cubren 35.6 horas de juego sin pausas de
  render, así que no reproduce el calentamiento térmico de una sesión gráfica real.
- STRESS no promete ningún número de enemigos para el juego real: el render añade su propio coste.
