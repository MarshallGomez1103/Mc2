# LEEME — Participante 3 (Colisión contra el terreno)

## Qué se agregó
- `src/domain/player/CollisionResolver.java`: resuelve el movimiento del jugador eje por eje (X, luego Z, luego Y) contra los bloques sólidos del `World`, integrando los deltas de `PlayerMovementService` (Participante 1) y `PlayerPhysics` (Participante 2). Es la única clase que muta `player.x/y/z` directamente. Actualiza `onGround` y cancela `velocityY` cuando el movimiento vertical es bloqueado.
- `src/manualtest/CollisionManualTest.java`: construye un `World` de prueba (piso 8x8 en y=0 + una pared de 3 bloques) y verifica:
  (a) el jugador cae y se detiene sobre el piso con `onGround = true`;
  (b) el jugador no puede atravesar una pared lateral;
  (c) tras aterrizar, un salto lo separa del piso (`onGround` vuelve a `false` mientras está en el aire) y luego vuelve a aterrizar.

## Cómo correrlo
```
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out manualtest.CollisionManualTest
```
Debe imprimir las 3 verificaciones y terminar con "Prueba de colisión superada".

## Simplificaciones documentadas (ver Javadoc de la clase para más detalle)
- Resolución eje por eje, no swept-AABB continuo: si un eje colisiona, ese eje no se aplica ese frame (no hay recorte al punto exacto de contacto ni slide de pared).
- No hay manejo especial de esquinas.
- Se asume que el jugador nunca es más ancho que un par de bloques (`Player.WIDTH = 0.6`).
- No se "empuja" al jugador hasta tocar exactamente la cara del bloque al bloquear un eje; queda en su última posición no colisionante.

## No toqué
Persistencia, menú, generación de terreno, ni `Player.java` (solo lo consumo, como indica la sección 0.6 del plan). No agregué patrones de diseño nuevos ni dependencias al `pom.xml`.
