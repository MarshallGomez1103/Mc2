package domain.world;

import domain.block.BlockType;

/**
 * Genera un perfil de terreno pequeño y reproducible para el MVP.
 * No crea chunks ni bloques; solo determina qué tipo corresponde a una posición del mundo.
 */
public final class SimpleTerrainGenerator {
    public static final int CHUNK_HEIGHT = 64;
    public static final int MIN_SURFACE_HEIGHT = 18;
    public static final int MAX_SURFACE_HEIGHT = 22;

    private static final int DIRT_LAYERS = 2;
    private static final int SURFACE_FEATURE_BOUND = 10;
    private static final int TREE_FEATURE_BOUND = 41;
    private static final long SURFACE_SALT = 71_593_801L;
    private static final long TREE_SALT = 126_484_503L;
    private static final int TREE_RADIUS = 1;
    private static final int TRUNK_HEIGHT = 3;
    private final long seed;

    public SimpleTerrainGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * Devuelve una altura entre 18 y 22, inclusiva, para una columna del mundo.
     * La misma semilla y coordenadas siempre producen la misma altura.
     */
    public int surfaceHeightAt(int worldX, int worldZ) {
        long mixedCoordinates = seed
                ^ ((long) worldX * 341_873_128_712L)
                ^ ((long) worldZ * 132_897_987_541L);

        int possibleHeights = MAX_SURFACE_HEIGHT - MIN_SURFACE_HEIGHT + 1;
        return MIN_SURFACE_HEIGHT + Math.floorMod(mix(mixedCoordinates), possibleHeights);
    }

    /**
     * Determina el material de una posición sin construir todavía un {@code Block}.
     * Distribuye superficie, subsuelo y árboles pequeños de manera reproducible.
     */
    public BlockType blockTypeAt(int worldX, int y, int worldZ) {
        validateHeight(y);
        int surfaceHeight = surfaceHeightAt(worldX, worldZ);

        if (y > surfaceHeight) {
            return treeBlockTypeAt(worldX, y, worldZ);
        }
        if (y == surfaceHeight) {
            return surfaceBlockTypeAt(worldX, worldZ);
        }
        if (y >= surfaceHeight - DIRT_LAYERS) {
            return BlockType.DIRT;
        }
        return BlockType.STONE;
    }

    private BlockType surfaceBlockTypeAt(int worldX, int worldZ) {
        int feature = featureAt(worldX, worldZ, SURFACE_SALT, SURFACE_FEATURE_BOUND);
        return switch (feature) {
            case 0 -> BlockType.SAND;
            case 1 -> BlockType.GRAVEL;
            default -> BlockType.GRASS;
        };
    }

    private BlockType treeBlockTypeAt(int worldX, int y, int worldZ) {
        for (int rootX = worldX - TREE_RADIUS; rootX <= worldX + TREE_RADIUS; rootX++) {
            for (int rootZ = worldZ - TREE_RADIUS; rootZ <= worldZ + TREE_RADIUS; rootZ++) {
                int rootSurface = surfaceHeightAt(rootX, rootZ);
                if (isTreeRoot(rootX, rootZ)
                        && worldX == rootX
                        && worldZ == rootZ
                        && y > rootSurface
                        && y <= rootSurface + TRUNK_HEIGHT) {
                    return BlockType.WOOD;
                }
            }
        }

        for (int rootX = worldX - TREE_RADIUS; rootX <= worldX + TREE_RADIUS; rootX++) {
            for (int rootZ = worldZ - TREE_RADIUS; rootZ <= worldZ + TREE_RADIUS; rootZ++) {
                if (isTreeRoot(rootX, rootZ) && isLeafPosition(rootX, rootZ, worldX, y, worldZ)) {
                    return BlockType.LEAVES;
                }
            }
        }
        return BlockType.AIR;
    }

    private boolean isTreeRoot(int worldX, int worldZ) {
        return surfaceBlockTypeAt(worldX, worldZ) == BlockType.GRASS
                && featureAt(worldX, worldZ, TREE_SALT, TREE_FEATURE_BOUND) == 0;
    }

    private boolean isLeafPosition(int rootX, int rootZ, int worldX, int y, int worldZ) {
        int rootSurface = surfaceHeightAt(rootX, rootZ);
        int heightAboveRoot = y - rootSurface;
        int horizontalDistance = Math.max(Math.abs(worldX - rootX), Math.abs(worldZ - rootZ));

        return (heightAboveRoot == TRUNK_HEIGHT && horizontalDistance <= TREE_RADIUS)
                || (heightAboveRoot == TRUNK_HEIGHT + 1 && horizontalDistance <= TREE_RADIUS)
                || (heightAboveRoot == TRUNK_HEIGHT + 2 && horizontalDistance == 0);
    }

    private int featureAt(int worldX, int worldZ, long salt, int bound) {
        long mixedCoordinates = seed
                ^ ((long) worldX * 341_873_128_712L)
                ^ ((long) worldZ * 132_897_987_541L)
                ^ salt;
        return Math.floorMod(mix(mixedCoordinates), bound);
    }

    private void validateHeight(int y) {
        if (y < 0 || y >= CHUNK_HEIGHT) {
            throw new IllegalArgumentException("La altura debe estar entre 0 y 63");
        }
    }

    private long mix(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }
}
