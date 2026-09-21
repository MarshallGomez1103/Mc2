package presentation.game;

import domain.world.Chunk;
import domain.world.World;

import java.util.Objects;

/** Radio cuadrado de chunks visibles; radio cero dibuja solo el chunk del jugador. */
public final class RenderDistance {
    private final int maxRadius;
    private int radius;

    public RenderDistance(int maxRadius) {
        if (maxRadius < 0) {
            throw new IllegalArgumentException("El radio máximo no puede ser negativo");
        }
        this.maxRadius = maxRadius;
        this.radius = Math.min(1, maxRadius);
    }

    public static RenderDistance forWorld(World world) {
        Objects.requireNonNull(world, "world no puede ser null");
        if (world.getChunks().isEmpty()) {
            return new RenderDistance(0);
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Chunk chunk : world.getChunks()) {
            minX = Math.min(minX, chunk.getChunkX());
            maxX = Math.max(maxX, chunk.getChunkX());
            minZ = Math.min(minZ, chunk.getChunkZ());
            maxZ = Math.max(maxZ, chunk.getChunkZ());
        }
        return new RenderDistance(Math.max(maxX - minX, maxZ - minZ));
    }

    public int radius() {
        return radius;
    }

    public int maxRadius() {
        return maxRadius;
    }

    public void decrease() {
        radius = Math.max(0, radius - 1);
    }

    public void increase() {
        radius = Math.min(maxRadius, radius + 1);
    }

    public boolean contains(int playerChunkX, int playerChunkZ, Chunk chunk) {
        return Math.abs((long) chunk.getChunkX() - playerChunkX) <= radius
                && Math.abs((long) chunk.getChunkZ() - playerChunkZ) <= radius;
    }
}
