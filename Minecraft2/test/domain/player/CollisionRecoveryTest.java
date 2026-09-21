package domain.player;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollisionRecoveryTest {
    @Test
    void playerAlreadyInsideOldSolidBlockIsLiftedToFirstFreeHeight() {
        Player player = new Player(8.5, 21, 8.5);
        player.setVelocityY(-4);
        World world = new World("recovery", "recovery", 1L,
                Instant.parse("2026-09-21T12:00:00Z"), player);
        Chunk chunk = new Chunk(0, 0);
        BlockFactory factory = new BlockFactory();
        for (int y = 20; y <= 24; y++) {
            chunk.addBlock(factory.create(BlockType.STONE, new Position(8, y, 8)));
        }
        world.addChunk(chunk);

        new CollisionResolver().resolveAndApply(player, world, 0, 0, 0);

        assertEquals(25, player.getY());
        assertEquals(0, player.getVelocityY());
    }
}
