package domain.enemy;

import domain.block.BlockType;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static domain.enemy.TestWorlds.GROUND_Y;
import static domain.enemy.TestWorlds.ground;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AStarPathfinderTest {
    private final World world = TestWorlds.flat(1, 1);
    private final NavigationGrid grid = new NavigationGrid(world);
    private final AStarPathfinder pathfinder = new AStarPathfinder();

    private static void assertConsecutiveNodesAreAdjacent(Path path) {
        List<NavigationNode> nodes = path.nodes();
        for (int i = 1; i < nodes.size(); i++) {
            assertEquals(1, nodes.get(i - 1).manhattanTo(nodes.get(i)), "waypoints no adyacentes en " + i);
            assertTrue(Math.abs(nodes.get(i).groundY() - nodes.get(i - 1).groundY()) <= NavigationGrid.MAX_DROP);
        }
    }

    @Test
    void straightLineHasOptimalLengthAndEndsAtGoal() {
        Path path = pathfinder.findPath(grid, ground(2, 5), ground(8, 5)).orElseThrow();
        assertEquals(7, path.size());
        assertEquals(ground(2, 5), path.nodes().get(0));
        assertEquals(ground(8, 5), path.goal());
        assertConsecutiveNodesAreAdjacent(path);
        assertEquals(1, pathfinder.getSearchCount());
    }

    @Test
    void wallForcesDetourThroughTheGap() {
        for (int z = 0; z < 14; z++) {
            TestWorlds.wall(world, 6, z);
        }
        Path path = pathfinder.findPath(grid, ground(2, 2), ground(10, 2)).orElseThrow();
        assertConsecutiveNodesAreAdjacent(path);
        assertTrue(path.nodes().stream().noneMatch(node -> node.x() == 6 && node.z() < 14), "no atraviesa la pared");
        assertTrue(path.nodes().stream().anyMatch(node -> node.x() == 6 && node.z() >= 14), "pasa por el hueco");
        assertTrue(path.size() > 9, "el rodeo es más largo que la recta");
    }

    @Test
    void enclosedGoalHasNoPathAndDoesNotFakeOne() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    TestWorlds.wall(world, 10 + dx, 10 + dz);
                }
            }
        }
        Optional<Path> path = pathfinder.findPath(grid, ground(2, 2), ground(10, 10));
        assertTrue(path.isEmpty());
        assertTrue(pathfinder.getLastExpansions() <= 16 * 16, "explora a lo sumo el chunk accesible");
    }

    @Test
    void startEqualsGoalReturnsSingleNodePath() {
        Path path = pathfinder.findPath(grid, ground(4, 4), ground(4, 4)).orElseThrow();
        assertEquals(1, path.size());
        assertEquals(0, pathfinder.getLastExpansions());
    }

    @Test
    void goalOnTwoBlockPillarIsUnreachableAndInvalidNodesAreRejected() {
        TestWorlds.place(world, 8, GROUND_Y + 1, 8, BlockType.STONE);
        TestWorlds.place(world, 8, GROUND_Y + 2, 8, BlockType.STONE);
        NavigationNode pillarTop = new NavigationNode(8, GROUND_Y + 2, 8);
        assertTrue(grid.isWalkable(pillarTop));
        assertTrue(pathfinder.findPath(grid, ground(2, 2), pillarTop).isEmpty(), "desnivel de 2 no es transitable");

        assertTrue(pathfinder.findPath(grid, ground(2, 2), new NavigationNode(5, GROUND_Y + 1, 5)).isEmpty(),
                "destino sin apoyo");
        assertTrue(pathfinder.findPath(grid, ground(-3, 2), ground(5, 5)).isEmpty(), "origen fuera de chunks");
    }

    @Test
    void oneBlockStepIsClimbedWithHigherCostThanFlatGround() {
        TestWorlds.place(world, 5, GROUND_Y + 1, 5, BlockType.STONE);
        Path path = pathfinder.findPath(grid, ground(3, 5), ground(7, 5)).orElseThrow();
        assertConsecutiveNodesAreAdjacent(path);
        assertEquals(5, path.size());
        assertTrue(path.nodes().contains(new NavigationNode(5, GROUND_Y + 1, 5)),
                "recta: 4 pasos + 1.0 de desnivel = 5.0; rodeo por z=4 o z=6: 6 pasos = 6.0");
    }

    @Test
    void twoBlockCliffIsAvoidedInFavorOfLongerFlatDetour() {
        TestWorlds.place(world, 5, GROUND_Y + 1, 5, BlockType.STONE);
        TestWorlds.place(world, 5, GROUND_Y + 2, 5, BlockType.STONE);
        Path path = pathfinder.findPath(grid, ground(3, 5), ground(7, 5)).orElseThrow();
        assertConsecutiveNodesAreAdjacent(path);
        assertEquals(7, path.size());
        assertFalse(path.nodes().stream().anyMatch(node -> node.x() == 5 && node.z() == 5));
    }

    @Test
    void expansionBudgetStopsSearchWithoutRoute() {
        AStarPathfinder limited = new AStarPathfinder(5);
        assertTrue(limited.findPath(grid, ground(0, 0), ground(15, 15)).isEmpty());
        assertEquals(6, limited.getLastExpansions());
    }
}
