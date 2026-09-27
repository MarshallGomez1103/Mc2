package application;

import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZombieTargetingTest {
    @Test void nearestAliveBoxWinsAndBodiesBehindOrBeyondReachDoNotCount() {
        World world = TestWorlds.flat(2, 2);
        Player player = new Player(5.5, 21, 5.5);
        ZombieTargeting targeting = new ZombieTargeting(world);
        Zombie near = new Zombie(5.5, 21, 9.5, 3), far = new Zombie(5.5, 21, 15.5, 3);
        Zombie dead = new Zombie(5.5, 21, 7.5, 3), behind = new Zombie(5.5, 21, 2.5, 3);
        dead.takeDamage(3);
        assertSame(near, targeting.findTarget(player, List.of(far, dead, behind, near), 12).orElseThrow());
        assertTrue(targeting.findTarget(player, List.of(near), 3).isEmpty());
        assertTrue(targeting.findTarget(player, List.of(dead, behind), 12).isEmpty());
    }
    @Test void crawlerHasLowHitboxAndCanBeShotWhenAimingDown() {
        World world = TestWorlds.flat(2, 2);
        Player player = new Player(5.5, 21, 5.5);
        Zombie crawler = new Zombie(5.5, 21, 9.5, 3);
        crawler.becomeCrawler();
        ZombieTargeting targeting = new ZombieTargeting(world);
        assertTrue(targeting.findTarget(player, List.of(crawler), 12).isEmpty());
        player.setPitch(-15);
        assertSame(crawler, targeting.findTarget(player, List.of(crawler), 12).orElseThrow());
    }
    @Test void exactDiagonalCannotShootThroughSideWallCorner() {
        World world = TestWorlds.flat(2, 2);
        Player player = new Player(5.5, 21, 5.5);
        player.setYaw(315);
        Zombie zombie = new Zombie(8.5, 21, 8.5, 3);
        ZombieTargeting targeting = new ZombieTargeting(world);
        assertTrue(targeting.findTarget(player, List.of(zombie), 12).isPresent());
        TestWorlds.wall(world, 6, 5);
        assertTrue(targeting.findTarget(player, List.of(zombie), 12).isEmpty());
    }
    @Test void missingChunkAlongRayBlocksTheShotEvenWhenTargetChunkExists() {
        World source = TestWorlds.flat(3, 1);
        World world = new World("gap", 7, Instant.EPOCH);
        world.addChunk(source.getChunks().get(0));
        world.addChunk(source.getChunks().get(2));
        Player player = new Player(5.5, 21, 5.5);
        player.setYaw(270);
        Zombie zombie = new Zombie(42.5, 21, 5.5, 3);
        assertTrue(new ZombieTargeting(world).findTarget(player, List.of(zombie), 40).isEmpty());
    }
    @Test void placedAndRemovedWallImmediatelyChangesTargetWithoutCache() {
        World world = TestWorlds.flat(2, 2);
        Player player = new Player(5.5, 21, 5.5);
        Zombie zombie = new Zombie(5.5, 21, 9.5, 3);
        ZombieTargeting targeting = new ZombieTargeting(world);
        TestWorlds.wall(world, 5, 7);
        assertTrue(targeting.findTarget(player, List.of(zombie), 12).isEmpty());
        TestWorlds.remove(world, 5, 21, 7);
        TestWorlds.remove(world, 5, 22, 7);
        assertTrue(targeting.findTarget(player, List.of(zombie), 12).isPresent());
    }
}
