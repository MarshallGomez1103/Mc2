package presentation.game;

/** Small presentation-only sprint effect; never changes the player or collision box. */
public final class SprintCameraEffect {
    private static final double MAX_VERTICAL_OFFSET = 0.035;
    private static final double MAX_FOV_OFFSET = 4;
    private double phase;
    private double intensity;
    private double verticalOffset;

    /** Call only while RUNNING. Not calling this method freezes the effect during pause. */
    public void update(double deltaSeconds, boolean sprinting, boolean moving, boolean grounded) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and nonnegative");
        }
        double target = sprinting && moving ? 1 : 0;
        intensity += (target - intensity) * (1 - Math.exp(-10 * deltaSeconds));
        if (sprinting && moving && grounded) {
            phase = (phase + deltaSeconds * Math.PI * 2 * 3) % (Math.PI * 2);
            verticalOffset = Math.sin(phase) * MAX_VERTICAL_OFFSET * intensity;
        } else {
            verticalOffset *= Math.exp(-15 * deltaSeconds);
        }
    }

    public float verticalOffset() { return (float) verticalOffset; }
    public float fovOffset() { return (float) (intensity * MAX_FOV_OFFSET); }

    public void reset() {
        phase = 0;
        intensity = 0;
        verticalOffset = 0;
    }
}
