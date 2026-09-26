package application;

import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardados que antes dejaban un archivo imposible de cargar o un mundo borrado aún en memoria. */
class SaveEdgeCasesTest {
    @TempDir
    Path worldsDirectory;

    private final BlockFactory blockFactory = new BlockFactory();

    @AfterEach
    void descargarMundoActual() {
        WorldManager.getInstance().unload();
    }

    private WorldApplicationService service() {
        WorldStorage storage = new JsonWorldStorage(worldsDirectory, blockFactory);
        return new WorldApplicationService(storage, blockFactory);
    }

    private static World current() {
        return WorldManager.getInstance().getCurrentWorld().orElseThrow();
    }

    @Test
    void playerFallingBelowTheWorldIsSavedOnTheGroundAndCanBeLoaded() throws IOException {
        WorldApplicationService service = service();
        service.createWorld("caida");
        Player player = current().getPlayer();
        double x = player.getX();
        double z = player.getZ();
        player.setY(-4.0);

        service.saveCurrentWorld();
        WorldManager.getInstance().unload();
        service.loadWorld("caida");

        Player loaded = current().getPlayer();
        assertEquals(x, loaded.getX(), "se conserva la columna");
        assertEquals(z, loaded.getZ());
        assertTrue(loaded.getY() >= 1 && loaded.getY() <= Chunk.HEIGHT, "y=" + loaded.getY());
    }

    @Test
    void playerFallingOutsideEveryColumnGoesBackToTheSpawnArea() throws IOException {
        WorldApplicationService service = service();
        service.createWorld("fuera");
        Player player = current().getPlayer();
        player.setX(-5.5);
        player.setZ(-5.5);
        player.setY(-7.0);

        service.saveCurrentWorld();
        WorldManager.getInstance().unload();
        service.loadWorld("fuera");

        Player loaded = current().getPlayer();
        assertTrue(loaded.getX() >= 0 && loaded.getX() <= 2 * Chunk.WIDTH, "x=" + loaded.getX());
        assertTrue(loaded.getZ() >= 0 && loaded.getZ() <= 2 * Chunk.DEPTH, "z=" + loaded.getZ());
        assertTrue(loaded.getY() >= 1 && loaded.getY() <= Chunk.HEIGHT, "y=" + loaded.getY());
    }

    @Test
    void deletingTheLoadedWorldWithOtherCaseUnloadsItWhenTheFileSystemIgnoresCase() throws IOException {
        WorldApplicationService service = service();
        service.createWorld("Mundo");
        boolean caseInsensitive = worldsDirectory.resolve("mundo.json").toFile().exists();

        service.deleteWorld(caseInsensitive ? "mundo" : "Mundo");

        assertEquals(List.of(), service.listWorlds());
        assertTrue(service.currentWorldName().isEmpty(), "el mundo borrado no puede quedar cargado");
    }
}
