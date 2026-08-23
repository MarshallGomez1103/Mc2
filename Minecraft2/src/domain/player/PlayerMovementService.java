package domain.player;

/**
 * Calcula el desplazamiento horizontal deseado a partir del input y la
 * orientación del jugador, y actualiza yaw/pitch al "mirar alrededor".
 *
 * Esta clase NUNCA muta x/y/z del jugador ni consulta World: el resultado
 * de {@link #computeIntendedDelta} es un delta propuesto que el
 * CollisionResolver (Participante 3) decide si aplica, recorta o descarta.
 */
public final class PlayerMovementService {

    /**
     * Calcula el desplazamiento horizontal deseado para este frame, combinando
     * forward/backward/left/right según la orientación actual del jugador.
     * El movimiento diagonal se normaliza para no ser más rápido que el
     * movimiento en una sola dirección.
     *
     * @return {dx, dz}
     */
    public double[] computeIntendedDelta(Player player, MovementInput input, double deltaSeconds) {
        if (input.isIdle()) {
            return new double[] {0d, 0d};
        }

        double[] forward = player.forwardVector();
        double[] right = player.rightVector();

        double forwardAxis = (input.isForward() ? 1d : 0d) - (input.isBackward() ? 1d : 0d);
        double rightAxis = (input.isRight() ? 1d : 0d) - (input.isLeft() ? 1d : 0d);

        double dx = forward[0] * forwardAxis + right[0] * rightAxis;
        double dz = forward[1] * forwardAxis + right[1] * rightAxis;

        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude < 1e-9) {
            return new double[] {0d, 0d};
        }

        double scale = (Player.MOVE_SPEED * deltaSeconds) / magnitude;
        return new double[] {dx * scale, dz * scale};
    }

    /**
     * Actualiza yaw/pitch del jugador según el desplazamiento del mouse
     * (mirar alrededor). El clamping de pitch y la normalización de yaw
     * los hace Player internamente en sus setters.
     */
    public void look(Player player, float deltaYaw, float deltaPitch) {
        player.setYaw(player.getYaw() + deltaYaw);
        player.setPitch(player.getPitch() + deltaPitch);
    }
}
