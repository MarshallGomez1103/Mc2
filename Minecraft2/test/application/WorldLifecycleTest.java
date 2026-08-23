package application;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CT-11 — el ciclo crear → generar → guardar → cerrar → cargar conserva los cambios de
 * colocar y eliminar bloques.
 *
 * <p>El menú de consola todavía no ofrece colocar ni eliminar bloques —eso llega con la
 * integración gráfica—, así que esa parte del criterio solo puede ejercitarse por código.
 */
@DisplayName("CT-11 · ciclo de vida completo de un mundo")
class WorldLifecycleTest {
    private final BlockFactory blockFactory = new BlockFactory();

    /** WorldManager es un Singleton con estado global: hay que dejarlo limpio entre pruebas. */
    @AfterEach
    void descargarMundoActual() {
        WorldManager.getInstance().unload();
    }

    @Test
    @DisplayName("crear → colocar/eliminar → guardar → cerrar → cargar → eliminar")
    void cicloCompletoConservaLosCambios(@TempDir Path worldsDirectory) throws Exception {
        WorldStorage storage = new JsonWorldStorage(worldsDirectory, blockFactory);
        WorldApplicationService service = new WorldApplicationService(storage, blockFactory);

        // 1. Crear: genera el terreno y lo guarda.
        service.createWorld("ct11_mundo");
        World created = currentWorld();
        long seed = created.getSeed();
        assertEquals("ct11_mundo", service.currentWorldName().orElseThrow());
        assertFalse(created.getChunks().isEmpty(), "crear un mundo debe generar terreno");

        // 2. Colocar un bloque en el aire y eliminar uno del terreno generado.
        Position placed = new Position(3, 40, 3);
        Position removed = anyBlockOf(created);
        created.placeBlock(0, 0, blockFactory.create(BlockType.WOOD, placed));
        assertTrue(created.removeBlock(Chunk.chunkXFor(removed), Chunk.chunkZFor(removed), removed)
                .isPresent(), "el bloque a eliminar debía existir");

        // 3. Guardar y cerrar.
        service.saveCurrentWorld();
        WorldManager.getInstance().unload();
        assertTrue(service.currentWorldName().isEmpty(), "el mundo quedó descargado");

        // 4. Cargar y comprobar que los cambios sobrevivieron.
        service.loadWorld("ct11_mundo");
        World reloaded = currentWorld();

        assertEquals("ct11_mundo", reloaded.getId());
        assertEquals(seed, reloaded.getSeed(), "la semilla debe sobrevivir al ciclo");
        assertEquals(created.getCreatedAt(), reloaded.getCreatedAt());
        assertEquals(BlockType.WOOD, typeAt(reloaded, placed), "el bloque colocado sigue ahí");
        assertNull(typeAt(reloaded, removed), "el bloque eliminado no reaparece");

        // 5. Eliminar el mundo: desaparece del listado y deja de estar cargado.
        service.deleteWorld("ct11_mundo");
        assertTrue(service.listWorlds().isEmpty());
        assertTrue(service.currentWorldName().isEmpty(),
                "borrar el mundo cargado también lo descarga");
    }

    private World currentWorld() {
        return WorldManager.getInstance().getCurrentWorld().orElseThrow();
    }

    /** Cualquier bloque ya generado del chunk de origen. */
    private static Position anyBlockOf(World world) {
        return world.findChunk(0, 0).orElseThrow()
                .getBlocks().iterator().next()
                .getPosition();
    }

    private static BlockType typeAt(World world, Position position) {
        return world.findChunk(position)
                .flatMap(chunk -> chunk.getBlock(position))
                .map(Block::getType)
                .orElse(null);
    }
}
