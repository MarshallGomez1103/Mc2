package application;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** MC2_SKIN_ID chooses the local player's skin; anything unusable falls back to skin 0. */
class SkinSelectionTest {
    @Test void missingVariableMeansSkinZero() {
        assertEquals(0, SkinSelection.fromEnvironment(Map.of()));
    }
    @Test void validIdsAreAcceptedWithSurroundingSpaces() {
        for (int id = 0; id <= 2; id++) {
            assertEquals(id, SkinSelection.fromEnvironment(Map.of("MC2_SKIN_ID", " " + id + " ")));
        }
    }
    @Test void outOfRangeOrNonNumericValuesFallBackToSkinZero() {
        for (String bad : new String[] {"3", "-1", "99999999999", "abc", "", " ", "1.5"}) {
            assertEquals(0, SkinSelection.fromEnvironment(Map.of("MC2_SKIN_ID", bad)), bad);
        }
    }
}
