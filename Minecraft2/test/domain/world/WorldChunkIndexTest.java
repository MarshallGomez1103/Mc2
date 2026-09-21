package domain.world;

import domain.Position;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
}
