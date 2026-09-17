# TDD — ZombieStateMachine

Estado: protocolo preparado. RED/GREEN/REFACTOR todavía NO ejecutados.
Owner de evidencia: Estudiante 2, coordinado con Estudiante 3.
ZombieState existe como enum; ZombieStateMachine no existe todavía.

## Problema y alcance

Probar transiciones de intención del zombie como lógica pura determinista,
sin LibGDX, archivos, reloj real o pathfinding. Crear pruebas antes de la conducta.
No reconstruir una narrativa TDD sobre código ya implementado.

## RED — pendiente

Primero acordar entradas de la FSM y escribir ZombieStateMachineTest bajo test/domain/enemy/.
Casos iniciales propuestos:

- idleShouldRemainIdleWithoutTarget
- idleShouldChangeToChaseWhenPlayerDetected
- chaseShouldChangeToAttackInsideAttackRange
- attackShouldReturnToChaseWhenPlayerMovesAway
- chaseShouldReturnToIdleWhenTargetIsLost
- anyLivingStateShouldBecomeDeadWhenHealthIsZero
- deadShouldRemainDead

Añadir límites y prioridad de muerte según reglas pactadas.
Si es necesario crear firma mínima para compilar, no implementar las transiciones:
preferir fallo de aserción intencional a solo error de compilación.
Registrar comando, fecha, revisión y salida exacta; fallo por dependencia/entorno
no demuestra el comportamiento RED. Comando previsto: mvn -Dtest=ZombieStateMachineTest test.
La excepción RED es local y deliberada; no integrar una suite rota.

## GREEN — pendiente

Implementar el mínimo para cumplir las pruebas; registrar mismo comando y mvn clean test.
Incluir conteos exactos. Preservar 41 casos existentes.

## REFACTOR — pendiente

Simplificar nombres/duplicación sin cambiar reglas ni tests para esconder errores.
Registrar diff y ejecución verde antes/después.

## Decisiones pendientes

Entradas, distancias inclusivas, prioridad de DEAD, pérdida del objetivo,
temporización y política frente a valores inválidos.

## Registro de evidencias futuras

| Etapa | Fecha/revisión | Comando | Resultado real | Evidencia |
| --- | --- | --- | --- | --- |
| RED | Pendiente | Pendiente | No ejecutado | Pendiente |
| GREEN | Pendiente | Pendiente | No ejecutado | Pendiente |
| REFACTOR | Pendiente | Pendiente | No ejecutado | Pendiente |
