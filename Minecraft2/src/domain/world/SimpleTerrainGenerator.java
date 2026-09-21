package domain.world;

import domain.block.BlockType;
import domain.world.biome.BiomeResolver;
import domain.world.biome.BiomeType;
import domain.world.generation.SpatialNoise;
import domain.world.generation.VillageGenerator;
import domain.world.generation.VillagePlan;

import java.util.Optional;

/** Determina altura y materiales desde seed y coordenadas, sin crear bloques ni renderizar. */
public final class SimpleTerrainGenerator {
    public static final int CHUNK_HEIGHT = Chunk.HEIGHT;
    public static final int MIN_SURFACE_HEIGHT = 18;
    public static final int MAX_SURFACE_HEIGHT = 45;

    private static final int DIRT_LAYERS = 2;
    private static final int TREE_RADIUS = 1;
    private static final int TRUNK_HEIGHT = 3;
    private static final long RELIEF_SALT = 0x52454c494546L;
    private static final long DETAIL_SALT = 0x44455441494cL;
    private static final long TREE_SALT = 126_484_503L;

    private final long seed;
    private final BiomeResolver biomes;
    private final VillageGenerator villages;

    public SimpleTerrainGenerator(long seed) {
        this.seed = seed;
        this.biomes = new BiomeResolver(seed);
        this.villages = new VillageGenerator(seed, biomes, this::naturalSurfaceHeightAt);
    }

    public BiomeType biomeAt(int worldX, int worldZ) {
        return biomes.biomeAt(worldX, worldZ);
    }

    public Optional<VillagePlan> villageAtChunk(int chunkX, int chunkZ) {
        return villages.planForChunk(chunkX, chunkZ);
    }

    public int surfaceHeightAt(int worldX, int worldZ) {
        Optional<VillagePlan> village = villageAt(worldX, worldZ);
        if (village.isPresent() && village.get().reserves(worldX, worldZ)) {
            return village.get().groundY();
        }
        return naturalSurfaceHeightAt(worldX, worldZ);
    }

    private int naturalSurfaceHeightAt(int worldX, int worldZ) {
        double relief = SpatialNoise.sample(seed, worldX, worldZ, 14, RELIEF_SALT);
        double detail = SpatialNoise.sample(seed, worldX, worldZ, 7, DETAIL_SALT);
        double climate = biomes.climateAt(worldX, worldZ);
        // El ascenso empieza antes del umbral visual de MOUNTAINS para evitar paredes abruptas.
        double mountain = Math.max(0.0, Math.min(1.0, (climate - 0.45) / 0.35));
        int height = 18 + (int) Math.round(4 * relief + 2 * detail + 20 * mountain);
        return Math.min(MAX_SURFACE_HEIGHT, Math.max(MIN_SURFACE_HEIGHT, height));
    }

    public BlockType blockTypeAt(int worldX, int y, int worldZ) {
        validateHeight(y);
        int surfaceHeight = surfaceHeightAt(worldX, worldZ);
        if (y > surfaceHeight) {
            Optional<VillagePlan> village = villageAt(worldX, worldZ);
            if (village.isPresent() && village.get().reserves(worldX, worldZ)) {
                return village.get().blockAt(worldX, y, worldZ).orElse(BlockType.AIR);
            }
            return treeBlockTypeAt(worldX, y, worldZ);
        }
        BiomeType biome = biomeAt(worldX, worldZ);
        if (y == surfaceHeight) {
            return switch (biome) {
                case DESERT -> BlockType.SAND;
                case PLAINS -> BlockType.GRASS;
                case MOUNTAINS -> surfaceHeight >= 34 ? BlockType.STONE : BlockType.GRAVEL;
            };
        }
        if (y >= surfaceHeight - DIRT_LAYERS) {
            return biome == BiomeType.DESERT ? BlockType.SAND : BlockType.DIRT;
        }
        return BlockType.STONE;
    }

    private BlockType treeBlockTypeAt(int worldX, int y, int worldZ) {
        if (biomeAt(worldX, worldZ) != BiomeType.PLAINS) {
            return BlockType.AIR;
        }
        for (int rootX = worldX - TREE_RADIUS; rootX <= worldX + TREE_RADIUS; rootX++) {
            for (int rootZ = worldZ - TREE_RADIUS; rootZ <= worldZ + TREE_RADIUS; rootZ++) {
                if (!isTreeRoot(rootX, rootZ)) {
                    continue;
                }
                int rootSurface = surfaceHeightAt(rootX, rootZ);
                int above = y - rootSurface;
                int distance = Math.max(Math.abs(worldX - rootX), Math.abs(worldZ - rootZ));
                if (distance == 0 && above >= 1 && above <= TRUNK_HEIGHT) {
                    return BlockType.WOOD;
                }
                if ((above == TRUNK_HEIGHT || above == TRUNK_HEIGHT + 1) && distance <= TREE_RADIUS
                        || above == TRUNK_HEIGHT + 2 && distance == 0) {
                    return BlockType.LEAVES;
                }
            }
        }
        return BlockType.AIR;
    }

    private boolean isTreeRoot(int worldX, int worldZ) {
        if (biomeAt(worldX, worldZ) != BiomeType.PLAINS) {
            return false;
        }
        Optional<VillagePlan> village = villageAt(worldX, worldZ);
        if (village.isPresent() && village.get().reserves(worldX, worldZ)) {
            return false;
        }
        return SpatialNoise.sample(seed, worldX, worldZ, 1, TREE_SALT) < 0.012;
    }

    private Optional<VillagePlan> villageAt(int worldX, int worldZ) {
        return villages.planForChunk(Math.floorDiv(worldX, Chunk.WIDTH), Math.floorDiv(worldZ, Chunk.DEPTH));
    }

    private void validateHeight(int y) {
        if (y < 0 || y >= CHUNK_HEIGHT) {
            throw new IllegalArgumentException("La altura debe estar entre 0 y 63");
        }
    }
}
