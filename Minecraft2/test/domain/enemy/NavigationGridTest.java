package domain.enemy;

import domain.block.BlockType;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static domain.enemy.TestWorlds.GROUND_Y;
import static domain.enemy.TestWorlds.ground;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationGridTest {
    private final World world = TestWorlds.flat(2, 1);
    private final NavigationGrid grid = new NavigationGrid(world);

    @Test
    void flatColumnWithSupportAndBodySpaceIsWalkable() {
        assertTrue(grid.isWalkable(ground(5, 5)));
        assertFalse(grid.isWalkable(new NavigationNode(5, GROUND_Y + 1, 5)), "sin apoyo sólido");
        assertFalse(grid.isWalkable(new NavigationNode(5, GROUND_Y - 1, 5)), "el apoyo es aire");
    }

    @Test
    void missingChunkIsNeverWalkableEvenIfHeightLooksFine() {
        assertFalse(grid.chunkExists(-1, 0));
        assertFalse(grid.isWalkable(ground(-1, 5)));
        assertFalse(grid.isWalkable(ground(5, 16)), "el chunk (0,1) no existe");
        assertTrue(grid.nodeAt(-1, 5, GROUND_Y).isEmpty());
    }

    @Test
    void currentBlocksDecideWalkabilityAfterPlacingAndRemoving() {
        assertTrue(grid.isWalkable(ground(3, 3)));
        TestWorlds.place(world, 3, GROUND_Y + 2, 3, BlockType.WOOD);
        assertFalse(grid.isWalkable(ground(3, 3)), "un bloque a la altura de la cabeza bloquea el cuerpo");
        TestWorlds.remove(world, 3, GROUND_Y + 2, 3);
        assertTrue(grid.isWalkable(ground(3, 3)));
        TestWorlds.remove(world, 3, GROUND_Y, 3);
        assertFalse(grid.isWalkable(ground(3, 3)), "sin suelo no hay apoyo");
    }

    @Test
    void neighborsCrossChunkBorderAndSkipMissingChunks() {
        List<NavigationNode> border = grid.neighbors(ground(15, 5));
        assertTrue(border.contains(ground(16, 5)), "el vecino está en el chunk (1,0), que existe");
        assertEquals(4, border.size());

        List<NavigationNode> edge = grid.neighbors(ground(0, 0));
        assertEquals(2, edge.size(), "x=-1 y z=-1 caen en chunks ausentes");
    }

    @Test
    void climbOneDropThreeButNotMore() {
        TestWorlds.place(world, 6, GROUND_Y + 1, 5, BlockType.STONE);
        assertEquals(new NavigationNode(6, GROUND_Y + 1, 5), grid.nodeAt(6, 5, GROUND_Y).orElseThrow());

        TestWorlds.place(world, 7, GROUND_Y + 1, 5, BlockType.STONE);
        TestWorlds.place(world, 7, GROUND_Y + 2, 5, BlockType.STONE);
        assertTrue(grid.nodeAt(7, 5, GROUND_Y).isEmpty(), "subir dos bloques no está permitido");

        for (int y = GROUND_Y; y > GROUND_Y - 3; y--) {
            TestWorlds.remove(world, 8, y, 5);
        }
        TestWorlds.place(world, 8, GROUND_Y - 3, 5, BlockType.STONE);
        assertEquals(new NavigationNode(8, GROUND_Y - 3, 5), grid.nodeAt(8, 5, GROUND_Y).orElseThrow());

        TestWorlds.remove(world, 9, GROUND_Y, 5);
        TestWorlds.place(world, 9, GROUND_Y - 4, 5, BlockType.STONE);
        assertTrue(grid.nodeAt(9, 5, GROUND_Y).isEmpty(), "caer cuatro bloques excede MAX_DROP");
    }

    @Test
    void nodeUnderResolvesSupportBelowContinuousPosition() {
        assertEquals(ground(4, 4), grid.nodeUnder(4.5, GROUND_Y + 1, 4.5).orElseThrow());
        assertEquals(ground(4, 4), grid.nodeUnder(4.5, GROUND_Y + 3.7, 4.5).orElseThrow(), "en el aire, apoyo abajo");
        assertTrue(grid.nodeUnder(-0.5, GROUND_Y + 1, 4.5).isEmpty(), "fuera de chunks");
        TestWorlds.place(world, 4, GROUND_Y + 1, 4, BlockType.STONE);
        TestWorlds.place(world, 4, GROUND_Y + 2, 4, BlockType.STONE);
        assertEquals(new NavigationNode(4, GROUND_Y + 2, 4), grid.nodeUnder(4.5, GROUND_Y + 3, 4.5).orElseThrow());
    }
}
