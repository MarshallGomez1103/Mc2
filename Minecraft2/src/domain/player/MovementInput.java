package domain.player;

/** Intención inmutable de movimiento, sin dependencia de dispositivos ni gráficos. */
public final class MovementInput {
    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean sprint;

    public MovementInput(boolean forward, boolean backward, boolean left, boolean right) {
        this(forward, backward, left, right, false);
    }

    public MovementInput(boolean forward, boolean backward, boolean left, boolean right, boolean sprint) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.sprint = sprint;
    }

    public boolean isForward() {
        return forward;
    }

    public boolean isBackward() {
        return backward;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isSprint() {
        return sprint;
    }

    public boolean isIdle() {
        return !forward && !backward && !left && !right;
    }
}
