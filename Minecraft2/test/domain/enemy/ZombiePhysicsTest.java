package domain.enemy;

import domain.block.BlockType;
import domain.player.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ZombiePhysicsTest {
    @Test void jumpUsesPlayerSpeedAndGravityWithoutTeleporting() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var z = new Zombie(4.5, 21, 4.5, 3);
        var physics = new ZombiePhysics();
        physics.jump(z, grid);
        assertEquals(Player.JUMP_SPEED, z.velocityY());
        assertEquals(21, z.getY());
        physics.advance(z, grid, 0.05);
        assertEquals(Player.JUMP_SPEED + Player.GRAVITY * 0.05, z.velocityY(), 1e-9);
        assertTrue(z.getY() > 21 && z.getY() < 21.4);
        assertFalse(z.isOnGround());
    }

    @Test void secondJumpInAirIsRejectedAndJumpReturnsToFloor() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var z = new Zombie(4.5, 21, 4.5, 3);
        var physics = new ZombiePhysics();
        physics.jump(z, grid);
        physics.advance(z, grid, 0.2);
        double velocity = z.velocityY();
        physics.jump(z, grid);
        assertEquals(velocity, z.velocityY());
        for (int i = 0; i < 60; i++) physics.advance(z, grid, 1.0 / 60);
        assertEquals(21, z.getY(), 1e-6);
        assertTrue(z.isOnGround());
        assertTrue(z.isAlive());
    }

    @Test void lowCeilingCancelsJumpWithoutEnteringSolidBlocks() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 4, 23, 4, BlockType.STONE);
        var grid = new NavigationGrid(world);
        var z = new Zombie(4.5, 21, 4.5, 3);
        var physics = new ZombiePhysics();
        physics.jump(z, grid);
        for (int i = 0; i < 60; i++) {
            physics.advance(z, grid, 1.0 / 60);
            assertTrue(z.getY() + z.height() <= 23 + 1e-5);
        }
        assertEquals(21, z.getY(), 1e-6);
    }

    @Test void deepLandingShattersAtFloorOnlyAndNeverTunnels() {
        var grid = new NavigationGrid(TestWorlds.flat(1, 1));
        var z = new Zombie(4.5, 31, 4.5, 3);
        z.setOnGround(false);
        var physics = new ZombiePhysics(new KamikazePolicy(() -> 0.8));
        physics.advance(z, grid, 0.1);
        assertTrue(z.getY() < 31 && z.getY() > 30.8);
        assertFalse(z.isShattered());
        physics.advance(z, grid, 2);
        assertFalse(z.isAlive());
        assertTrue(z.isShattered());
        assertEquals(21, z.getY(), 1e-6);
    }

    @Test void exactlyThirtyPercentBoundaryIsUsedForDeepLanding() {
        var survivor = new Zombie(4.5, 21, 4.5, 3);
        new KamikazePolicy(() -> 0.299999).onLanding(survivor, 8);
        assertTrue(survivor.isCrawler());
        assertTrue(survivor.isAlive());
        assertTrue(survivor.isKamikaze());
        assertEquals(0.8, survivor.height());
        var broken = new Zombie(4.5, 21, 4.5, 3);
        new KamikazePolicy(() -> 0.3).onLanding(broken, 8);
        assertTrue(broken.isShattered());
    }

    @Test void safeFallsDoNotDrawRandomOrRemoveLegs() {
        var calls = new AtomicInteger();
        var z = new Zombie(4.5, 21, 4.5, 3);
        new KamikazePolicy(() -> { calls.incrementAndGet(); return 0; }).onLanding(z, 7.9);
        assertEquals(0, calls.get());
        assertFalse(z.isCrawler());
        assertTrue(z.isAlive());
    }

    @Test void crawlerFitsOneBlockTunnelAndCannotJump() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 4, 22, 4, BlockType.STONE);
        var normalGrid = new NavigationGrid(world);
        var lowGrid = new NavigationGrid(world, 1, 0);
        assertFalse(normalGrid.isWalkable(TestWorlds.ground(4, 4)));
        assertTrue(lowGrid.isWalkable(TestWorlds.ground(4, 4)));
        var z = new Zombie(4.5, 21, 4.5, 3);
        z.becomeCrawler();
        var physics = new ZombiePhysics();
        assertTrue(physics.isBodyClear(z, lowGrid, 4.5, 21, 4.5));
        physics.jump(z, lowGrid);
        assertEquals(0, z.velocityY());
    }

    @Test void voidFallEventuallyShattersAndDoesNotHover() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.remove(world, 4, 20, 4);
        var grid = new NavigationGrid(world);
        var z = new Zombie(4.5, 21, 4.5, 3);
        var physics = new ZombiePhysics();
        physics.advance(z, grid, 3);
        assertTrue(z.isShattered());
        assertTrue(z.getY() < -8);
    }

    @Test void consecutiveMountainStepsRequireRealAirTimeAndLanding() {
        var world = TestWorlds.flat(1, 1);
        for (int x = 4; x <= 6; x++) for (int zz = 0; zz < 16; zz++) {
            for (int y = 21; y <= 21 + x - 4; y++) TestWorlds.place(world, x, y, zz, BlockType.STONE);
        }
        var grid = new NavigationGrid(world);
        var zombie = new Zombie(3.5, 21, 5.5, 3);
        var path = new AStarPathfinder().findPath(grid, TestWorlds.ground(3, 5),
                new NavigationNode(6, 23, 5)).orElseThrow();
        var movement = new ZombieMovement();
        int frames = 0;
        double previousY = zombie.getY();
        while (!path.isFinished() && frames < 600) {
            assertTrue(movement.follow(zombie, path, grid, 4.3, 1.0 / 60));
            assertTrue(Math.abs(zombie.getY() - previousY) < 0.14, "sin escalones verticales instantáneos");
            previousY = zombie.getY();
            frames++;
        }
        assertTrue(path.isFinished());
        assertTrue(frames > 120, "tres saltos no equivalen a caminar tres bloques: " + frames);
    }

    @Test void damageFlashStartsAtPoint18AndDecaysInSimulationTime() {
        var z = new Zombie(4.5, 21, 4.5, 3);
        z.takeDamage(1);
        assertEquals(0.18, z.hitFlashSeconds());
        z.advanceFlash(0.1);
        assertEquals(0.08, z.hitFlashSeconds(), 1e-9);
        z.advanceFlash(0.1);
        assertEquals(0, z.hitFlashSeconds());
    }
}
