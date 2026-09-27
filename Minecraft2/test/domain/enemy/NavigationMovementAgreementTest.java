package domain.enemy;

import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavigationMovementAgreementTest {
    @Test void lowCeilingAboveLowerHalfRejectsStepBeforePathPlanning() {
        var world = TestWorlds.flat(1, 1);
        int y = TestWorlds.GROUND_Y;
        TestWorlds.place(world, 4, y + 1, 2, BlockType.STONE);
        TestWorlds.place(world, 3, y + 3, 2, BlockType.STONE);
        var grid = new NavigationGrid(world);
        var start = new NavigationNode(3, y, 2);
        var step = new NavigationNode(4, y + 1, 2);
        assertTrue(grid.isWalkable(start));
        assertTrue(grid.isWalkable(step));
        assertFalse(grid.neighbors(start).contains(step),
                "The 0.6-wide body rises while still overlapping the low ceiling column");
    }
}
