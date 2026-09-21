package domain.world.generation;

/** Ruido de valor interpolado, estable para coordenadas positivas y negativas. */
public final class SpatialNoise {
    private SpatialNoise() {
    }

    public static double sample(long seed, int worldX, int worldZ, int scale, long salt) {
        if (scale <= 0) {
            throw new IllegalArgumentException("La escala debe ser positiva");
        }
        int cellX = Math.floorDiv(worldX, scale);
        int cellZ = Math.floorDiv(worldZ, scale);
        double x = smooth(Math.floorMod(worldX, scale) / (double) scale);
        double z = smooth(Math.floorMod(worldZ, scale) / (double) scale);

        double north = lerp(value(seed, cellX, cellZ, salt),
                value(seed, (long) cellX + 1, cellZ, salt), x);
        double south = lerp(value(seed, cellX, (long) cellZ + 1, salt),
                value(seed, (long) cellX + 1, (long) cellZ + 1, salt), x);
        return lerp(north, south, z);
    }

    private static double value(long seed, long cellX, long cellZ, long salt) {
        long mixed = seed ^ salt ^ (cellX * 341_873_128_712L) ^ (cellZ * 132_897_987_541L);
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double value) {
        return value * value * (3 - 2 * value);
    }

    private static double lerp(double first, double second, double fraction) {
        return first + (second - first) * fraction;
    }
}
