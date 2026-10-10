package application;

import domain.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.WorldStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Fallos del caso de uso aislados mediante un stub, sin disco ni red. */
class RecoveryWorldStorageFailureTest {
    @BeforeEach @AfterEach void unload() { WorldManager.getInstance().unload(); }
    @Test void readFailurePropagatesAndKeepsThePreviouslyLoadedWorld() {
        // Arrange
        World previous = new World("estable", 7, Instant.EPOCH);
        WorldManager.getInstance().load(previous);
        FaultStorage storage = new FaultStorage();
        var service = new WorldApplicationService(storage, new BlockFactory());
        // Act
        IOException error = assertThrows(IOException.class, () -> service.loadWorld("otro"));
        // Assert
        assertSame(storage.failure, error);
        assertSame(previous, service.currentWorld().orElseThrow());
    }
    @Test void saveFailurePropagatesWithoutUnloadingTheCurrentWorld() {
        // Arrange
        World current = new World("estable", 7, Instant.EPOCH);
        WorldManager.getInstance().load(current);
        FaultStorage storage = new FaultStorage();
        var service = new WorldApplicationService(storage, new BlockFactory());
        // Act
        IOException error = assertThrows(IOException.class, service::saveCurrentWorld);
        // Assert
        assertSame(storage.failure, error);
        assertSame(current, storage.updated);
        assertSame(current, service.currentWorld().orElseThrow());
    }
    private static final class FaultStorage implements WorldStorage {
        final IOException failure = new IOException("Fallo de almacenamiento simulado");
        World updated;
        public World read(String id) throws IOException { throw failure; }
        public void update(World world) throws IOException { updated = world; throw failure; }
        public void create(World world) { throw new UnsupportedOperationException(); }
        public void delete(String id) { throw new UnsupportedOperationException(); }
        public List<String> list() { return List.of(); }
    }
}
