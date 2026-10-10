package domain.world;

import domain.Position;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldChunkIndexTest {
    @Test
    void indexesPositiveAndNegativeChunksWithoutChangingPublicList() {
        World world = new World("index", 1, Instant.parse("2026-09-21T12:00:00Z"));
        Chunk negative = new Chunk(-1, -1);
        Chunk positive = new Chunk(8, 2);
        world.addChunk(negative);
        world.addChunk(positive);
        assertEquals(negative, world.findChunk(new Position(-1, 20, -1)).orElseThrow());
        assertEquals(positive, world.findChunk(8, 2).orElseThrow());
        assertEquals(2, world.getChunks().size());
        assertTrue(world.findChunk(0, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> world.addChunk(new Chunk(-1, -1)));
        assertEquals(2, world.getChunks().size());
    }

    @Test
    void chunkKeysOfTheLargestWorldHaveDistinctHashCodes() {
        // Arrange: Grande es 16 × 16 chunks; se incluyen coordenadas negativas por simetría.
        Set<Integer> hashes = new HashSet<>();
        int keys = 0;

        // Act
        for (int x = -16; x < 16; x++) {
            for (int z = -16; z < 16; z++) {
                Object key = World.chunkKey(x, z);
                hashes.add(key.hashCode());
                keys++;
            }
        }

        // Assert: sin colisiones, el índice no degrada a cubetas en árbol en cada consulta de A*.
        assertEquals(keys, hashes.size());
    }
}
