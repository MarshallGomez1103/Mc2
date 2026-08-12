package domain.player;

import domain.Position;

import java.util.Objects;

/** Estado mínimo del jugador; movimiento, salto y gravedad quedan pendientes. */
public final class Player {
    private Position position;

    public Player(Position initialPosition) {
        this.position = Objects.requireNonNull(initialPosition, "initialPosition no puede ser null");
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "position no puede ser null");
    }
}
