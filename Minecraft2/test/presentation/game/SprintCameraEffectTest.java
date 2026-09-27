package presentation.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SprintCameraEffectTest {
    @Test void offsetIsSmallSettlesWhenWalkingAndResets() {
        SprintCameraEffect effect = new SprintCameraEffect();
        for (int i = 0; i < 200; i++) {
            effect.update(1.0 / 60, true, true, true);
            assertTrue(Math.abs(effect.verticalOffset()) <= .035001);
            assertTrue(effect.fovOffset() <= 4.00001);
        }
        assertTrue(effect.fovOffset() > 3.9);
        effect.update(2, false, true, true);
        assertEquals(0, effect.fovOffset(), .0001);
        assertEquals(0, effect.verticalOffset(), .0001);
        effect.reset();
        assertEquals(0, effect.fovOffset());
        assertEquals(0, effect.verticalOffset());
    }

    @Test void noIdleBobAndPauseZeroDeltaPreservesOffsets() {
        SprintCameraEffect effect = new SprintCameraEffect();
        effect.update(1, true, false, true);
        assertEquals(0, effect.fovOffset());
        assertEquals(0, effect.verticalOffset());
        effect.update(.04, true, true, true);
        float fov = effect.fovOffset();
        float offset = effect.verticalOffset();
        effect.update(0, true, true, true);
        assertEquals(fov, effect.fovOffset());
        assertEquals(offset, effect.verticalOffset());
        assertThrows(IllegalArgumentException.class, () -> effect.update(-1, true, true, true));
    }

    @Test void airborneSprintHasFovButNoNewBob() {
        SprintCameraEffect effect = new SprintCameraEffect();
        effect.update(1, true, true, false);
        assertTrue(effect.fovOffset() > 3.9);
        assertEquals(0, effect.verticalOffset());
    }
}
