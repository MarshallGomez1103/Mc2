package domain.world;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldBlockOperationsTest {
    private final BlockFactory blockFactory = new BlockFactory();

    @Test
    void findsAChunkCreatedForAPosition() {
        World world = worldWithOriginChunk();

        assertTrue(world.findChunk(new Position(15, 20, 15)).isPresent());
    }

    @Test
    void placesABlockInTheCorrespondingChunk() {
        World world = worldWithOriginChunk();
        Position position = new Position(2, 20, 3);
        Block block = blockFactory.create(BlockType.GRASS, position);

        world.placeBlock(0, 0, block);

        assertSame(block, world.findChunk(0, 0).orElseThrow().getBlock(position).orElseThrow());
    }

    @Test
    void removesAnExistingBlock() {
        World world = worldWithOriginChunk();
        Position position = new Position(2, 20, 3);
        Block block = blockFactory.create(BlockType.STONE, position);
        world.placeBlock(0, 0, block);

        Block removed = world.removeBlock(0, 0, position).orElseThrow();

        assertSame(block, removed);
        assertTrue(world.findChunk(0, 0).orElseThrow().getBlock(position).isEmpty());
    }

    @Test
    void returnsEmptyWhenRemovingAir() {
        World world = worldWithOriginChunk();

        assertTrue(world.removeBlock(0, 0, new Position(2, 20, 3)).isEmpty());
    }

    private World worldWithOriginChunk() {
        World world = new World("test-world");
        world.addChunk(new Chunk(0, 0));
        return world;
    }
}
