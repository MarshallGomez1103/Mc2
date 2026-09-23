# TDD — ZombieStateMachine

Estado: **RED → GREEN → REFACTOR ejecutados** el 2026-09-21 sobre la revisión `6202362` (master).
Owner de evidencia: Estudiante 2 (Jasub). Sin LibGDX, archivos, reloj real ni pathfinding.

## Reglas fijadas antes de RED

| Regla | Decisión |
| --- | --- |
| Entradas | `ZombieState current` + `ZombiePerception(health, distanceToPlayer, playerAlive)` |
| Salida | nuevo `ZombieState`; la FSM no muta al zombi ni ejecuta A* |
| Prioridad | 1) `health <= 0` o ya DEAD → DEAD; 2) jugador muerto → IDLE; 3) distancia |
| Límites | inclusivos al entrar en un rango (`<=`) |
| Histéresis | `attackRange <= detectionRange <= loseTargetRange`; CHASE se abandona solo al superar `loseTargetRange` |
| IDLE adyacente | si el jugador ya está a distancia de ataque, IDLE pasa directo a ATTACK |
| Valores inválidos | rangos desordenados o distancia negativa/NaN → `IllegalArgumentException` |
| Pruebas | detección 10, ataque 1.5, pérdida 14 (los valores reales de sesión viven en `ZombieParameters`) |

## RED

Se escribió `test/domain/enemy/ZombieStateMachineTest.java` (13 casos) y una firma mínima de
`ZombieStateMachine.next()` que **devolvía siempre el estado actual** y no validaba rangos, para
que el fallo fuera de aserción y no de compilación.

Comando: `mvn -q -Dtest=ZombieStateMachineTest -Dsurefire.failIfNoSpecifiedTests=false test`

```text
Tests run: 13, Failures: 8, Errors: 0, Skipped: 0 <<< FAILURE! -- in domain.enemy.ZombieStateMachineTest
  anyLivingStateShouldBecomeDeadWhenHealthIsZeroOrBelow:80  expected: <DEAD>  but was: <IDLE>
  attackShouldReturnToChaseWhenPlayerMovesAway:61           expected: <CHASE> but was: <ATTACK>
  chaseShouldChangeToAttackInsideAttackRangeInclusive:43    expected: <ATTACK> but was: <CHASE>
  chaseShouldReturnToIdleWhenTargetIsLost:56                expected: <IDLE>  but was: <CHASE>
  deadPlayerMeansTargetLostFromAnyLivingState:74            expected: <IDLE>  but was: <CHASE>
  idleShouldChangeToChaseWhenPlayerDetectedInclusiveLimit:32 expected: <CHASE> but was: <IDLE>
  idleShouldJumpDirectlyToAttackIfPlayerAlreadyAdjacent:38  expected: <ATTACK> but was: <IDLE>
  rangesMustBeOrderedAndPositive:93  Expected IllegalArgumentException to be thrown, but nothing was thrown.
```

Los 5 casos que pasaron en RED lo hicieron porque el stub los satisface trivialmente
(IDLE sin objetivo sigue IDLE, DEAD permanece DEAD, ATTACK en rango sigue ATTACK, CHASE entre
rangos sigue CHASE y la validación de `ZombiePerception`, que sí estaba implementada en el record).

## GREEN

Se implementaron las transiciones con un `switch` de ternarios anidados y la validación de rangos.
Mismo comando:

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.232 s -- in domain.enemy.ZombieStateMachineTest
```

`mvn -q test` completo tras GREEN (con `ZombieTest` ya añadido): **80 pruebas, 0 fallos**; las 63
del baseline se conservan.

## REFACTOR

Se sustituyeron los ternarios anidados por tres métodos privados con nombre (`fromIdle`,
`fromChase`, `fromAttack`). Los tests no se tocaron.

```diff
-            case IDLE -> distance <= attackRange ? ZombieState.ATTACK
-                    : distance <= detectionRange ? ZombieState.CHASE
-                    : ZombieState.IDLE;
-            case CHASE -> distance <= attackRange ? ZombieState.ATTACK
-                    : distance <= loseTargetRange ? ZombieState.CHASE
-                    : ZombieState.IDLE;
-            case ATTACK -> distance <= attackRange ? ZombieState.ATTACK : ZombieState.CHASE;
+            case IDLE -> fromIdle(distance);
+            case CHASE -> fromChase(distance);
+            case ATTACK -> fromAttack(distance);
             case DEAD -> ZombieState.DEAD;
         };
     }
+
+    private ZombieState fromIdle(double distance) {
+        if (distance <= attackRange) {
+            return ZombieState.ATTACK;
+        }
+        return distance <= detectionRange ? ZombieState.CHASE : ZombieState.IDLE;
+    }
+
+    private ZombieState fromChase(double distance) {
+        if (distance <= attackRange) {
+            return ZombieState.ATTACK;
+        }
+        return distance <= loseTargetRange ? ZombieState.CHASE : ZombieState.IDLE;
+    }
+
+    private ZombieState fromAttack(double distance) {
+        return distance <= attackRange ? ZombieState.ATTACK : ZombieState.CHASE;
+    }
```

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.065 s -- in domain.enemy.ZombieStateMachineTest
```

## Registro de evidencias

| Etapa | Fecha/revisión | Comando | Resultado real |
| --- | --- | --- | --- |
| RED | 2026-09-21 / 6202362 | `mvn -Dtest=ZombieStateMachineTest test` | 13 ejecutadas, 8 fallos de aserción, 0 errores |
| GREEN | 2026-09-21 / 6202362 + working tree | mismo comando | 13 ejecutadas, 0 fallos |
| REFACTOR | 2026-09-21 / 6202362 + working tree | mismo comando y `mvn -q test` | 13/13 y suite completa 80/80 |

Al cierre del frente la suite completa es **110 pruebas, 0 fallos, 0 errores** (`mvn clean package`).
