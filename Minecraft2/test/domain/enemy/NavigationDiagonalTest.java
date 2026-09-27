package domain.enemy;

import domain.block.BlockType;
import org.junit.jupiter.api.Test;

import static domain.enemy.TestWorlds.GROUND_Y;
import static domain.enemy.TestWorlds.ground;
import static org.junit.jupiter.api.Assertions.*;

class NavigationDiagonalTest {
    @Test void openTerrainRoutesDirectlyFromTopLeftToCentre() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var path = new AStarPathfinder().findPath(grid, ground(1, 1), ground(2, 2)).orElseThrow();
        assertEquals(2, path.size(), "1 → 5 ocupa una sola diagonal");
        assertEquals(ground(2, 2), path.goal());
        assertEquals(Math.sqrt(2), ground(1, 1).octileTo(ground(2, 2)), 1e-12);
    }

    @Test void longOpenRouteUsesDiagonalsThenStraightSteps() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var start = ground(1, 1);
        var goal = ground(9, 5);
        var path = new AStarPathfinder().findPath(grid, start, goal).orElseThrow();
        assertEquals(9, path.size());
        double cost = 0;
        for (int i = 1; i < path.size(); i++) {
            var before = path.nodes().get(i - 1);
            var after = path.nodes().get(i);
            cost += Math.hypot(after.x() - before.x(), after.z() - before.z());
        }
        assertEquals(4 + 4 * Math.sqrt(2), cost, 1e-12);
        assertEquals(cost, start.octileTo(goal), 1e-12);
    }

    @Test void diagonalCannotCutEitherBlockedCorner() {
        for (boolean xSide : new boolean[]{true, false}) {
            var world = TestWorlds.flat(1, 1);
            TestWorlds.wall(world, xSide ? 5 : 4, xSide ? 4 : 5);
            var grid = new NavigationGrid(world);
            assertFalse(grid.neighbors(ground(4, 4)).contains(ground(5, 5)));
        }
    }

    @Test void diagonalCannotCrossUnsupportedSideColumn() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.remove(world, 5, GROUND_Y, 4);
        var grid = new NavigationGrid(world);
        assertFalse(grid.neighbors(ground(4, 4)).contains(ground(5, 5)));
    }

    @Test void diagonalClimbRespectsCeilingAboveSideColumn() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 5, GROUND_Y + 1, 5, BlockType.STONE);
        TestWorlds.place(world, 5, GROUND_Y + 3, 4, BlockType.STONE);
        var grid = new NavigationGrid(world);
        var target = new NavigationNode(5, GROUND_Y + 1, 5);
        assertTrue(grid.isWalkable(target));
        assertFalse(grid.neighbors(ground(4, 4)).contains(target));
    }

    @Test void raisedSideColumnDoesNotBecomeHiddenDiagonalStep() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 5, GROUND_Y + 1, 4, BlockType.STONE);
        var grid = new NavigationGrid(world);
        assertFalse(grid.neighbors(ground(4, 4)).contains(ground(5, 5)),
                "la ruta plana no debe omitir una subida lateral real");
    }

    @Test void diagonalCannotClimbTwoBlocks() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 5, GROUND_Y + 1, 5, BlockType.STONE);
        TestWorlds.place(world, 5, GROUND_Y + 2, 5, BlockType.STONE);
        var grid = new NavigationGrid(world);
        assertFalse(grid.neighbors(ground(4, 4)).contains(new NavigationNode(5, GROUND_Y + 2, 5)));
    }

    @Test void movementCompletesEveryDiagonalOnFlatTerrainAtConstantSpeed() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var zombie = new Zombie(2.5, GROUND_Y + 1, 2.5, 3);
        var path = new AStarPathfinder().findPath(grid, ground(2, 2), ground(10, 10)).orElseThrow();
        var movement = new ZombieMovement();
        for (int frame = 0; frame < 60; frame++) {
            assertTrue(movement.follow(zombie, path, grid, 2, 1.0 / 60));
        }
        assertEquals(2, Math.hypot(zombie.getX() - 2.5, zombie.getZ() - 2.5), 1e-9,
                "diagonal no multiplica la velocidad por sqrt(2)");
        finish(movement, zombie, path, grid);
        assertEquals(10.5, zombie.getX(), 1e-9);
        assertEquals(10.5, zombie.getZ(), 1e-9);
    }

    @Test void plannedDiagonalClimbMatchesActualBodyMovement() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 5, GROUND_Y + 1, 5, BlockType.STONE);
        var grid = new NavigationGrid(world);
        var zombie = new Zombie(4.5, GROUND_Y + 1, 4.5, 3);
        var goal = new NavigationNode(5, GROUND_Y + 1, 5);
        var path = new AStarPathfinder().findPath(grid, ground(4, 4), goal).orElseThrow();
        assertEquals(2, path.size());
        finish(new ZombieMovement(), zombie, path, grid);
        assertEquals(goal.feetY(), zombie.getY());
    }

    @Test void plannedDiagonalDropMatchesActualBodyMovement() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.remove(world, 5, GROUND_Y, 5);
        TestWorlds.place(world, 5, GROUND_Y - 3, 5, BlockType.STONE);
        var grid = new NavigationGrid(world);
        var zombie = new Zombie(4.5, GROUND_Y + 1, 4.5, 3);
        var goal = new NavigationNode(5, GROUND_Y - 3, 5);
        var path = new AStarPathfinder().findPath(grid, ground(4, 4), goal).orElseThrow();
        assertEquals(2, path.size());
        finish(new ZombieMovement(), zombie, path, grid);
        assertEquals(goal.feetY(), zombie.getY());
    }

    @Test void newlyBlockedSideOfDiagonalStopsBodyBeforeCorner() {
        var world = TestWorlds.flat(1, 1);
        var grid = new NavigationGrid(world);
        var path = new AStarPathfinder().findPath(grid, ground(4, 4), ground(5, 5)).orElseThrow();
        var zombie = new Zombie(4.5, GROUND_Y + 1, 4.5, 3);
        TestWorlds.wall(world, 5, 4);
        var movement = new ZombieMovement();
        boolean blocked = false;
        for (int frame = 0; frame < 100 && !blocked; frame++) {
            blocked = !movement.follow(zombie, path, grid, 2, 1.0 / 60);
        }
        assertTrue(blocked, "un cambio de bloques invalida la diagonal en tiempo real");
        assertTrue(zombie.getX() + Zombie.WIDTH / 2 <= 5.0 + 1e-9);
        assertFalse(path.isFinished());
    }

    private static void finish(ZombieMovement movement, Zombie zombie, Path path, NavigationGrid grid) {
        for (int frame = 0; frame < 1000 && !path.isFinished(); frame++) {
            assertTrue(movement.follow(zombie, path, grid, 2, 1.0 / 60),
                    "cada transición planificada debe ser ejecutable por el cuerpo completo");
        }
        assertTrue(path.isFinished());
        for (int frame = 0; frame < 90; frame++) movement.advancePhysics(zombie, grid, 1.0 / 60);
    }
}
