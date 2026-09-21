package domain.world.generation;

import domain.block.BlockType;
import domain.world.Chunk;

import java.util.Optional;

/** Una casa con puerta y ventanas abiertas, más un camino, dentro de un solo chunk. */
public record VillagePlan(int chunkX, int chunkZ, int groundY) {
    private int localX(int worldX) {
        return Math.floorMod(worldX, Chunk.WIDTH);
    }

    private int localZ(int worldZ) {
        return Math.floorMod(worldZ, Chunk.DEPTH);
    }

    public boolean reserves(int worldX, int worldZ) {
        if (Math.floorDiv(worldX, Chunk.WIDTH) != chunkX
                || Math.floorDiv(worldZ, Chunk.DEPTH) != chunkZ) {
            return false;
        }
        int x = localX(worldX);
        int z = localZ(worldZ);
        return x >= 4 && x <= 11 && z >= 1 && z <= 11;
    }

    /** AIR explícito vacía el interior y la puerta; Optional.empty conserva el terreno. */
    public Optional<BlockType> blockAt(int worldX, int y, int worldZ) {
        if (!reserves(worldX, worldZ) || y <= groundY) {
            return Optional.empty();
        }
        int x = localX(worldX);
        int z = localZ(worldZ);
        boolean house = x >= 5 && x <= 10 && z >= 5 && z <= 10;
        boolean path = x == 7 && z >= 1 && z <= 4;
        if (house) {
            if (y == groundY + 1 || y == groundY + 5) {
                return Optional.of(BlockType.WOOD);
            }
            if (y >= groundY + 2 && y <= groundY + 4) {
                boolean wall = x == 5 || x == 10 || z == 5 || z == 10;
                boolean door = x == 7 && z == 5 && y <= groundY + 3;
                boolean window = y == groundY + 3 && (
                        (x == 5 || x == 10) && (z == 7 || z == 8)
                                || z == 10 && (x == 7 || x == 8)
                                || z == 5 && x == 9);
                return Optional.of(wall && !door && !window ? BlockType.WOOD : BlockType.AIR);
            }
        } else if (path && y == groundY + 1) {
            return Optional.of(BlockType.GRAVEL);
        }
        return Optional.of(BlockType.AIR);
    }
}
