> Registro histórico del Corte 2. Los conteos, parámetros y métricas corresponden a la etapa descrita; no son resultados del Corte 3.

# Zombis: modelo, separación y balance — 2026-09-26

> Evidencia de la primera integración. Los ajustes posteriores y su validación están en
> [feedback-20260926.md](feedback-20260926.md); el README y el registro final de oleadas describen la última etapa del Corte 2.

## Implementado

- Modelo compartido de seis partes (cabeza, torso, dos brazos, dos piernas), frente +Z estable, orientación por desplazamiento y marcha mínima por distancia recorrida. Hitbox lógica sigue 0.6 × 1.8 × 0.6; brazos visuales pueden sobresalir de la caja.
- Atlas de 48 × 32, cuatro filas de partes y seis columnas de caras. Ojos y boca solo en cabeza frontal; torso, brazos/manos y pantalones tienen regiones independientes. `ZombieSkin` conserva su matriz histórica y paleta para no romper clientes.
- `ZombieRenderer.setTexturesEnabled(boolean)` aplica inmediatamente; instancias descartadas al despawn; modelo/textura compartidos liberados, Pixmap liberado al crear textura.
- Separación AABB local determinista en orden de registro, resolución acotada y validación del cuerpo completo contra terreno. Muertos/salud cero no bloquean vivos. No modifica FSM ni convierte A* en navegación multiagente.
- Spawn rechaza cuerpo atravesando terreno o solapando zombi vivo. HordeManager proyecta el anillo al borde siguiendo el rayo, en lugar de amontonar posiciones recortadas en esquinas.
- NORMAL velocidad 4.5 b/s, detección24, pérdida30; VERY_HARD5.2 b/s, detección28, pérdida36. Jugador caminar4.3/sprint7.095. Caps de oleadas siguen6/12. Pruebas explícitas de20 cuerpos no alteran el cap normal.
- Seguimiento consume el presupuesto exacto de distancia de cada frame entre centros de waypoints; no pierde0.2 bloques ni un frame por cada nodo. Subpasos≤0.1 validan terreno y previenen saltar paredes con delta grande.

## Reproducción y corrección de NavigationGrid vs ZombieMovement

Se agregó primero `NavigationMovementAgreementTest.lowCeilingAboveLowerHalfRejectsStepBeforePathPlanning`.
Terreno plano, escalón de1, techo a3 sobre la columna baja: ambas celdas caminables individualmente,
pero el cuerpo de0.6 ocupa ambas mientras sube y toca el techo de la columna baja.

Ejecución RED real: `mvn -q -Dtest=NavigationMovementAgreementTest test` →1test,1fallo de aserción,
0errores. Grid ofrecía la transición incompatible. Corrección: valida ambas columnas a la mayor
altura de pies en `isTransitionClear`. Además movimiento bloqueado espera `noRouteRetrySeconds`
antes de buscar otra ruta; una cola de zombis espera localmente sin generar reintento A* por frame.

## Probado automáticamente

Comando focalizado (sin clean, sin otras ejecuciones Maven simultáneas):

```sh
mvn -q -Dtest=ZombieAtlasTest,ZombieSkinTest,ZombieSeparationTest,NavigationMovementAgreementTest,ZombieMovementTest,EnemyUpdateServiceTest,DifficultyTest,HordeManagerTest test
```

52 pruebas, cero fallos/errores. Incluye pareja coincidente/determinismo,20coincidentes,
20convergentes, pared, corredor, muertos, spawn ocupado, cuerpo junto pared, UV y velocidad efectiva
4.5bloques después de60frames. Tests de navegación conservan fixture explícita original2.6/rango12
para aislar coordinación y tiempos de IA; prueba de dificultad se mueve de distancia15 a26 para
seguir comprobando el rango intermedio tras el balance, manteniendo todas sus aserciones.

Gate global, PIT y carga constan en el reporte histórico de integración y se registran por separado.

## Smoke visual OpenGL real

Harness `test/presentation/game/ZombieVisualSmoke.java` (main, no JUnit): seis vistas,
30frames por vista; captura frente, espalda, lateral, arriba, marcha y texturasOFF.

```sh
java -cp target/classes:target/test-classes:target/minecraft2-0.1.0-SNAPSHOT.jar presentation.game.ZombieVisualSmoke /ruta/a/capturas
```

Ejecutado exitosamente con contexto gráfico real: **Mesa Intel(R) Graphics (RPL-S)**.
Las capturas de estas vistas no están versionadas en el repositorio.
Frente, espalda, lateral y marcha se revisaron visualmente: cabeza al derecho,
ojos/boca únicamente de frente, brazos extendidos y piernas diferenciadas. Capturas framebuffer
usan flipY al escribir PNG; este ajuste es de captura, no de orientación del modelo.

## Limitaciones / pendiente

- Smoke automatizado con revisión de imágenes no equivale a playtest humano de diversión.
- Separación utiliza resolución local acotada, no reservas globales de rutas. En callejones sin
  espacio disponible puede quedar cola/bloqueo; no promete resolver una configuración geométricamente
  imposible.32pasadas máximas por actualización, interrupción temprana si no hay correcciones.
- IA mantiene detección por distancia sin línea de visión y navegación2.5D, sin gravedad continua.
- No se probaron AMD ni cambio de GPU dentro de este smoke.
- Validar melee/oleadas/juego y confirmar gates finales sobre estado conjunto.

## Revisión adicional: paredes como refugio

`EnemyWallAttackTest` reprodujo un golpe a través de pared: zombi y jugador separados1.6bloques,
pared sólida entre ambos;120frames mataban al jugador. RED real:1prueba,1fallo de aserción.
Se corrigió únicamente `EnemyUpdateService.attack`: una línea corta entre centros corporales
consulta bloques actuales a intervalos≤0.05; sólido o chunk ausente bloquea melee y reinicia su
cooldown. No modifica FSM ni percepción general. Retirar la pared vuelve a permitir ataque después
del cooldown completo, comprobado en el mismo test.

GREEN focalizado: `mvn -q -Dtest=EnemyWallAttackTest,EnemyUpdateServiceTest,ZombieSeparationTest test`
→17pruebas,0fallos/errores; `git diff --check` correcto. Gate global debe incluir esta corrección final.

## Revisión adicional: spawn seguro en borde

`HordeSpawnSafetyTest` recorre40seeds con jugador en x=31.5 (borde este) y comprueba que ningún
zombi aparezca a distancia planar<2.5 del jugador. RED real:1test,1fallo de aserción en seed0;
la proyección de rayos hacia fuera daba fraction0 y podía coincidir con el jugador.

Corrección pequeña: HordeManager descarta candidatos con distancia planar<2.5 después de
proyectar al borde y continúa sus intentos. Se conservan8intentos y todos los asserts existentes.
GREEN: `mvn -q -Dtest=HordeSpawnSafetyTest,HordeManagerTest test` →17pruebas,0fallos/errores;
`git diff --check` limpio. Fallar un spawn es preferible a materializarlo en el cuerpo del jugador.
