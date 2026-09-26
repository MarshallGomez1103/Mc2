# Mutation testing — C2-09

Estado: PIT configurado en `pom.xml` y ejecutado. Owner: Estudiante 3.
Objetivo aproximado 80 % sobre un scope pequeño de lógica pura, no 80 % global del juego.

## Configuración

- PIT `pitest-maven` 1.30.0 + `pitest-junit5-plugin` 1.2.3 (JUnit 5.10.2).
- No está atado a ninguna fase de Maven: `mvn clean test` no lo ejecuta ni se vuelve más lento.
- `sourceDirectory=src` y `testSourceDirectory=test` funcionan sin configuración extra.
- Mutadores por defecto de PIT (DEFAULTS). Reporte HTML y CSV en `target/pit-reports/`,
  sin carpetas con fecha para que la ruta sea estable.

Comando reproducible, desde `Minecraft2/`:

```text
mvn clean test
mvn org.pitest:pitest-maven:mutationCoverage
```

(`mvn test-compile org.pitest:pitest-maven:mutationCoverage` hace lo mismo sin pasar por el gate.)

## Scope y justificación

| Clase | Owner | Por qué entra |
| --- | --- | --- |
| `domain.enemy.ZombieStateMachine` | Estudiante 2 | FSM pura, sin LibGDX; TDD propio |
| `domain.enemy.AStarPathfinder` | Estudiante 2 | Búsqueda pura sobre `NavigationGrid` |
| `domain.world.biome.BiomeResolver` | Estudiante 1 | Función pura seed + coordenadas → bioma |
| `domain.enemy.WaveRules` | Estudiante 3 | Reglas de oleadas y su validación |
| `domain.enemy.Difficulty` | Estudiante 3 | Paquetes de parámetros por nivel |
| `application.HordeManager` | Estudiante 3 | Oleadas deterministas con tiempo explícito |
| `application.GameSettings` | Estudiante 3 | Configuración de sesión |

Tests que PIT ejecuta: `domain.*` y `application.*`. Excluidos a propósito:

- `VoxelGame`, `GameWindow`, renderers y atlas: necesitan OpenGL;
- `bootstrap`;
- persistencia: su comportamiento ya se cubre con CT-09, que tiene 26 documentos inválidos;
- enums de vocabulario: no se mutan para inflar el resultado.

## Resultado real

Ejecución del 2026-09-25:

- rama `feature/c2-Ethian` sobre el commit `0de00c7` con los cambios de Estudiante 3 sin commitear;
- JDK Temurin 17.0.20.1, Maven 3.9.9;
- AMD Ryzen 5 4500U, 7.4 GB de RAM, Windows 11 Pro;
- duración: 34 s de PIT (análisis de mutantes 30 s); 72 tests examinados; 733 ejecuciones de test.

| Scope | Generated | Killed | Timed out | Survived | No coverage | Score |
| --- | --- | --- | --- | --- | --- | --- |
| Total | 172 | 153 | 1 | 17 | 1 | **89.5 %** |
| HordeManager | 63 | 57 | 1 | 5 | 0 | 92.1 % |
| GameSettings | 14 | 14 | 0 | 0 | 0 | 100 % |
| WaveRules | 22 | 22 | 0 | 0 | 0 | 100 % |
| Difficulty | 5 | 5 | 0 | 0 | 0 | 100 % |
| ZombieStateMachine | 29 | 26 | 0 | 3 | 0 | 89.7 % |
| AStarPathfinder | 31 | 23 | 0 | 7 | 1 | 74.2 % |
| BiomeResolver | 8 | 6 | 0 | 2 | 0 | 75.0 % |

Cómo se calcula el score:

- score = (killed + timed out) / generated × 100. Es el mismo criterio que usa PIT, que cuenta el
  timeout como mutante detectado.
- Sin contar el timeout: 153 / 172 = 88.95 %.
- Line coverage de las clases mutadas: 213/216 (99 %). No es lo mismo que el mutation score y no
  se usa como tal.

## Análisis de supervivientes

**Primera ejecución: 81 %.** Sobrevivían 19 mutantes en `HordeManager` y 1 en `GameSettings`.

- Se añadieron tests de comportamiento, sin cambiar el scope:
  - apariciones en los cuatro cuadrantes y a lo largo de todo el anillo;
  - zombis de una oleada, y de oleadas seguidas, en sitios distintos;
  - recorte a los cuatro márgenes de un mundo en chunks negativos;
  - delta 0 que no avanza la horda;
  - aserción explícita de que la textura de enemigos está apagada tras cambiarla.
- Un mutante señaló código muerto: `waveZombies.clear()` en `startNextWave` nunca tenía efecto,
  porque una oleada solo termina con el conjunto vacío. Se eliminó la línea.

**Supervivientes de Estudiante 3 (5), considerados equivalentes o de valor bajo:**

- `HordeManager` líneas 140 y 141 (`player ± cos/sin · d`): la mutación refleja el anillo
  alrededor del jugador. La distribución de apariciones es la misma y es equivalente en conducta.
- Línea 135 (`spawnIndex++`) y línea 150 (una de las operaciones de mezcla de `spawnSeed`): cambian
  la secuencia pseudoaleatoria, pero siguen dando semillas distintas por zombi y por oleada. Los
  tests verifican propiedades como el determinismo, el anillo y la unicidad, no una secuencia
  concreta.
- Línea 137 (`attempt < 8` → `<= 8`): hace un intento extra de aparición. Solo se observaría con
  un contador de llamadas a `spawnAt`; no cambia ningún resultado visible.

**Supervivientes de otros owners (12 + 1 sin cobertura).** Se reportan y no se editan sus tests:

- `ZombieStateMachine`, Jasub:
  - línea 19: límites `<` y `>` de la validación del constructor; falta un test con
    `attackRange == detectionRange`;
  - línea 52: `distance <= attackRange` desde IDLE; falta el caso exacto en el límite.
- `AStarPathfinder`, Jasub:
  - línea 36: límite del presupuesto en el constructor;
  - línea 76: `lastExpansions++`, relacionado con el contador desfasado en uno que se vio en la
    revisión;
  - líneas 87 y 89: el coste de subida `CLIMB_COST` y la comparación `tentativeG < knownG`; ningún
    test distingue una ruta por el coste de desnivel;
  - `getTotalExpansions` no tiene cobertura.
- `BiomeResolver`, Thomas, líneas 17 y 20: umbrales `< 0.35` y `> 0.65`; ningún test cae
  exactamente en el umbral.

## Limitaciones

- Una sola ejecución: los timeouts pueden variar entre máquinas.
- No se usó el modo incremental ni el historial de PIT.
- El scope excluye la presentación. Un score alto aquí no dice nada del renderizado ni del
  menú gráfico.
