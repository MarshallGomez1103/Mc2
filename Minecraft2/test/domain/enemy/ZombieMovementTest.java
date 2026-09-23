package domain.enemy;

import domain.block.BlockType;
import domain.world.World;
import org.junit.jupiter.api.Test;

import static domain.enemy.TestWorlds.GROUND_Y;
import static domain.enemy.TestWorlds.ground;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieMovementTest {
    private static final double SPEED = 2.0;
    private static final double DELTA = 1.0 / 60.0;

    private final World world = TestWorlds.flat(1, 1);
    private final NavigationGrid grid = new NavigationGrid(world);
    private final AStarPathfinder pathfinder = new AStarPathfinder();
    private final ZombieMovement movement = new ZombieMovement();

    @Test
    void followsEveryWaypointUntilTheGoalCenter() {
        Zombie zombie = new Zombie(2.5, GROUND_Y + 1, 2.5, 3);
        Path path = pathfinder.findPath(grid, ground(2, 2), ground(6, 2)).orElseThrow();
        int frames = 0;
        while (!path.isFinished() && frames < 600) {
            assertTrue(movement.follow(zombie, path, grid, SPEED, DELTA));
            frames++;
        }
        assertTrue(path.isFinished());
        assertEquals(6.5, zombie.getX(), ZombieMovement.ARRIVE_DISTANCE + 1e-9);
        assertEquals(2.5, zombie.getZ(), 1e-6);
        assertEquals(GROUND_Y + 1, zombie.getY());
        assertTrue(frames > 100 && frames < 160, "4 bloques a 2 b/s tardan ~2 s: " + frames);
    }

    @Test
    void blockPlacedAfterPlanningStopsTheZombieAndReportsInvalidPath() {
        Zombie zombie = new Zombie(2.5, GROUND_Y + 1, 2.5, 3);
        Path path = pathfinder.findPath(grid, ground(2, 2), ground(6, 2)).orElseThrow();
        TestWorlds.wall(world, 4, 2);
        boolean blocked = false;
        for (int frame = 0; frame < 600 && !blocked; frame++) {
            blocked = !movement.follow(zombie, path, grid, SPEED, DELTA);
        }
        assertTrue(blocked, "el paso hacia la pared debe rechazarse");
        assertTrue(zombie.getX() + Zombie.WIDTH / 2 <= 4.0 + 1e-9, "el cuerpo no entra en la columna x=4");
        assertFalse(path.isFinished());
    }

    @Test
    void climbsOneBlockStepAndDropsBackDown() {
        TestWorlds.place(world, 4, GROUND_Y + 1, 2, BlockType.STONE);
        Zombie zombie = new Zombie(3.5, GROUND_Y + 1, 2.5, 3);
        Path path = pathfinder.findPath(grid, ground(3, 2), ground(5, 2)).orElseThrow();
        boolean climbed = false;
        for (int frame = 0; frame < 600 && !path.isFinished(); frame++) {
            assertTrue(movement.follow(zombie, path, grid, SPEED, DELTA));
            if (zombie.getY() == GROUND_Y + 2) {
                climbed = true;
            }
        }
        assertTrue(climbed, "debe pasar por encima del escalón");
        assertEquals(GROUND_Y + 1, zombie.getY(), "vuelve al suelo al bajar");
    }
}
