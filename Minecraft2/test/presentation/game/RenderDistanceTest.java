package presentation.game;

import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDistanceTest {
    @Test
    void zeroShowsOneChunkAndRadiusCanGrowUntilWholeFiniteWorld() {
        RenderDistance distance = new RenderDistance(2);
        Chunk center = new Chunk(0, 0);
        Chunk neighbor = new Chunk(1, 0);
        Chunk far = new Chunk(2, 2);
        assertTrue(distance.contains(0, 0, center));
        assertTrue(distance.contains(0, 0, neighbor));
        assertFalse(distance.contains(0, 0, far));

        distance.decrease();
        distance.decrease();
        assertEquals(0, distance.radius());
        assertFalse(distance.contains(0, 0, neighbor));
        distance.increase();
        distance.increase();
        distance.increase();
        assertEquals(2, distance.radius());
        assertTrue(distance.contains(0, 0, far));
    }

    @Test
    void maximumComesFromLoadedChunksIncludingNegativeCoordinates() {
        World world = new World("distance", 1, Instant.parse("2026-09-21T12:00:00Z"));
        world.addChunk(new Chunk(-2, -1));
        world.addChunk(new Chunk(3, 4));
        RenderDistance distance = RenderDistance.forWorld(world);
        assertEquals(5, distance.maxRadius());
    }
}
