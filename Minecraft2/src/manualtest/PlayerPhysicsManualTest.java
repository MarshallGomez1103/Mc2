package manualtest;

import domain.player.Player;
import domain.player.PlayerPhysics;

/**
 * Comprobación manual y simple de la física vertical (gravedad y salto).
 *
 * No hay JUnit configurado todavía (ver sección 0.8 del plan distribuido),
 * así que esta clase sigue el mismo patrón que {@link WorldBlockOperationsTest}:
 * un {@code main()} ejecutable directamente que imprime resultados y lanza
 * {@link AssertionError} si algo falla.
 */
public final class PlayerPhysicsManualTest {

    private static final double DELTA_SECONDS = 1.0 / 20.0; // tick fijo de 20 Hz, como Minecraft real
    private static final int TICKS_FOR_TWO_SECONDS = (int) Math.round(2.0 / DELTA_SECONDS);

    private PlayerPhysicsManualTest() {
    }

    public static void main(String[] args) {
        testFreeFallAccumulatesVelocity();
        testJumpWorksOnlyOnGround();
        testGravityDoesNotTouchXOrZ();
        testIntendedDeltaYMatchesVelocityTimesDelta();

        System.out.println("Prueba de física del jugador superada: gravedad y salto.");
    }

    /** Simula ~2 segundos de caída libre e imprime cómo crece velocityY en magnitud. */
    private static void testFreeFallAccumulatesVelocity() {
        Player player = new Player(0, 50, 0);
        PlayerPhysics physics = new PlayerPhysics();

        System.out.println("--- Caída libre (~2s, tick = " + DELTA_SECONDS + "s) ---");
        System.out.printf("t=0.00s velocityY=%.4f%n", player.getVelocityY());

        double previousVelocity = player.getVelocityY();
        for (int tick = 1; tick <= TICKS_FOR_TWO_SECONDS; tick++) {
            physics.applyGravity(player, DELTA_SECONDS);

            verify(player.getVelocityY() < previousVelocity,
                    "velocityY debe volverse más negativa en cada tick de caída libre");
            previousVelocity = player.getVelocityY();

            if (tick % 4 == 0 || tick == TICKS_FOR_TWO_SECONDS) {
                double elapsed = tick * DELTA_SECONDS;
                System.out.printf("t=%.2fs velocityY=%.4f%n", elapsed, player.getVelocityY());
            }
        }

        double expectedVelocity = Player.GRAVITY * TICKS_FOR_TWO_SECONDS * DELTA_SECONDS;
        verify(Math.abs(player.getVelocityY() - expectedVelocity) < 1e-9,
                "velocityY final no coincide con GRAVITY * tiempo total acumulado por tick");
        verify(player.getVelocityY() < Player.GRAVITY,
                "tras ~2s de caída libre, la velocidad debe ser más negativa que la aceleración de un solo segundo");

        System.out.println("OK: velocityY crece en magnitud (más negativa) en cada tick de caída libre.");
    }

    /** jump() debe funcionar en el suelo y no hacer nada en el aire (sin doble salto). */
    private static void testJumpWorksOnlyOnGround() {
        System.out.println("--- Salto ---");
        PlayerPhysics physics = new PlayerPhysics();

        // Caso A: en el suelo -> el salto sí se aplica.
        Player grounded = new Player(0, 10, 0);
        grounded.setOnGround(true);
        grounded.setVelocityY(0);
        physics.jump(grounded);
        verify(grounded.getVelocityY() == Player.JUMP_SPEED,
                "jump() en el suelo debe fijar velocityY a JUMP_SPEED");
        verify(!grounded.isOnGround(),
                "jump() en el suelo debe poner onGround en false");
        System.out.println("OK: jump() en el suelo fija velocityY=" + grounded.getVelocityY()
                + " y onGround=" + grounded.isOnGround());

        // Caso B: en el aire -> jump() no hace nada (no hay doble salto).
        Player airborne = new Player(0, 10, 0);
        airborne.setOnGround(false);
        airborne.setVelocityY(-3.5);
        physics.jump(airborne);
        verify(airborne.getVelocityY() == -3.5,
                "jump() en el aire NO debe modificar velocityY (no hay doble salto)");
        verify(!airborne.isOnGround(),
                "jump() en el aire no debe cambiar onGround");
        System.out.println("OK: jump() en el aire no hace nada (velocityY se mantiene en "
                + airborne.getVelocityY() + ").");
    }

    /** applyGravity/jump solo tocan el estado vertical; x/z quedan intactos. */
    private static void testGravityDoesNotTouchXOrZ() {
        Player player = new Player(5.25, 30, -7.75);
        PlayerPhysics physics = new PlayerPhysics();

        physics.applyGravity(player, DELTA_SECONDS);
        player.setOnGround(true);
        physics.jump(player);

        verify(player.getX() == 5.25, "PlayerPhysics no debe modificar X");
        verify(player.getZ() == -7.75, "PlayerPhysics no debe modificar Z");
        System.out.println("OK: PlayerPhysics no toca X/Z, solo el estado vertical.");
    }

    /** computeIntendedDeltaY debe ser solo un cálculo (no debe mover a Y) y coincidir con velocityY * deltaSeconds. */
    private static void testIntendedDeltaYMatchesVelocityTimesDelta() {
        Player player = new Player(0, 40, 0);
        PlayerPhysics physics = new PlayerPhysics();
        player.setVelocityY(-6.0);
        double yBefore = player.getY();

        double intendedDeltaY = physics.computeIntendedDeltaY(player, DELTA_SECONDS);

        verify(Math.abs(intendedDeltaY - (-6.0 * DELTA_SECONDS)) < 1e-9,
                "computeIntendedDeltaY debe ser velocityY * deltaSeconds");
        verify(player.getY() == yBefore,
                "computeIntendedDeltaY NO debe aplicar el delta; eso lo hace CollisionResolver (Participante 3)");
        System.out.println("OK: computeIntendedDeltaY=" + intendedDeltaY + " y no muta Y (queda en " + player.getY() + ").");
    }

    private static void verify(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
