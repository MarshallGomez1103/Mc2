package domain.player;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaminaTest {
    @Test void drainsByElapsedTimeIndependentOfFps() {
        for (int fps : new int[] {30, 60, 144}) {
            Stamina stamina = new Stamina();
            for (int i = 0; i < 3 * fps; i++) stamina.update(1.0 / fps, true);
            assertEquals(70, stamina.current(), 1e-8);
            for (int i = 0; i < 7 * fps; i++) stamina.update(1.0 / fps, true);
            assertEquals(0, stamina.current(), 1e-8);
            assertTrue(stamina.exhausted());
            assertFalse(stamina.canSprint());
        }
    }

    @Test void exhaustionLocksSprintUntilTwentyPercentThenResetIsFull() {
        Stamina stamina = new Stamina();
        stamina.update(10, true);
        stamina.update(.5, false);
        assertEquals(10, stamina.current());
        assertTrue(stamina.exhausted());
        assertEquals(0, stamina.sprintSeconds(1));
        stamina.update(.5, false);
        assertEquals(20, stamina.current(), 1e-9);
        assertTrue(stamina.canSprint());
        stamina.reset();
        assertEquals(100, stamina.current());
        assertFalse(stamina.exhausted());
    }

    @Test void stationaryRegeneratesAndDoesNotExceedMaximum() {
        Stamina stamina = new Stamina();
        stamina.update(1, true);
        stamina.update(1, false);
        assertEquals(100, stamina.current());
        stamina.update(100, false);
        assertEquals(100, stamina.current());
    }

    @Test void zeroDeltaPreservesStateAndInvalidTimesAreRejected() {
        Stamina stamina = new Stamina();
        stamina.update(1, true);
        stamina.update(0, false);
        assertEquals(90, stamina.current());
        assertThrows(IllegalArgumentException.class, () -> stamina.update(-1, false));
        assertThrows(IllegalArgumentException.class, () -> stamina.update(Double.NaN, false));
        assertThrows(IllegalArgumentException.class, () -> stamina.sprintSeconds(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> stamina.advance(1, 2));
        assertThrows(IllegalArgumentException.class, () -> stamina.advance(1, -1));
    }

    @Test void finalSprintStepUsesOnlyAvailableEnergy() {
        Stamina stamina = new Stamina();
        stamina.update(9.99, true);
        assertEquals(.01, stamina.sprintSeconds(.05), 1e-9);
        stamina.advance(.05, stamina.sprintSeconds(.05));
        assertEquals(0, stamina.current());
        assertTrue(stamina.exhausted());
    }
}
