package application;

import domain.block.Block;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Hueco del Corte 1: una seed describe siempre el mismo terreno, bloque por bloque. */
class SameSeedTest {
    private static Map<String, String> terrain(long seed) {
        ChunkGenerationService service =
                new ChunkGenerationService(new SimpleTerrainGenerator(seed), new BlockFactory());
        List<Chunk> chunks = service.generateChunks(2, 2);
        return chunks.stream()
                .flatMap(chunk -> chunk.getBlocks().stream())
                .collect(Collectors.toMap(block -> block.getPosition().toString(), SameSeedTest::type));
    }

    private static String type(Block block) {
        return block.getType().name();
    }

    @Test
    void sameSeedGeneratesIdenticalBlocks() {
        Map<String, String> first = terrain(4242L);
        Map<String, String> second = terrain(4242L);

        assertEquals(first.size(), second.size());
        assertEquals(first, second);
    }

    @Test
    void differentSeedChangesTheTerrain() {
        assertNotEquals(terrain(4242L), terrain(4243L));
    }
}
