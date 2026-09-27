package application;

import domain.enemy.TestWorlds;
import domain.enemy.WaveRules;
import domain.enemy.ZombieParameters;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HordeSpawnSafetyTest {
    @Test void failedZeroIntervalSpawnRetriesOnNextStepWithoutLosingCountOrLooping() {
        var world = new domain.world.World("retry", 7, java.time.Instant.EPOCH);
        world.addChunk(new domain.world.Chunk(0, 0));
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        var horde = new HordeManager(new WaveRules(0, 4, 0, 4, 0, 1, 8, 12), world, enemies);
        horde.update(.01, 8.5, 8.5);
        assertEquals(4, horde.pendingSpawns());
        assertEquals(1, horde.failedSpawns());
        assertEquals(HordeManager.Phase.SPAWNING, horde.phase());
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            TestWorlds.place(world, x, TestWorlds.GROUND_Y, z, domain.block.BlockType.STONE);
        }
        horde.update(.02, 8.5, 8.5);
        assertEquals(0, enemies.zombies().size(), "todavía espera el siguiente intento");
        horde.update(.05, 8.5, 8.5);
        assertEquals(4, enemies.zombies().size());
        assertEquals(0, horde.pendingSpawns());
        assertEquals(1, horde.wave());
    }

    @Test void instantLargeWavesKeepPendingSpawnsAfterBoundedFrameBudget() {
        var world = TestWorlds.flat(4, 4);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        var horde = new HordeManager(new WaveRules(0, 500, 0, 500, 0, 1, 8, 12), world, enemies);
        horde.update(.05, 32.5, 32.5);
        assertTrue(enemies.zombies().size() > 0);
        assertTrue(enemies.zombies().size() <= HordeManager.MAX_SPAWNS_PER_UPDATE);
        assertEquals(500, enemies.zombies().size() + horde.pendingSpawns());
        assertEquals(HordeManager.Phase.SPAWNING, horde.phase());
    }

    @Test void playerOnEastBoundaryNeverReceivesAZombieOnTheirBody() {
        for(long seed=0;seed<40;seed++) {
            var world=TestWorlds.flat(2,2);
            var enemies=new EnemyUpdateService(world,ZombieParameters.defaults());
            var horde=new HordeManager(new WaveRules(0,6,0,6,0,1,14,20),seed,world,enemies);
            for (int frame = 0; frame < 120 && (horde.pendingSpawns() > 0 || horde.wave() == 0); frame++) {
                horde.update(1.0/60,31.5,16.5);
            }
            assertEquals(6, enemies.zombies().size(), "todos aparecen tras reintentos seguros, seed=" + seed);
            assertEquals(0, horde.pendingSpawns(), "ninguna aparición se descarta, seed=" + seed);
            for(var zombie:enemies.zombies()) assertTrue(Math.hypot(zombie.getX()-31.5,zombie.getZ()-16.5)>=2.5,
                    "no clamped zombie may appear on player: seed="+seed);
        }
    }
}
