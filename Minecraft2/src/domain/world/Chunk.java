package domain.world;

import domain.Position;
import domain.block.Block;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Contenedor mínimo de bloques. El tamaño y los límites se definirán en el MVP. */
public final class Chunk {
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

    public Collection<Block> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public Optional<Block> getBlock(Position position) {
        return Optional.ofNullable(blocks.get(position));
    }

    Block put(Block block) {
        return blocks.put(block.getPosition(), block);
    }

    Block remove(Position position) {
        return blocks.remove(position);
    }
}
