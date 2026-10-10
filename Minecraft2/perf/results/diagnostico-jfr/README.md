# Diagnóstico con JFR — cuello de botella de la IA

Perfilado de CPU del escenario `REC_STRESS` con Java Flight Recorder, el perfilador incluido en el JDK. Es un **diagnóstico**: el perfilador añade coste, así que los tiempos de estas corridas no se usan como resultado ni para el SLO. Los resultados oficiales son las corridas sin perfilador de [carga.md §6](../../../docs/recuperacion-c2/carga.md#6-resultados).

## Cómo se obtuvo

Mismo equipo, JDK (Temurin 17.0.20.1), heap y escenario que las corridas medidas. Desde `Minecraft2/`, después de `mvn -q test-compile`:

```bash
java -Xms1g -Xmx1g -XX:StartFlightRecording=filename=stress.jfr,settings=profile \
  -cp "target/classes:target/test-classes" loadtest.EnemyLoadHarness REC_STRESS 20
jfr print --events jdk.ExecutionSample --stack-depth 64 stress.jfr > samples.txt
gawk -v from=HH:MM:SS -v to=HH:MM:SS -f perf/results/diagnostico-jfr/atribucion.awk samples.txt
```

En Windows el separador del classpath es `;`. La ventana `[from, to)` se toma de las horas en que la consola del harness informa el final de cada nivel. [`atribucion.awk`](atribucion.awk) clasifica cada muestra tomada dentro de `EnemyUpdateService.update` según los métodos de su pila. Una muestra que pasa por A\* cuenta como A\* aunque su hoja esté en `World` o `Chunk`. Los archivos `.jfr` (6–16 MB) no se versionan.

| Corrida | Commit | Hora (−05:00) | Muestras en `update` |
| --- | --- | --- | --- |
| Antes de la corrección | `d180eb9` | 10-10-2026, 00:31–00:33 | 2 167 en el tramo de 160 zombis |
| Después de la corrección | `c2e03e7` | 10-10-2026, 00:38–00:40 | 858 en el tramo de 160; 2 906 en el de 320 |

## Resultado

Porcentaje de muestras dentro de `EnemyUpdateService.update`, medido de forma inclusiva:

| Componente | Antes, 160 zombis | Después, 160 zombis | Después, 320 zombis |
| --- | --- | --- | --- |
| A\* (`AStarPathfinder.findPath`) | 91,7 % | 83,4 % | 75,0 % |
| `World.findChunk` | 51,0 % | 5,8 % | 3,9 % |
| Cubetas en árbol de `HashMap` (`TreeNode`, `comparableClassFor`) | 43,1 % | 1,2 % | 0,9 % |
| `Chunk.getBlock` | 22,3 % | 42,8 % | 39,4 % |
| `ZombieSeparation.resolve` + `isOccupied` | 4,5 % | 11,8 % | 20,9 % |
| `ZombieMovement`, sin A\* | 2,1 % | 3,3 % | 2,3 % |

Antes de la corrección, la mitad del tiempo de IA se iba en localizar el chunk de cada celda que A\* consultaba. La clave del índice tenía `hashCode = x ^ z`: con 100 chunks solo había 16 hashes distintos, y el `HashMap` degradaba a árboles. Tras la corrección (`World.ChunkKey`), el coste dominante pasa a ser leer los bloques de cada celda y, con 320 zombis, la separación cuadrática entre zombis. El análisis completo está en [carga.md §7](../../../docs/recuperacion-c2/carga.md#7-análisis-rec-e9).
