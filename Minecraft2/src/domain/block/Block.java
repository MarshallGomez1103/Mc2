package domain.block;

import domain.Position;

import java.util.Objects;

/** Bloque básico, sin comportamiento gráfico ni físico. */
public final class Block {
    private final BlockType type;
    private final Position position;

    public Block(BlockType type, Position position) {
        this.type = Objects.requireNonNull(type, "type no puede ser null");
        this.position = Objects.requireNonNull(position, "position no puede ser null");
    }

    public BlockType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }
}
