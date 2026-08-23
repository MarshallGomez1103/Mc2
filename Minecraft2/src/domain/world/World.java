package domain.world;

import domain.Position;
import domain.block.Block;
import domain.player.Player;
import patterns.observer.Observer;
import patterns.observer.Subject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado principal del mundo y origen de notificaciones de cambios de bloques.
 * Conserva además la identidad, la semilla, la fecha de creación y el jugador que exige
 * el esquema JSON acordado en docs/decisiones-compartidas.md.
 */
public final class World implements Subject<BlockChange> {
    /** Posición inicial del jugador; queda por encima de la superficie generada (18 a 22). */
    public static final Position DEFAULT_SPAWN = new Position(8, 32, 8);

    private static final String ID_PATTERN = "[a-zA-Z0-9_-]+";

    private final String id;
    private final String name;
    private final long seed;
    private final Instant createdAt;
    private final Player player;
    private final List<Chunk> chunks = new ArrayList<>();
    private final List<Observer<BlockChange>> observers = new ArrayList<>();

    /** Constructor completo; lo usa la persistencia al reconstruir un mundo guardado. */
    public World(String id, String name, long seed, Instant createdAt, Player player) {
        if (id == null || !id.matches(ID_PATTERN)) {
            throw new IllegalArgumentException(
                    "El identificador solo puede contener letras, números, guion y guion bajo"
            );
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del mundo no puede estar vacío");
        }
        this.id = id;
        this.name = name;
        this.seed = seed;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser null");
        this.player = Objects.requireNonNull(player, "player no puede ser null");
    }

    /** Constructor de conveniencia para mundos nuevos: el identificador coincide con el nombre. */
    public World(String name, long seed, Instant createdAt) {
        this(name, name, seed, createdAt, new Player(DEFAULT_SPAWN));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSeed() {
        return seed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Player getPlayer() {
        return player;
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
