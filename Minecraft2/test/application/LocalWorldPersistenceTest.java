package application;
import domain.Position;
import domain.block.BlockType;
import domain.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.JsonWorldStorage;
import persistence.InvalidWorldFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Reproducción del flujo local con un nuevo servicio y un archivo dañado en la misma carpeta. */
class LocalWorldPersistenceTest {
    @AfterEach void unload() { WorldManager.getInstance().unload(); }
    private WorldApplicationService service(Path dir) {
        BlockFactory f = new BlockFactory();
        return new WorldApplicationService(new JsonWorldStorage(dir,f),f);
    }
    @Test void creationAlreadyAppearsAndSavedEditsSurviveRestart(@TempDir Path dir) throws Exception {
        WorldApplicationService first = service(dir);
        first.createWorld("visible",WorldSize.SMALL);
        assertEquals(java.util.List.of("visible"),first.listWorlds());
        World original=first.currentWorld().orElseThrow();
        Position p=new Position(2,60,2);
        original.placeBlock(0,0,new BlockFactory().create(BlockType.WOOD,p));
        original.getPlayer().setYaw(42);
        first.saveCurrentWorld();
        WorldManager.getInstance().unload();
        WorldApplicationService restarted=service(dir);
        assertTrue(restarted.listWorlds().contains("visible")); restarted.loadWorld("visible");
        World loaded=restarted.currentWorld().orElseThrow();
        assertEquals(original.getSeed(),loaded.getSeed());
        assertEquals(42,loaded.getPlayer().getYaw());
        assertEquals(BlockType.WOOD,loaded.findChunk(p).orElseThrow().getBlock(p).orElseThrow().getType());
        try (var files = Files.list(dir)) {
            assertFalse(files.anyMatch(f->f.toString().endsWith(".tmp")));
        }
    }
    @Test void invalidFileIsVisibleAndLoadErrorKeepsCurrentWorld(@TempDir Path dir) throws Exception {
        WorldApplicationService s=service(dir); s.createWorld("bueno");
        World loaded=s.currentWorld().orElseThrow();
        Files.writeString(dir.resolve("roto.json"),"{\"name\":\"incompleto\"}");
        assertTrue(s.listWorlds().contains("roto"));
        assertThrows(InvalidWorldFileException.class,()->s.loadWorld("roto"));
        assertSame(loaded,s.currentWorld().orElseThrow());
        assertTrue(Files.exists(dir.resolve("roto.json")));
    }
    @Test void quittingWithoutSaveKeepsPreviousSnapshot(@TempDir Path dir) throws Exception {
        WorldApplicationService s=service(dir); s.createWorld("sin_guardar");
        Position p=new Position(2,60,2);
        s.currentWorld().orElseThrow().placeBlock(0,0,new BlockFactory().create(BlockType.WOOD,p));
        WorldManager.getInstance().unload();
        service(dir).loadWorld("sin_guardar");
        assertTrue(WorldManager.getInstance().getCurrentWorld().orElseThrow()
                .findChunk(p).orElseThrow().getBlock(p).isEmpty());
    }
}
