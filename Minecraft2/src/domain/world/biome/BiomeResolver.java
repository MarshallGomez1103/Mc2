package domain.world.biome;

import domain.world.generation.SpatialNoise;

/** Selecciona regiones de bioma a partir de una señal espacial reproducible. */
public final class BiomeResolver {
    private static final int REGION_SCALE = 18;
    private static final long CLIMATE_SALT = 0x42494f4d45L;
    private final long seed;

    public BiomeResolver(long seed) {
        this.seed = seed;
    }

    public BiomeType biomeAt(int worldX, int worldZ) {
        double climate = climateAt(worldX, worldZ);
        if (climate < 0.35) {
            return BiomeType.DESERT;
        }
        if (climate > 0.65) {
            return BiomeType.MOUNTAINS;
        }
        return BiomeType.PLAINS;
    }

    /** Señal continua que permite suavizar la altura al acercarse a montañas. */
    public double climateAt(int worldX, int worldZ) {
        return SpatialNoise.sample(seed, worldX, worldZ, REGION_SCALE, CLIMATE_SALT);
    }
}
