package patterns.singleton;

import domain.world.World;

import java.util.Optional;

/** Singleton: conserva la referencia al único mundo actualmente cargado. */
public final class WorldManager {
    private World currentWorld;

    private WorldManager() {
    }

    private static final class InstanceHolder {
        private static final WorldManager INSTANCE = new WorldManager();
    }

    public static WorldManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public Optional<World> getCurrentWorld() {
        return Optional.ofNullable(currentWorld);
    }

    public void load(World world) {
        currentWorld = java.util.Objects.requireNonNull(world, "world no puede ser null");
    }

    public void unload() {
        currentWorld = null;
    }
}
