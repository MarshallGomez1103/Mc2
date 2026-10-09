# Evidencia de pruebas — correspondencia con la entrega

Generado a partir de una corrida real de `mvn verify` (2026-10-08), sin cifras inventadas.

## Separacion por nivel

Surefire (`mvn test`, fase `test`) ejecuta unicamente clases `*Test.java`. Failsafe
(fase `integration-test`, dentro de `mvn verify`) ejecuta unicamente clases `*IT.java`.
La exclusion esta declarada en `pom.xml` (`maven-surefire-plugin` excluye `**/*IT.java`;
`maven-failsafe-plugin` las recoge en su fase propia). Los reportes quedan en carpetas
separadas: `target/surefire-reports/` y `target/failsafe-reports/`.

Tras acuerdo con Thomas, se reclasificaron 4 clases que ya probaban componentes reales
integrados (sin dobles) y estaban mal ubicadas bajo el sufijo `*Test`: se renombraron a
`*IT` para que el reporte las cuente como integracion y no como unitarias.

| Clase | Sufijo anterior | Sufijo nuevo | Tests |
|---|---|---|---|
| `EnemyUpdateServiceTest` -> `EnemyUpdateServiceIT` | Test | IT | 11 |
| `LocalWorldPersistenceTest` -> `LocalWorldPersistenceIT` | Test | IT | 3 |
| `SaveEdgeCasesTest` -> `SaveEdgeCasesIT` | Test | IT | 3 |
| `WorldLifecycleTest` -> `WorldLifecycleIT` | Test | IT | 1 |

Total reclasificado: 18 tests, de unitarias a integracion.

### Unitarias (Surefire)

- Clases ejecutadas: 56 (60 - 4 reclasificadas)
- Tests run: 295 (313 - 18 reclasificados)
- Failures: 0
- Errors: 0
- Skipped: 0
- Paquetes cubiertos: `application`, `bootstrap`, `domain.enemy`, `domain.player`,
  `domain.world`, `patterns.factory`, `persistence`, `presentation`, `presentation.game`

### Integracion (Failsafe)

- Clases ejecutadas: 5 (`presentation.GameFlowSmokeIT` + las 4 reclasificadas)
- Tests run: 19 (1 + 18 reclasificados)
- Failures: 0
- Errors: 0
- Skipped: 0
- Caja negra sobre el jar empaquetado real: `GameFlowSmokeIT` (ver `rec-06-blackbox-flow.md`
  para el detalle y el alcance declarado); el resto integra componentes reales de
  aplicacion + dominio/persistencia sin mocks

### Totales de la entrega

- Total ejecutado: 314 (295 unitarias + 19 integracion)
- Fallos: 0 / Errores: 0 / Omitidas: 0
- BUILD SUCCESS

Confirmado con una segunda corrida real de `mvn verify` tras el renombrado
(2026-10-08): Surefire reporto 295 pruebas (0 fallos) y Failsafe reporto 19
(0 fallos), con `EnemyUpdateServiceIT`, `LocalWorldPersistenceIT`, `SaveEdgeCasesIT`
y `WorldLifecycleIT` apareciendo correctamente bajo
`--- failsafe:3.2.5:integration-test ---` junto a `GameFlowSmokeIT`. Resultado:
BUILD SUCCESS, 314 pruebas totales, 0 fallos, 0 errores, 0 omitidas.

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

Nota de alcance: se revisaron las 60 clases unitarias originales y se identificaron y
reclasificaron las 4 que integraban componentes reales sin dobles. El resto conserva
el sufijo `Test` por tratarse de pruebas unitarias aisladas (una sola unidad bajo
prueba, con fixtures de dominio o dobles donde aplica).

## Responsables

- Configuracion de separacion Surefire/Failsafe: Thomas
- Caso de integracion GameFlowSmokeIT y casos propios de Enemy AI: Jasub