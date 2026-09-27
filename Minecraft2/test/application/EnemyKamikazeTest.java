package application;

import domain.block.BlockType;
import domain.enemy.*;
import domain.player.PlayerLife;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnemyKamikazeTest {
    @Test void zombieRoutesToPitThenFallsAndSurvivorCrawlsAfterPlayer() {
        var world = pitWorld();
        var player = world.getPlayer();
        player.setX(5.5); player.setY(11); player.setZ(5.5);
        var life = new PlayerLife(player);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults(), new AStarPathfinder(), () -> 0.1);
        var z = enemies.spawnAt(3.5, 5.5).orElseThrow();
        boolean airborne = false;
        for (int frame = 0; frame < 180 && !z.isCrawler(); frame++) {
            enemies.update(life, 1.0 / 60);
            if (z.getY() > 11.1 && z.getY() < 20.9) airborne = true;
        }
        assertTrue(airborne, "no teletransporte del borde al fondo");
        assertTrue(z.isCrawler());
        assertEquals(11, z.getY(), 1e-6);
        assertTrue(z.isAlive());
        assertTrue(z.isKamikaze());
        player.setX(8.5);
        double initial = z.getX();
        // Cave floor extends horizontally; the crawler must resume pathfinding after landing.
        for (int x = 5; x <= 9; x++) for (int zz = 4; zz <= 6; zz++) {
            TestWorlds.remove(world, x, 20, zz);
            TestWorlds.place(world, x, 10, zz, BlockType.STONE);
        }
        for (int frame = 0; frame < 90; frame++) enemies.update(life, 1.0 / 60);
        assertTrue(z.getX() > initial + 0.5);
    }

    @Test void nonSurvivorShattersAtBottomAndDespawns() {
        var world = pitWorld();
        var p = world.getPlayer(); p.setX(5.5); p.setY(11); p.setZ(5.5);
        var life = new PlayerLife(p);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults(), new AStarPathfinder(), () -> 0.9);
        var z = enemies.spawnAt(3.5, 5.5).orElseThrow();
        for (int frame = 0; frame < 120 && z.isAlive(); frame++) enemies.update(life, 1.0 / 60);
        assertTrue(z.isShattered());
        assertEquals(11, z.getY(), 1e-6);
        for (int frame = 0; frame < 60; frame++) enemies.update(life, 1.0 / 60);
        assertFalse(enemies.zombies().contains(z));
    }

    @Test void aCrawlerAtBottomDoesNotPermanentlyCloseThePitEntrance() {
        var world = pitWorld();
        var p = world.getPlayer(); p.setX(5.5); p.setY(11); p.setZ(5.5);
        var life = new PlayerLife(p);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults(), new AStarPathfinder(), () -> 0.9);
        var crawler = new Zombie(5.5, 11, 5.5, 3); crawler.becomeCrawler();
        enemies.register(crawler);
        var next = enemies.spawnAt(3.5, 5.5).orElseThrow();
        boolean entered = false;
        for (int frame = 0; frame < 90; frame++) {
            enemies.update(life, 1.0 / 60);
            if (next.getY() < 20) { entered = true; break; }
        }
        assertTrue(entered, "el superviviente diez bloques abajo no bloquea el borde");
    }

    @Test void twoZombiesEnterPitAtDifferentTimesInsteadOfOverlapping() {
        var world = pitWorld();
        var p = world.getPlayer(); p.setX(5.5); p.setY(11); p.setZ(5.5);
        var life = new PlayerLife(p);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults(), new AStarPathfinder(), () -> 0.9);
        var first = enemies.spawnAt(3.5, 5.5).orElseThrow();
        var second = enemies.spawnAt(2.5, 5.5).orElseThrow();
        int firstFrame = -1, secondFrame = -1;
        for (int frame = 0; frame < 240 && secondFrame < 0; frame++) {
            enemies.update(life, 1.0 / 60);
            if (first.getY() < 20.9 && firstFrame < 0) firstFrame = frame;
            if (second.getY() < 20.9 && secondFrame < 0) secondFrame = frame;
        }
        assertTrue(firstFrame >= 0 && secondFrame > firstFrame);
        assertTrue(secondFrame - firstFrame >= 20, "entrada secuencial con separación temporal");
    }

    @Test void fallingZombieCannotUseAnotherHeadAsAPlatform() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.wall(world, 4, 5); TestWorlds.wall(world, 6, 5);
        TestWorlds.wall(world, 5, 4); TestWorlds.wall(world, 5, 6);
        var p = world.getPlayer(); p.setX(9.5); p.setY(21); p.setZ(5.5);
        var life = new PlayerLife(p);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        var bottom = new Zombie(5.5, 21, 5.5, 3);
        var falling = new Zombie(5.5, 26, 5.5, 3); falling.setOnGround(false);
        enemies.register(bottom); enemies.register(falling);
        // Keep the lower body in place, while the falling zombie remains an acquired target.
        p.setX(5.5);
        for (int frame = 0; frame < 80 && falling.isAlive() && !life.isDead(); frame++) {
            enemies.update(life, 1.0 / 60);
        }
        assertTrue(falling.isShattered());
        assertFalse(falling.isAlive());
        assertTrue(bottom.isAlive());
    }

    @Test void disablingEnemiesStillAllowsFlashToFadeWithoutMoving() {
        var world = TestWorlds.flat(1, 1);
        var life = new PlayerLife(world.getPlayer());
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        var z = enemies.spawnAt(3.5, 5.5).orElseThrow();
        enemies.setEnabled(false);
        z.takeDamage(1);
        for (int frame = 0; frame < 20; frame++) enemies.update(life, 1.0 / 60);
        assertEquals(0, z.hitFlashSeconds());
        assertEquals(3.5, z.getX());
        assertEquals(21, z.getY());
    }

    @Test void committedDescentRemembersEntranceWhenPlayerMovesAlongCave() {
        var world = pitWorld();
        for (int x = 5; x <= 15; x++) for (int z = 4; z <= 6; z++) {
            TestWorlds.place(world, x, 10, z, BlockType.STONE);
        }
        var player = world.getPlayer();
        player.setX(5.5); player.setY(11); player.setZ(5.5);
        var life = new PlayerLife(player);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults(), new AStarPathfinder(), () -> .1);
        var zombie = enemies.spawnAt(1.5, 5.5).orElseThrow();
        enemies.update(life, 1.0/60);
        player.setX(15.5);
        for (int frame = 0; frame < 240; frame++) enemies.update(life, 1.0/60);
        assertTrue(zombie.isCrawler(), "conserva la entrada elegida aunque el jugador ya esté lejos");
        assertEquals(11, zombie.getY(), 1e-6);
        assertTrue(zombie.getX() > 6, "tras aterrizar continúa persiguiendo dentro de la cueva");
    }

    private static domain.world.World pitWorld() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.remove(world, 5, 20, 5);
        TestWorlds.place(world, 5, 10, 5, BlockType.STONE);
        return world;
    }
}
