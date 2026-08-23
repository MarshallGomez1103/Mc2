package patterns.factory;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;

import java.util.Objects;

/** Factory: punto único de creación de los bloques admitidos. */
public final class BlockFactory {
    public Block create(BlockType type, Position position) {
        Objects.requireNonNull(type, "type no puede ser null");
        Objects.requireNonNull(position, "position no puede ser null");

        return switch (type) {
            case AIR, GRASS, DIRT, STONE, SAND, GRAVEL, WOOD, LEAVES -> new Block(type, position);
        };
    }
}
