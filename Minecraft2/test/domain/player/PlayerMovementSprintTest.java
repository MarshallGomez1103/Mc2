package domain.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMovementSprintTest {
    @Test
    void shiftIncreasesSpeedWithoutChangingDirectionOrDiagonalNormalization() {
        Player player = new Player(0, 20, 0);
        PlayerMovementService movement = new PlayerMovementService();
        double[] walk = movement.computeIntendedDelta(player,
                new MovementInput(true, false, false, false), 1);
        double[] sprint = movement.computeIntendedDelta(player,
                new MovementInput(true, false, false, false, true), 1);
        double[] diagonal = movement.computeIntendedDelta(player,
                new MovementInput(true, false, false, true, true), 1);

        assertEquals(Player.MOVE_SPEED, Math.hypot(walk[0], walk[1]), 1e-9);
        assertEquals(Player.MOVE_SPEED * PlayerMovementService.SPRINT_MULTIPLIER,
                Math.hypot(sprint[0], sprint[1]), 1e-9);
        assertEquals(Math.hypot(sprint[0], sprint[1]), Math.hypot(diagonal[0], diagonal[1]), 1e-9);
        assertEquals(0, movement.computeIntendedDelta(player,
                new MovementInput(false, false, false, false, true), 1)[0]);
    }
}
