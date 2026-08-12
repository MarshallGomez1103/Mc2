package domain.world;

import domain.block.Block;

import java.util.Objects;

/** Notificación pequeña usada por Observer al cambiar un bloque. */
public record BlockChange(World world, Block block, Type type) {
    public BlockChange {
        Objects.requireNonNull(world, "world no puede ser null");
        Objects.requireNonNull(block, "block no puede ser null");
        Objects.requireNonNull(type, "type no puede ser null");
    }

    public enum Type {
        PLACED,
        REMOVED
    }
}
