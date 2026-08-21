package domain.world;

import domain.Position;
import domain.block.Block;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Contenedor de bloques con límites fijos y coordenadas absolutas del mundo. */
public final class Chunk {
    public static final int WIDTH = 16;
    public static final int HEIGHT = 64;
    public static final int DEPTH = 16;

    private final int chunkX;
    private final int chunkZ;
    private final Map<Position, Block> blocks = new LinkedHashMap<>();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    /** Resuelve la coordenada horizontal del chunk para una posición absoluta del mundo. */
    public static int chunkXFor(Position position) {
        return Math.floorDiv(requirePosition(position).x(), WIDTH);
    }

    /** Resuelve la coordenada de profundidad del chunk para una posición absoluta del mundo. */
    public static int chunkZFor(Position position) {
        return Math.floorDiv(requirePosition(position).z(), DEPTH);
    }

    public Collection<Block> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public Optional<Block> getBlock(Position position) {
        validatePosition(position);
        return Optional.ofNullable(blocks.get(position));
    }

    /** Agrega un bloque durante la construcción inicial del chunk. */
    public void addBlock(Block block) {
        put(block);
    }

    Block put(Block block) {
        Block validatedBlock = Objects.requireNonNull(block, "block no puede ser null");
        validatePosition(validatedBlock.getPosition());
        return blocks.put(validatedBlock.getPosition(), validatedBlock);
    }

    Block remove(Position position) {
        validatePosition(position);
        return blocks.remove(position);
    }

    private void validatePosition(Position position) {
        Position validatedPosition = requirePosition(position);
        if (validatedPosition.y() < 0 || validatedPosition.y() >= HEIGHT) {
            throw new IllegalArgumentException("La altura debe estar entre 0 y 63");
        }
        if (chunkXFor(validatedPosition) != chunkX || chunkZFor(validatedPosition) != chunkZ) {
            throw new IllegalArgumentException("La posición no pertenece a este chunk");
        }
    }

    private static Position requirePosition(Position position) {
        return Objects.requireNonNull(position, "position no puede ser null");
    }
}
