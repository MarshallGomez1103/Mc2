package domain.enemy;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.time.Instant;

/** Mundos pequeños y deterministas para probar navegación e IA sin el generador de terreno. */
public final class TestWorlds {
    public static final int GROUND_Y = 20;
    private static final BlockFactory FACTORY = new BlockFactory();

    private TestWorlds() {
    }

    /** Chunks (0..chunksX-1, 0..chunksZ-1) con una única capa de STONE en GROUND_Y. */
    public static World flat(int chunksX, int chunksZ) {
        World world = new World("nav", 7, Instant.parse("2026-09-21T12:00:00Z"));
        for (int cx = 0; cx < chunksX; cx++) {
            for (int cz = 0; cz < chunksZ; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    for (int z = 0; z < Chunk.DEPTH; z++) {
                        Position position = new Position(cx * Chunk.WIDTH + x, GROUND_Y, cz * Chunk.DEPTH + z);
                        chunk.addBlock(FACTORY.create(BlockType.STONE, position));
                    }
                }
                world.addChunk(chunk);
            }
        }
        return world;
    }

    public static void place(World world, int x, int y, int z, BlockType type) {
        Position position = new Position(x, y, z);
        world.placeBlock(Chunk.chunkXFor(position), Chunk.chunkZFor(position), FACTORY.create(type, position));
    }

    public static void remove(World world, int x, int y, int z) {
        Position position = new Position(x, y, z);
        world.removeBlock(Chunk.chunkXFor(position), Chunk.chunkZFor(position), position);
    }

    /** Pared de dos bloques de alto (impide el cuerpo) en la columna (x, z). */
    public static void wall(World world, int x, int z) {
        place(world, x, GROUND_Y + 1, z, BlockType.STONE);
        place(world, x, GROUND_Y + 2, z, BlockType.STONE);
    }

    public static NavigationNode ground(int x, int z) {
        return new NavigationNode(x, GROUND_Y, z);
    }
}
