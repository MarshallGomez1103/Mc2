# Participante 2 — entrega (Física: gravedad y salto)

Este documento describe lo agregado en esta entrega, ya integrado dentro de
`Minecraft2/src/`. También integra la entrega del Participante 1 (recibida
por separado) para que el proyecto compile completo.

## Contenido nuevo (Participante 2)

- `src/domain/player/PlayerPhysics.java`
- `src/manualtest/PlayerPhysicsManualTest.java`

## Contenido integrado de Participante 1 (sin modificar)

- `src/domain/player/Player.java` (reemplaza al stub original — contrato de la sección 0.6)
- `src/domain/player/MovementInput.java`
- `src/domain/player/PlayerMovementService.java`
- `src/manualtest/PlayerMovementManualTest.java`

## Alcance respetado

- `PlayerPhysics` solo lee/escribe `velocityY` y `onGround` de `Player`. No conoce
  `World`, no hace colisión, no toca `x`/`z`.
- `computeIntendedDeltaY` **no** aplica el desplazamiento — solo lo calcula.
  Aplicar (o recortar) ese delta y decidir `onGround` es responsabilidad del
  Participante 3 (`CollisionResolver`), tal como indica el plan.
- `jump()` no hace nada si `onGround == false` (no hay doble salto en el MVP).
- No se agregaron dependencias al `pom.xml`, no se tocó `persistence`,
  `presentation`, ni la lógica de `World`/`Chunk` de Estudiante 1.
- No se creó ningún patrón de diseño adicional a Factory/Singleton/Observer.

## Decisión de diseño documentada

No se implementó velocidad terminal de caída (terminal velocity). El plan la
marca como opcional para el MVP; se omitió para mantener `PlayerPhysics`
simple y con una sola responsabilidad. Si el equipo la quiere en una
iteración futura, se puede agregar como un `Math.max(velocityY, -limite)`
dentro de `applyGravity` sin cambiar la firma pública.

## Cómo verificar

```
javac --release 17 -d out $(find src -name '*.java')
java -cp out manualtest.WorldBlockOperationsTest
java -cp out manualtest.PlayerMovementManualTest
java -cp out manualtest.PlayerPhysicsManualTest
```

Los tres compilan y pasan (verificado con JDK 21 en modo `--release 17`,
sin agregar nada al `pom.xml`).

`PlayerPhysicsManualTest` cubre exactamente lo pedido en el plan:
- Simula ~2 segundos de caída libre (tick fijo de 1/20 s) e imprime cómo
  crece `velocityY` en magnitud tick a tick.
- Verifica que `jump()` sí actúa estando en el suelo (fija `velocityY = JUMP_SPEED`,
  pone `onGround = false`) y que **no hace nada** si `onGround == false`.
- Verifica adicionalmente que `PlayerPhysics` no toca `x`/`z`, y que
  `computeIntendedDeltaY` calcula pero no aplica el delta vertical.

## Pendiente para integración (fuera de mi alcance)

- Participante 3 (`CollisionResolver`) debe consumir `computeIntendedDeltaY`
  junto con el delta horizontal de Participante 1 para mover realmente al
  jugador y fijar `onGround` según colisión real contra el mundo.
- Participante 4 (`PlayerInteractionService`) es independiente de esta pieza.
