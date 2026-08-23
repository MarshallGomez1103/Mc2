package application;

import domain.world.SimpleTerrainGenerator;
import domain.world.World;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.WorldStorage;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Casos de uso relacionados con la administración de mundos.
 * No contiene entrada/salida de la interfaz de usuario.
 */
public final class WorldApplicationService {
    /** Lado de la cuadrícula de chunks generada al crear un mundo: 2 × 2 = 4 chunks. */
    private static final int DEFAULT_WORLD_CHUNKS = 2;

    private final WorldStorage storage;
    private final BlockFactory blockFactory;
    private final WorldManager worldManager;
    private final Random seedGenerator = new Random();

    public WorldApplicationService(WorldStorage storage, BlockFactory blockFactory) {
        this.storage = Objects.requireNonNull(storage, "storage no puede ser null");
        this.blockFactory = Objects.requireNonNull(blockFactory, "blockFactory no puede ser null");
        this.worldManager = WorldManager.getInstance();
    }

    public void createWorld(String name) throws IOException {
        long seed = seedGenerator.nextLong();
        World world = new World(name, seed, Instant.now().truncatedTo(ChronoUnit.SECONDS));

        // El generador depende de la semilla, así que se construye por mundo y no por servicio.
        ChunkGenerationService generationService =
                new ChunkGenerationService(new SimpleTerrainGenerator(seed), blockFactory);
        generationService.generateChunks(DEFAULT_WORLD_CHUNKS, DEFAULT_WORLD_CHUNKS)
                .forEach(world::addChunk);

        storage.create(world);
        worldManager.load(world);
    }

    public List<String> listWorlds() throws IOException {
        return storage.list();
    }

    public void loadWorld(String name) throws IOException {
        World world = storage.read(name);
        worldManager.load(world);
    }

    public void saveCurrentWorld() throws IOException {
        World current = worldManager.getCurrentWorld()
                .orElseThrow(() -> new IllegalStateException("No hay un mundo cargado"));
        storage.update(current);
    }

    public void deleteWorld(String name) throws IOException {
        storage.delete(name);
        worldManager.getCurrentWorld()
                .filter(world -> world.getName().equals(name))
                .ifPresent(world -> worldManager.unload());
    }
}
