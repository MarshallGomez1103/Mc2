package domain.world;

import domain.Position;
import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hueco del Corte 1: límites de un chunk de 16 × 64 × 16 y resolución de coordenadas negativas. */
class ChunkBoundsTest {
    private final BlockFactory factory = new BlockFactory();

    private void add(Chunk chunk, int x, int y, int z) {
        chunk.addBlock(factory.create(BlockType.STONE, new Position(x, y, z)));
    }

    @Test
    void acceptsTheEightCornersOfItsVolume() {
        Chunk chunk = new Chunk(0, 0);
        int[] xs = {0, Chunk.WIDTH - 1};
        int[] ys = {0, Chunk.HEIGHT - 1};
        int[] zs = {0, Chunk.DEPTH - 1};
        for (int x : xs) {
            for (int y : ys) {
                for (int z : zs) {
                    add(chunk, x, y, z);
                    assertTrue(chunk.getBlock(new Position(x, y, z)).isPresent());
                }
            }
        }
        assertEquals(8, chunk.getBlocks().size());
    }

    @Test
    void rejectsHeightsOutsideZeroToSixtyThree() {
        Chunk chunk = new Chunk(0, 0);
        assertThrows(IllegalArgumentException.class, () -> add(chunk, 3, -1, 3));
        assertThrows(IllegalArgumentException.class, () -> add(chunk, 3, Chunk.HEIGHT, 3));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(new Position(3, Chunk.HEIGHT, 3)));
    }

    @Test
    void rejectsPositionsOfNeighbourChunks() {
        Chunk chunk = new Chunk(0, 0);
        assertThrows(IllegalArgumentException.class, () -> add(chunk, Chunk.WIDTH, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> add(chunk, -1, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> add(chunk, 0, 5, Chunk.DEPTH));
        assertThrows(IllegalArgumentException.class, () -> add(chunk, 0, 5, -1));
        assertTrue(chunk.getBlocks().isEmpty());
    }

    @Test
    void negativeCoordinatesBelongToNegativeChunks() {
        assertEquals(-1, Chunk.chunkXFor(new Position(-1, 0, 0)));
        assertEquals(-1, Chunk.chunkXFor(new Position(-16, 0, 0)));
        assertEquals(-2, Chunk.chunkXFor(new Position(-17, 0, 0)));
        assertEquals(0, Chunk.chunkZFor(new Position(0, 0, 15)));
        assertEquals(1, Chunk.chunkZFor(new Position(0, 0, 16)));
        assertEquals(-1, Chunk.chunkZFor(new Position(0, 0, -16)));

        Chunk negative = new Chunk(-1, -1);
        add(negative, -16, 10, -1);
        add(negative, -1, 10, -16);
        assertEquals(2, negative.getBlocks().size());
        assertThrows(IllegalArgumentException.class, () -> add(negative, 0, 10, -1));
    }
}
