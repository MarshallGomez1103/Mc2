package domain.world.generation;

import domain.world.Chunk;
import domain.world.biome.BiomeResolver;
import domain.world.biome.BiomeType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Decide la ubicación por chunk; la plantilla de bloques está en VillagePlan. */
public final class VillageGenerator {
    @FunctionalInterface
    public interface HeightAt {
        int get(int worldX, int worldZ);
    }

    private static final long VILLAGE_SALT = 0x56494c4c414745L;
    private final long seed;
    private final BiomeResolver biomes;
    private final HeightAt naturalHeight;
    private final Map<Long, Optional<VillagePlan>> plans = new HashMap<>();

    public VillageGenerator(long seed, BiomeResolver biomes, HeightAt naturalHeight) {
        this.seed = seed;
        this.biomes = Objects.requireNonNull(biomes, "biomes no puede ser null");
        this.naturalHeight = Objects.requireNonNull(naturalHeight, "naturalHeight no puede ser null");
    }

    public Optional<VillagePlan> planForChunk(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        return plans.computeIfAbsent(key, ignored -> select(chunkX, chunkZ));
    }

    private Optional<VillagePlan> select(int chunkX, int chunkZ) {
        if (SpatialNoise.sample(seed, chunkX, chunkZ, 1, VILLAGE_SALT) >= 0.5) {
            return Optional.empty();
        }
        int originX = chunkX * Chunk.WIDTH;
        int originZ = chunkZ * Chunk.DEPTH;
        int centerX = originX + 7;
        int centerZ = originZ + 7;
        if (biomes.biomeAt(centerX, centerZ) != BiomeType.PLAINS) {
            return Optional.empty();
        }
        int ground = naturalHeight.get(centerX, centerZ);
        for (int x = 4; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                if (biomes.biomeAt(worldX, worldZ) != BiomeType.PLAINS
                        || Math.abs(naturalHeight.get(worldX, worldZ) - ground) > 5) {
                    return Optional.empty();
                }
            }
        }
        if (ground + 5 >= Chunk.HEIGHT) {
            return Optional.empty();
        }
        return Optional.of(new VillagePlan(chunkX, chunkZ, ground));
    }
}
