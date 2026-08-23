package manualtest;

import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerMovementService;

/**
 * Comprobación manual (no JUnit todavía, ver docs/decisiones-compartidas.md
 * sección "Estado de build") de Player, MovementInput y PlayerMovementService.
 *
 * Verifica:
 *  1. Moverse "forward" en yaw = 0, 90, 180, 270 produce la dirección esperada.
 *  2. Moverse en diagonal (forward+right) no es más rápido que en una sola
 *     dirección (la magnitud del delta debe ser igual en ambos casos).
 *  3. look() ajusta yaw/pitch y clampea pitch a [-89, 89].
 *  4. toPosition() redondea hacia abajo (floor) correctamente, incluyendo
 *     coordenadas negativas.
 *
 * Ejecutar como una clase Java normal (tiene main); no requiere JUnit.
 */
public final class PlayerMovementManualTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testForwardAtEachYaw();
        testDiagonalIsNotFaster();
        testLookUpdatesAndClampsPitch();
        testToPositionFloorsCoordinates();
        testIdleInputProducesNoDelta();

        System.out.println();
        System.out.println("Resumen: " + passed + " pasaron, " + failed + " fallaron.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testForwardAtEachYaw() {
        System.out.println("== Forward en distintos yaw ==");
        PlayerMovementService movement = new PlayerMovementService();
        double deltaSeconds = 1.0;

        checkForwardDirection(movement, 0f, deltaSeconds, 0d, +1d, "yaw=0 (mirando +Z)");
        checkForwardDirection(movement, 90f, deltaSeconds, -1d, 0d, "yaw=90 (mirando -X)");
        checkForwardDirection(movement, 180f, deltaSeconds, 0d, -1d, "yaw=180 (mirando -Z)");
        checkForwardDirection(movement, 270f, deltaSeconds, +1d, 0d, "yaw=270 (mirando +X)");
    }

    private static void checkForwardDirection(
            PlayerMovementService movement,
            float yaw,
            double deltaSeconds,
            double expectedDxSign,
            double expectedDzSign,
            String label
    ) {
        Player player = new Player(0, 0, 0);
        player.setYaw(yaw);
        MovementInput forward = new MovementInput(true, false, false, false);

        double[] delta = movement.computeIntendedDelta(player, forward, deltaSeconds);
        double expectedMagnitude = Player.MOVE_SPEED * deltaSeconds;

        boolean directionOk = sameSign(delta[0], expectedDxSign) && sameSign(delta[1], expectedDzSign);
        boolean magnitudeOk = approximately(Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]), expectedMagnitude);

        report(label + " -> dx=" + round(delta[0]) + ", dz=" + round(delta[1]), directionOk && magnitudeOk);
    }

    private static void testDiagonalIsNotFaster() {
        System.out.println("== Movimiento diagonal no debe ser mas rapido ==");
        PlayerMovementService movement = new PlayerMovementService();
        double deltaSeconds = 1.0;

        Player straight = new Player(0, 0, 0);
        MovementInput forwardOnly = new MovementInput(true, false, false, false);
        double[] straightDelta = movement.computeIntendedDelta(straight, forwardOnly, deltaSeconds);
        double straightMagnitude = Math.sqrt(straightDelta[0] * straightDelta[0] + straightDelta[1] * straightDelta[1]);

        Player diagonal = new Player(0, 0, 0);
        MovementInput forwardAndRight = new MovementInput(true, false, false, true);
        double[] diagonalDelta = movement.computeIntendedDelta(diagonal, forwardAndRight, deltaSeconds);
        double diagonalMagnitude = Math.sqrt(diagonalDelta[0] * diagonalDelta[0] + diagonalDelta[1] * diagonalDelta[1]);

        report(
                "magnitud recta=" + round(straightMagnitude) + ", magnitud diagonal=" + round(diagonalMagnitude),
                approximately(straightMagnitude, diagonalMagnitude)
        );
    }

    private static void testLookUpdatesAndClampsPitch() {
        System.out.println("== look() actualiza yaw/pitch y clampea pitch ==");
        PlayerMovementService movement = new PlayerMovementService();
        Player player = new Player(0, 0, 0);

        movement.look(player, 10f, 5f);
        report("yaw tras +10 = " + player.getYaw(), approximately(player.getYaw(), 10f));
        report("pitch tras +5 = " + player.getPitch(), approximately(player.getPitch(), 5f));

        movement.look(player, 0f, 1000f);
        report("pitch clampeado a <= 89, valor=" + player.getPitch(), player.getPitch() <= 89f + 1e-6);
    }

    private static void testToPositionFloorsCoordinates() {
        System.out.println("== toPosition() redondea hacia abajo, incluyendo negativos ==");
        Player player = new Player(-0.5, 10.9, -0.1);
        domain.Position position = player.toPosition();

        report(
                "esperado (-1, 10, -1), obtenido (" + position.x() + ", " + position.y() + ", " + position.z() + ")",
                position.x() == -1 && position.y() == 10 && position.z() == -1
        );
    }

    private static void testIdleInputProducesNoDelta() {
        System.out.println("== Input sin direccion no produce delta ==");
        PlayerMovementService movement = new PlayerMovementService();
        Player player = new Player(0, 0, 0);
        MovementInput idle = new MovementInput(false, false, false, false);

        double[] delta = movement.computeIntendedDelta(player, idle, 1.0);
        report("delta=" + round(delta[0]) + "," + round(delta[1]), delta[0] == 0d && delta[1] == 0d);
    }

    private static boolean sameSign(double value, double expectedSign) {
        if (expectedSign == 0d) {
            return approximately(value, 0d);
        }
        return (value > 0) == (expectedSign > 0);
    }

    private static boolean approximately(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    private static void report(String description, boolean ok) {
        String tag = ok ? "PASA" : "FALLA";
        System.out.println("  [" + tag + "] " + description);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
