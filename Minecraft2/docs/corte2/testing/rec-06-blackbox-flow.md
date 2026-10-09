# REC-06 — flujo público de caja negra

El flujo aportado por Jasub está integrado en [GameFlowSmokeIT](../../../test/presentation/GameFlowSmokeIT.java). La integración amplió la prueba a tres casos, reabrir desde otro proceso y timeout sin lectura bloqueante de stdout.

Comando actual, desde Minecraft2/: `mvn clean verify -DskipUnitTests=true`. Package genera el JAR antes de Failsafe dentro del mismo comando. Resultado actual: 3/3 casos públicos aprobados, sin fallos/errores/omitidas, como parte de 148 pruebas de integración.

[Escenarios, aislamiento y límites](../../recuperacion-c2/integracion.md). [Reporte actual y evidencia](../../pruebas.md). Este documento no convierte la evidencia gráfica histórica en prueba del flujo actual ni exige package previo a verify.
