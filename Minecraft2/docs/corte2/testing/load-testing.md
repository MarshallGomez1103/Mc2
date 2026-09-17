# Load/performance testing — preparación

Estado: harness y escenarios NO construidos ni ejecutados.
Owner: Estudiante 3. Depende de EnemyUpdateService + A* integrados.

## Objetivo

Medir antes de optimizar. Harness reproducible con seed, settings, cantidad de chunks
y enemigos, delta simulado, duración, warm-up y repeticiones explícitos.
Separar tiempo simulado de wall-clock y no confundir unit tests con carga.

## Métricas

Tiempo de generación de terreno; AI update y percentiles; número/tiempo de llamadas A*;
enemigos activos; memoria heap aproximada y errores.
Frame time/FPS solo con ejecución gráfica separada; update headless no equivale a FPS.
Registrar memoria nativa/GPU como no medida si solo se mide heap.

## Escenarios futuros

| Escenario | Objetivo |
| --- | --- |
| BASELINE | 4 chunks, pocos zombies, NORMAL; carga de referencia |
| PEAK | Más enemigos y repaths simultáneos razonables |
| STRESS | Aumento escalonado hasta degradación observada, sin prometer 100 enemigos |
| ENDURANCE/RESISTANCE | Spawn/update/pathfinding/death/despawn/oleadas prolongados |

Definir duración real de endurance y umbral de degradación antes de ejecutar.
Mantener funcionamiento real en lazo, no solo repetir A* aislado.
Enemies OFF puede servir de control; no debe actualizar/spawnear enemigos.

## TODO reproducibilidad

- [ ] Harness headless fuera del gate unitario normal, con argumentos documentados.
- [ ] Contadores A*, medición de update y memoria aproximada.
- [ ] Warm-up, repeticiones, seeds y límites de tiempo.
- [ ] Registrar CPU, RAM, SO, JVM/flags y commit exactos.
- [ ] Registrar VSync/FPS cap para experimentos gráficos.
- [ ] Comparar baseline/peak/stress/endurance y describir errores/cuello de botella.
- [ ] No optimizar antes de medir ni presentar una ejecución breve como resistencia.

## Resultados futuros

| Escenario | Enemigos/chunks | Duración | A* calls/ms | AI update ms/p95 | Heap | Errores |
| --- | --- | --- | --- | --- | --- | --- |
| BASELINE | Pendiente | N/D | N/D | N/D | N/D | N/D |
| PEAK | Pendiente | N/D | N/D | N/D | N/D | N/D |
| STRESS | Pendiente | N/D | N/D | N/D | N/D | N/D |
| ENDURANCE/RESISTANCE | Pendiente | N/D | N/D | N/D | N/D | N/D |

Las mediciones gráficas históricas del Corte 1 no son resultados de carga del Corte 2.
