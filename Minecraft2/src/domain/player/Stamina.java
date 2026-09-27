package domain.player;

/** Session stamina, independent of input devices, rendering and persistence. */
public final class Stamina {
    public static final double MAXIMUM = 100;
    public static final double CONSUMPTION_PER_SECOND = 10;
    public static final double REGENERATION_PER_SECOND = 20;
    public static final double RECOVERY_THRESHOLD = MAXIMUM * 0.20;
    private static final double EPSILON = 1e-9;

    private double current = MAXIMUM;
    private boolean exhausted;

    public double current() { return current; }
    public double maximum() { return MAXIMUM; }
    public double fraction() { return current / MAXIMUM; }
    public boolean exhausted() { return exhausted; }
    public boolean canSprint() { return !exhausted && current > EPSILON; }

    /** Limits sprint duration so the last frame never receives free sprint time. */
    public double sprintSeconds(double deltaSeconds) {
        validateDelta(deltaSeconds);
        return canSprint() ? Math.min(deltaSeconds, current / CONSUMPTION_PER_SECOND) : 0;
    }

    /** Call only for active simulation; pausing/death freezes this component. */
    public void update(double deltaSeconds, boolean actuallySprinting) {
        advance(deltaSeconds, actuallySprinting ? sprintSeconds(deltaSeconds) : 0);
    }

    /** Actual sprint time comes from the collision-resolved horizontal displacement. */
    public void advance(double deltaSeconds, double actualSprintSeconds) {
        validateDelta(deltaSeconds);
        if (!Double.isFinite(actualSprintSeconds) || actualSprintSeconds < 0
                || actualSprintSeconds > deltaSeconds + EPSILON) {
            throw new IllegalArgumentException("Sprint time must fit the simulation step");
        }
        if (actualSprintSeconds > 0 && canSprint()) {
            current = Math.max(0, current - actualSprintSeconds * CONSUMPTION_PER_SECOND);
            if (current <= EPSILON) {
                current = 0;
                exhausted = true;
            }
        } else {
            current = Math.min(MAXIMUM, current + REGENERATION_PER_SECOND * deltaSeconds);
            if (current + EPSILON >= RECOVERY_THRESHOLD) {
                exhausted = false;
            }
        }
    }

    /** Respawn and a new play session start full; JSON saves remain compatible. */
    public void reset() {
        current = MAXIMUM;
        exhausted = false;
    }

    private static void validateDelta(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and nonnegative");
        }
    }
}
