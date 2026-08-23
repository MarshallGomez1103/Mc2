package persistence;

import domain.Position;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.time.Instant;

/** Mundos de prueba compartidos por las pruebas de persistencia. */
final class WorldSamples {
    static final BlockFactory FACTORY = new BlockFactory();
    static final Instant CREATED_AT = Instant.parse("2026-08-20T20:30:00Z");
    static final long SEED = 48_271L;

    private WorldSamples() {
    }

    /**
     * Mundo con dos chunks —uno con coordenadas negativas— y varios tipos de bloque.
     * Es el "mundo de prueba con dos chunks y varios tipos" que pide CT-08.
     */
    static World twoChunkWorld(String id) {
        Player player = new Player(8.5, 32.25, -3.75);
        player.setYaw(123.5f);
        player.setPitch(-45.25f);
        World world = new World(id, id, SEED, CREATED_AT, player);

        Chunk origin = new Chunk(0, 0);
        origin.addBlock(FACTORY.create(BlockType.STONE, new Position(0, 0, 0)));
        origin.addBlock(FACTORY.create(BlockType.GRASS, new Position(15, 63, 15)));
        origin.addBlock(FACTORY.create(BlockType.WOOD, new Position(7, 20, 9)));
        world.addChunk(origin);

        // Chunk negativo: aquí es donde floorDiv/floorMod se ganan el sueldo.
        Chunk negative = new Chunk(-1, -1);
        negative.addBlock(FACTORY.create(BlockType.SAND, new Position(-1, 10, -1)));     // local (15, 10, 15)
        negative.addBlock(FACTORY.create(BlockType.LEAVES, new Position(-16, 5, -16)));  // local (0, 5, 0)
        negative.addBlock(FACTORY.create(BlockType.GRAVEL, new Position(-8, 30, -3)));   // local (8, 30, 13)
        world.addChunk(negative);

        return world;
    }
}
