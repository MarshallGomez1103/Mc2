package application;

import domain.world.Chunk;
import domain.world.World;
import patterns.singleton.WorldManager;
import persistence.WorldStorage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Casos de uso relacionados con la administración de mundos.
 * No contiene entrada/salida de la interfaz de usuario.
 */
public final class WorldApplicationService {
    private final WorldStorage storage;
    private final WorldManager worldManager;

    public WorldApplicationService(WorldStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage no puede ser null");
        this.worldManager = WorldManager.getInstance();
    }

    public void createWorld(String name) throws IOException {
        World world = new World(name);
        world.addChunk(new Chunk(0, 0));
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
