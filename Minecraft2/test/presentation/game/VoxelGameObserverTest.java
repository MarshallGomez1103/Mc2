package presentation.game;

import application.PlayerInteractionService;
import domain.Position;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprueba que la vista 3D se entera de los cambios de bloques a través del patrón Observer.
 *
 * <p>Esta parte se puede probar sin gráficos: construir {@link VoxelGame} y recibir un
 * {@code BlockChange} no toca OpenGL, solo resuelve a qué chunk pertenece el bloque cambiado y lo
 * marca para reconstruir. Lo que sí exige una ventana —que la malla se redibuje— queda cubierto
 * por la prueba manual de {@code docs/evidencias/}.
 */
@DisplayName("La vista 3D reacciona a los cambios de bloques (Observer)")
class VoxelGameObserverTest {
    private static final BlockFactory FACTORY = new BlockFactory();

    private World worldWithOneChunk() {
        World world = new World("mundo_observer", "mundo_observer", 1L,
                Instant.parse("2026-08-20T20:30:00Z"), new Player(8.5, 20.0, 8.5));
        Chunk chunk = new Chunk(0, 0);
        chunk.addBlock(FACTORY.create(BlockType.STONE, new Position(4, 10, 4)));
        world.addChunk(chunk);
        return world;
    }

    @Test
    @DisplayName("colocar y eliminar marcan el chunk afectado para reconstruir")
    void marcaElChunkAfectado() {
        World world = worldWithOneChunk();
        VoxelGame game = new VoxelGame(world, new PlayerInteractionService());
        world.addObserver(game);

        assertEquals(0, game.pendingRebuildCount(), "no hay nada pendiente al empezar");

        world.placeBlock(0, 0, FACTORY.create(BlockType.WOOD, new Position(5, 11, 5)));
        assertEquals(1, game.pendingRebuildCount(), "colocar debe marcar su chunk");

        world.removeBlock(0, 0, new Position(4, 10, 4));
        assertEquals(1, game.pendingRebuildCount(),
                "eliminar en el mismo chunk no debe duplicar la reconstrucción");
    }

    @Test
    @DisplayName("eliminar aire no genera trabajo de reconstrucción")
    void eliminarAireNoMarcaNada() {
        World world = worldWithOneChunk();
        VoxelGame game = new VoxelGame(world, new PlayerInteractionService());
        world.addObserver(game);

        world.removeBlock(0, 0, new Position(9, 30, 9));

        assertEquals(0, game.pendingRebuildCount(),
                "World no notifica al eliminar aire, así que no hay nada que redibujar");
    }

    @Test
    void cambiarBloqueEnBordeMarcaLasMallasVecinas() {
        World world = worldWithOneChunk();
        world.addChunk(new Chunk(1, 0));
        world.addChunk(new Chunk(0, 1));
        VoxelGame game = new VoxelGame(world, new PlayerInteractionService());
        world.addObserver(game);

        world.placeBlock(0, 0, FACTORY.create(BlockType.WOOD, new Position(15, 20, 15)));

        assertEquals(3, game.pendingRebuildCount(),
                "cambia la cara del chunk propio y de sus dos vecinos ortogonales");
    }
}
