package application;

import domain.Position;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInteractionPlacementTest {
    private final BlockFactory blocks = new BlockFactory();

    @Test
    void cannotPlaceBlockInsideOwnBody() {
        Player player = new Player(8.5, 20, 8.5);
        World world = worldWithTarget(player, 9);

        new PlayerInteractionService().placeBlockOfType(player, world, BlockType.WOOD);

        assertTrue(world.findChunk(0, 0).orElseThrow()
                .getBlock(new Position(8, 21, 8)).isEmpty());
    }

    @Test
    void canStillPlaceBlockInAdjacentFreeCell() {
        Player player = new Player(8.5, 20, 8.5);
        World world = worldWithTarget(player, 10);

        new PlayerInteractionService().placeBlockOfType(player, world, BlockType.WOOD);

        assertEquals(BlockType.WOOD, world.findChunk(0, 0).orElseThrow()
                .getBlock(new Position(8, 21, 9)).orElseThrow().getType());
    }

    private World worldWithTarget(Player player, int targetZ) {
        World world = new World("placement", "placement", 1L,
                Instant.parse("2026-09-21T12:00:00Z"), player);
        Chunk chunk = new Chunk(0, 0);
        chunk.addBlock(blocks.create(BlockType.STONE, new Position(8, 21, targetZ)));
        world.addChunk(chunk);
        return world;
    }
}
