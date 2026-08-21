package application;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import patterns.factory.BlockFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Coordina la creación inicial de chunks a partir del terreno y la fábrica de bloques. */
public final class ChunkGenerationService {
    private static final int CHUNK_WIDTH = 16;
    private static final int CHUNK_DEPTH = 16;

    private final SimpleTerrainGenerator terrainGenerator;
    private final BlockFactory blockFactory;

    public ChunkGenerationService(SimpleTerrainGenerator terrainGenerator, BlockFactory blockFactory) {
        this.terrainGenerator = Objects.requireNonNull(terrainGenerator, "terrainGenerator no puede ser null");
        this.blockFactory = Objects.requireNonNull(blockFactory, "blockFactory no puede ser null");
    }

    /** Crea un chunk poblado con los bloques no vacíos determinados por la semilla. */
    public Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);

        for (int localX = 0; localX < CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < CHUNK_DEPTH; localZ++) {
                int worldX = chunkX * CHUNK_WIDTH + localX;
                int worldZ = chunkZ * CHUNK_DEPTH + localZ;
                populateColumn(chunk, worldX, worldZ);
            }
        }
        return chunk;
    }

    /** Crea una cuadrícula de chunks desde la coordenada de chunk {@code (0, 0)}. */
    public List<Chunk> generateChunks(int chunksWide, int chunksDeep) {
        if (chunksWide <= 0 || chunksDeep <= 0) {
            throw new IllegalArgumentException("La cantidad de chunks debe ser positiva");
        }

        List<Chunk> chunks = new ArrayList<>();
        for (int chunkX = 0; chunkX < chunksWide; chunkX++) {
            for (int chunkZ = 0; chunkZ < chunksDeep; chunkZ++) {
                chunks.add(generateChunk(chunkX, chunkZ));
            }
        }
        return List.copyOf(chunks);
    }

    private void populateColumn(Chunk chunk, int worldX, int worldZ) {
        for (int y = 0; y < SimpleTerrainGenerator.CHUNK_HEIGHT; y++) {
            BlockType type = terrainGenerator.blockTypeAt(worldX, y, worldZ);
            if (type != BlockType.AIR) {
                chunk.addBlock(blockFactory.create(type, new Position(worldX, y, worldZ)));
            }
        }
    }
}
