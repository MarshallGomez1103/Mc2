package application;

import domain.block.BlockType;
import domain.player.MovementInput;
import java.util.Objects;

/** Intenciones inmutables de un tick, independientes del dispositivo y del render. */
public record PlayerFrameInput(MovementInput movement, boolean jumpRequested,
                               float deltaYaw, float deltaPitch,
                               boolean primaryAction, boolean secondaryAction,
                               boolean togglePistol, BlockType selectedType) {
    public static final PlayerFrameInput NEUTRAL = new PlayerFrameInput(
            new MovementInput(false, false, false, false), false, 0, 0,
            false, false, false, null);

    public PlayerFrameInput {
        Objects.requireNonNull(movement, "movement no puede ser null");
        if (!Float.isFinite(deltaYaw) || !Float.isFinite(deltaPitch)) {
            throw new IllegalArgumentException("Los deltas de mirada deben ser finitos");
        }
        if (selectedType == BlockType.AIR) {
            throw new IllegalArgumentException("AIR no es un material seleccionable");
        }
    }

    /** selectedType null conserva el material actual; los demás campos son intenciones del tick. */
    public static PlayerFrameInput neutral() { return NEUTRAL; }
}
