package domain.world;

import application.ChunkGenerationService;
import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.world.biome.BiomeType;
import domain.world.generation.VillagePlan;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;
import persistence.WorldJsonCodec;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainGenerationTest {
    private final BlockFactory blocks = new BlockFactory();

    @Test
    void layersAndHeightsRespectEachBiomeAndChunkLimits() {
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(17);
        Map<BiomeType, Position> samples = new HashMap<>();
        for (int x = -64; x <= 64; x += 2) {
            for (int z = -64; z <= 64; z += 2) {
                samples.putIfAbsent(terrain.biomeAt(x, z), new Position(x, 0, z));
            }
        }
        assertEquals(3, samples.size());
        for (Map.Entry<BiomeType, Position> sample : samples.entrySet()) {
            int x = sample.getValue().x();
            int z = sample.getValue().z();
            int top = terrain.surfaceHeightAt(x, z);
            assertTrue(top >= SimpleTerrainGenerator.MIN_SURFACE_HEIGHT);
            assertTrue(top <= SimpleTerrainGenerator.MAX_SURFACE_HEIGHT);
            assertEquals(BlockType.STONE, terrain.blockTypeAt(x, 0, z));
            assertEquals(BlockType.AIR, terrain.blockTypeAt(x, Chunk.HEIGHT - 1, z));
            if (sample.getKey() == BiomeType.DESERT) {
                assertEquals(BlockType.SAND, terrain.blockTypeAt(x, top, z));
                assertEquals(BlockType.SAND, terrain.blockTypeAt(x, top - 1, z));
            } else if (sample.getKey() == BiomeType.PLAINS) {
                assertEquals(BlockType.GRASS, terrain.blockTypeAt(x, top, z));
                assertEquals(BlockType.DIRT, terrain.blockTypeAt(x, top - 1, z));
            } else {
                assertTrue(terrain.blockTypeAt(x, top, z) == BlockType.STONE
                        || terrain.blockTypeAt(x, top, z) == BlockType.GRAVEL);
            }
        }
        assertTrue(terrain.surfaceHeightAt(-64, -60) >= 32, "la montaña debe elevar el relieve");
        assertThrows(IllegalArgumentException.class, () -> terrain.blockTypeAt(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> terrain.blockTypeAt(0, Chunk.HEIGHT, 0));
    }

    @Test
    void desertHasNoTreesAndPlainsCanGenerateThem() {
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(17);
        boolean foundTree = false;
        for (int x = -48; x < 48; x++) {
            for (int z = -48; z < 48; z++) {
                int top = terrain.surfaceHeightAt(x, z);
                for (int y = top + 1; y <= Math.min(top + 5, Chunk.HEIGHT - 1); y++) {
                    BlockType type = terrain.blockTypeAt(x, y, z);
                    if (terrain.biomeAt(x, z) == BiomeType.DESERT) {
                        assertNotEquals(BlockType.WOOD, type);
                        assertNotEquals(BlockType.LEAVES, type);
                    }
                    if (terrain.biomeAt(x, z) == BiomeType.PLAINS && type == BlockType.WOOD) {
                        foundTree = true;
                    }
                }
            }
        }
        assertTrue(foundTree, "los árboles deben aparecer ocasionalmente en PLAINS");
    }

    @Test
    void chunksGeneratedInDifferentOrderContainTheSameBlocksEvenAcrossNegativeSeams() {
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(17);
        ChunkGenerationService service = new ChunkGenerationService(terrain, blocks);
        Chunk left = service.generateChunk(-1, 0);
        Chunk right = service.generateChunk(0, 0);
        Map<Position, BlockType> before = types(left);
        before.putAll(types(right));

        Chunk rightFirst = service.generateChunk(0, 0);
        Chunk leftSecond = service.generateChunk(-1, 0);
        Map<Position, BlockType> after = types(rightFirst);
        after.putAll(types(leftSecond));
        assertEquals(before, after);
        Chunk recreated = new ChunkGenerationService(new SimpleTerrainGenerator(17), blocks)
                .generateChunk(-1, 0);
        assertEquals(types(left), types(recreated), "otra instancia con la misma seed debe coincidir");

        for (int z = 0; z < Chunk.DEPTH; z++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                assertEquals(terrain.blockTypeAt(-1, y, z), typeAt(left, -1, y, z));
                assertEquals(terrain.blockTypeAt(0, y, z), typeAt(right, 0, y, z));
            }
        }
    }

    @Test
    void neighboringNaturalColumnsChangeGraduallyAcrossChunkBorders() {
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(17);
        for (int z = -32; z < 32; z++) {
            for (int x : new int[] {-17, -1, 15, 31}) {
                int difference = Math.abs(terrain.surfaceHeightAt(x, z)
                        - terrain.surfaceHeightAt(x + 1, z));
                assertTrue(difference <= 6, "desnivel excesivo entre columnas vecinas en " + x + "," + z);
            }
        }
    }

    @Test
    void villageIsDeterministicContainedAndHasHouseDoorAndPath() {
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(17);
        VillagePlan plan = terrain.villageAtChunk(0, 1).orElseThrow();
        assertEquals(plan, new SimpleTerrainGenerator(17).villageAtChunk(0, 1).orElseThrow());
        assertEquals(BiomeType.PLAINS, terrain.biomeAt(7, 23));
        int ground = plan.groundY();
        assertEquals(BlockType.WOOD, terrain.blockTypeAt(5, ground + 2, 21));
        assertEquals(BlockType.AIR, terrain.blockTypeAt(7, ground + 2, 21));
        assertEquals(BlockType.WOOD, terrain.blockTypeAt(7, ground + 5, 23));
        assertEquals(BlockType.GRAVEL, terrain.blockTypeAt(7, ground + 1, 19));
        assertEquals(BlockType.AIR, terrain.blockTypeAt(5, ground + 3, 23),
                "la pared lateral debe tener una ventana abierta");
        assertEquals(BlockType.WOOD, terrain.blockTypeAt(5, ground + 2, 23),
                "debe quedar pared bajo la ventana");
        assertFalse(plan.reserves(7, 16));
        assertFalse(plan.reserves(16, 23));

        Chunk chunk = new ChunkGenerationService(terrain, blocks).generateChunk(0, 1);
        assertEquals(BlockType.WOOD, typeAt(chunk, 7, ground + 5, 23));
        assertEquals(BlockType.AIR, typeAt(chunk, 7, ground + 2, 21));
    }

    @Test
    void generatedBlocksAndAPlayerEditSurviveExistingJsonFormat() throws Exception {
        long seed = 17;
        SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(seed);
        ChunkGenerationService service = new ChunkGenerationService(terrain, blocks);
        World world = new World("terrain_c2", seed, Instant.parse("2026-09-21T12:00:00Z"));
        world.addChunk(service.generateChunk(0, 1));
        world.addChunk(service.generateChunk(-1, 0));
        Position edit = new Position(-1, 50, 0);
        world.placeBlock(-1, 0, blocks.create(BlockType.WOOD, edit));

        World restored = WorldJsonCodec.read(WorldJsonCodec.write(world), blocks);
        assertEquals(world.getSeed(), restored.getSeed());
        assertEquals(BlockType.WOOD, typeAt(restored.findChunk(-1, 0).orElseThrow(), -1, 50, 0));
        assertEquals(types(world.findChunk(0, 1).orElseThrow()), types(restored.findChunk(0, 1).orElseThrow()));
    }

    private Map<Position, BlockType> types(Chunk chunk) {
        Map<Position, BlockType> result = new HashMap<>();
        for (Block block : chunk.getBlocks()) {
            result.put(block.getPosition(), block.getType());
        }
        return result;
    }

    private BlockType typeAt(Chunk chunk, int x, int y, int z) {
        return chunk.getBlock(new Position(x, y, z)).map(Block::getType).orElse(BlockType.AIR);
    }
}
