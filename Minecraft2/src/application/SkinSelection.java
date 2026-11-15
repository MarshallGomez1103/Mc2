package application;

import domain.player.Player;
import java.util.Map;

/** Reads the local player's skin from MC2_SKIN_ID; a missing or unusable value means skin 0. */
public final class SkinSelection {
    public static final String VARIABLE = "MC2_SKIN_ID";

    private SkinSelection() { }

    public static int fromEnvironment(Map<String, String> environment) {
        String raw = environment.get(VARIABLE);
        if (raw == null) return 0;
        try {
            int id = Integer.parseInt(raw.trim());
            return id >= 0 && id <= Player.MAX_SKIN_ID ? id : 0;
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }
}
