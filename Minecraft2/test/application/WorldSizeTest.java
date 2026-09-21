package application;

import domain.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.WorldStorage;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSizeTest {
    @AfterEach
    void unload() {
        WorldManager.getInstance().unload();
    }

    @Test
    void sizesRemainBoundedForSnapshotStorage() {
        assertEquals(4, WorldSize.SMALL.totalChunks());
        assertEquals(100, WorldSize.MEDIUM.totalChunks());
        assertEquals(256, WorldSize.LARGE.totalChunks());
    }

    @Test
    void mediumWorldCreationUsesSelectedSizeAndCentersSpawn() throws IOException {
        RecordingStorage storage = new RecordingStorage();
        WorldApplicationService service = new WorldApplicationService(storage, new BlockFactory());
        service.createWorld("medium", WorldSize.MEDIUM);
        World created = storage.world;
        assertEquals(100, created.getChunks().size());
        assertTrue(created.getPlayer().getX() >= 72 && created.getPlayer().getX() < 88);
        assertTrue(created.getPlayer().getZ() >= 72 && created.getPlayer().getZ() < 88);
        assertThrows(IllegalStateException.class,
                () -> service.createWorld("medium", WorldSize.LARGE),
                "un nombre repetido se rechaza antes de generar 256 chunks innecesarios");
    }

    private static final class RecordingStorage implements WorldStorage {
        private World world;

        @Override public void create(World created) { world = created; }
        @Override public World read(String id) { return world; }
        @Override public void update(World updated) { world = updated; }
        @Override public void delete(String id) { world = null; }
        @Override public List<String> list() { return world == null ? List.of() : List.of(world.getId()); }
    }
}
