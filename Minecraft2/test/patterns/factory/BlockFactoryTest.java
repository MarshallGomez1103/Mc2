package patterns.factory;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Hueco del Corte 1: la Factory crea cada tipo de bloque admitido con su posición. */
class BlockFactoryTest {
    private final BlockFactory factory = new BlockFactory();

    @ParameterizedTest
    @EnumSource(BlockType.class)
    void createsEveryBlockTypeAtTheRequestedPosition(BlockType type) {
        Position position = new Position(-3, 12, 40);

        Block block = factory.create(type, position);

        assertEquals(type, block.getType());
        assertEquals(position, block.getPosition());
    }

    @Test
    void eachCallCreatesANewBlock() {
        Position position = new Position(1, 2, 3);
        assertNotSame(factory.create(BlockType.SAND, position), factory.create(BlockType.SAND, position));
    }

    @Test
    void rejectsMissingData() {
        assertThrows(NullPointerException.class, () -> factory.create(null, new Position(0, 0, 0)));
        assertThrows(NullPointerException.class, () -> factory.create(BlockType.DIRT, null));
    }
}
