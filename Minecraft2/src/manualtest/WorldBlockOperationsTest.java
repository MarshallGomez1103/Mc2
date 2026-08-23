package manualtest;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

/** Comprobación manual y simple de las operaciones básicas de bloques. */
public final class WorldBlockOperationsTest {
    private WorldBlockOperationsTest() {
    }

    public static void main(String[] args) {
        BlockFactory blockFactory = new BlockFactory();
        World world = new World("test-world");
        Chunk chunk = new Chunk(0, 0);
        Position position = new Position(2, 20, 3);
        world.addChunk(chunk);

        verify(world.findChunk(position).isPresent(), "No se encontró el chunk creado");

        Block block = blockFactory.create(BlockType.GRASS, position);
        world.placeBlock(0, 0, block);
        verify(chunk.getBlock(position).orElse(null) == block, "No se colocó el bloque");

        verify(world.removeBlock(0, 0, position).orElse(null) == block, "No se eliminó el bloque");
        verify(chunk.getBlock(position).isEmpty(), "El bloque sigue en el chunk");
        verify(world.removeBlock(0, 0, position).isEmpty(), "Eliminar aire debe devolver vacío");

        System.out.println("Prueba de bloques superada: crear, consultar, colocar y eliminar.");
    }

    private static void verify(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
