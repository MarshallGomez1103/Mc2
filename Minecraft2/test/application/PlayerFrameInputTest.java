package application;

import domain.block.BlockType;
import domain.player.MovementInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerFrameInputTest {
    @Test void neutralCarriesNoActionsAndKeepsTheCurrentMaterial() {
        var input = PlayerFrameInput.neutral();
        assertTrue(input.movement().isIdle());
        assertFalse(input.movement().isSprint());
        assertFalse(input.jumpRequested());
        assertFalse(input.primaryAction());
        assertFalse(input.secondaryAction());
        assertFalse(input.togglePistol());
        assertEquals(0, input.deltaYaw());
        assertEquals(0, input.deltaPitch());
        assertNull(input.selectedType());
    }
    @Test void rejectsInvalidLookAndAirWithoutMutatingNeutralInput() {
        MovementInput movement = PlayerFrameInput.NEUTRAL.movement();
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new PlayerFrameInput(movement,
                    false, invalid, 0, false, false, false, null));
            assertThrows(IllegalArgumentException.class, () -> new PlayerFrameInput(movement,
                    false, 0, invalid, false, false, false, null));
        }
        assertThrows(NullPointerException.class, () -> new PlayerFrameInput(null,
                false, 0, 0, false, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerFrameInput(movement,
                false, 0, 0, false, false, false, BlockType.AIR));
        assertTrue(PlayerFrameInput.NEUTRAL.movement().isIdle());
    }
}
