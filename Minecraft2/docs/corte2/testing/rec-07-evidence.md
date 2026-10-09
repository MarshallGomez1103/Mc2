# Evidencia de pruebas — correspondencia con la entrega

Generado a partir de una corrida real de `mvn verify` (2026-10-08), sin cifras inventadas.

## Separacion por nivel

Surefire (`mvn test`, fase `test`) ejecuta unicamente clases `*Test.java`. Failsafe
(fase `integration-test`, dentro de `mvn verify`) ejecuta unicamente clases `*IT.java`.
La exclusion esta declarada en `pom.xml` (`maven-surefire-plugin` excluye `**/*IT.java`;
`maven-failsafe-plugin` las recoge en su fase propia). Los reportes quedan en carpetas
separadas: `target/surefire-reports/` y `target/failsafe-reports/`.

### Unitarias (Surefire)

- Clases ejecutadas: 60
- Tests run: 313
- Failures: 0
- Errors: 0
- Skipped: 0
- Paquetes cubiertos: `application`, `bootstrap`, `domain.enemy`, `domain.player`,
  `domain.world`, `patterns.factory`, `persistence`, `presentation`, `presentation.game`

### Integracion (Failsafe)

- Clases ejecutadas: 1 (`presentation.GameFlowSmokeIT`)
- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 0
- Caja negra sobre el jar empaquetado real (ver `rec-06-blackbox-flow.md` para el
  detalle y el alcance declarado)

### Totales de la entrega

- Total ejecutado: 314 (313 unitarias + 1 integracion)
- Fallos: 0 / Errores: 0 / Omitidas: 0
- BUILD SUCCESS

## Clases de equivalencia y valores limite (muestra — dominio Enemy AI)

Estos casos ya existen en el codigo; aqui se documentan explicitamente como lo pide REC-07.

| Clase de prueba | Clases de equivalencia cubiertas | Valores limite |
|---|---|---|
| `AStarPathfinderTest` | camino recto, con obstaculo, con rodeo, origen = destino | destino inalcanzable (sin camino) |
| `NavigationGridTest` | celda libre, celda ocupada, borde de grilla | fuera de los limites de la grilla |
| `NavigationDiagonalTest` | movimiento ortogonal, movimiento diagonal libre, diagonal bloqueada por esquina | diagonal en el borde exacto de un obstaculo |
| `WaveRulesTest` | oleada inicial, oleada intermedia, oleada maxima configurada | limite superior de dificultad/oleada |
| `ZombieStateMachineTest` | estados idle, persiguiendo, atacando, muerto | transicion en el instante exacto del limite de vida/alcance |

## Integracion entre componentes reales (muestra)

- `EnemyUpdateServiceTest`: `EnemyUpdateService` real + `World` real + pathfinding A* real
  sobre un mundo construido con la fixture `TestWorlds` (sin dobles).
- `LocalWorldPersistenceTest`, `WorldLifecycleTest`, `SaveEdgeCasesTest`: servicio de
  aplicacion real + almacenamiento real en disco (vía `@TempDir`), sin mocks de IO.
- `GameFlowSmokeIT` (ver documento REC-06): integracion de extremo a extremo a traves
  del proceso real del jar empaquetado.

Nota de alcance: la reclasificacion formal de cuales de las 60 clases unitarias son
"integracion entre componentes reales" vs. "unitarias con fixtures de dominio" queda
pendiente de confirmacion de Thomas/Jasub antes de renombrar nada a `*IT` — aqui solo
se documentan los casos ya evidentes para no introducir reclasificaciones no acordadas.

## Responsables

- Configuracion de separacion Surefire/Failsafe: Thomas
- Caso de integracion GameFlowSmokeIT y casos propios de Enemy AI: Jasub