package domain.world;

import domain.Position;
import domain.block.Block;
import patterns.observer.Observer;
import patterns.observer.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Agregado principal del mundo y origen de notificaciones de cambios de bloques. */
public final class World implements Subject<BlockChange> {
    private final String name;
    private final List<Chunk> chunks = new ArrayList<>();
    private final List<Observer<BlockChange>> observers = new ArrayList<>();

    public World(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del mundo no puede estar vacío");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Chunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    public void addChunk(Chunk chunk) {
        chunks.add(Objects.requireNonNull(chunk, "chunk no puede ser null"));
    }

    public Optional<Chunk> findChunk(int chunkX, int chunkZ) {
        return chunks.stream()
                .filter(chunk -> chunk.getChunkX() == chunkX && chunk.getChunkZ() == chunkZ)
                .findFirst();
    }

    /** Busca el chunk que contiene una posición absoluta del mundo. */
    public Optional<Chunk> findChunk(Position position) {
        return findChunk(Chunk.chunkXFor(position), Chunk.chunkZFor(position));
    }

    public void placeBlock(int chunkX, int chunkZ, Block block) {
        Chunk chunk = requireChunk(chunkX, chunkZ);
        chunk.put(Objects.requireNonNull(block, "block no puede ser null"));
        notifyObservers(new BlockChange(this, block, BlockChange.Type.PLACED));
    }

    public Optional<Block> removeBlock(int chunkX, int chunkZ, Position position) {
        Chunk chunk = requireChunk(chunkX, chunkZ);
        Block removed = chunk.remove(position);
        if (removed != null) {
            notifyObservers(new BlockChange(this, removed, BlockChange.Type.REMOVED));
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public void addObserver(Observer<BlockChange> observer) {
        observers.add(Objects.requireNonNull(observer, "observer no puede ser null"));
    }

    @Override
    public void removeObserver(Observer<BlockChange> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(BlockChange change) {
        List.copyOf(observers).forEach(observer -> observer.update(change));
    }

    private Chunk requireChunk(int chunkX, int chunkZ) {
        return findChunk(chunkX, chunkZ)
                .orElseThrow(() -> new IllegalArgumentException("El chunk solicitado no existe"));
    }
}
