package application;

import domain.enemy.TestWorlds;
import domain.enemy.WaveRules;
import domain.enemy.ZombieParameters;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HordeSpawnSafetyTest {
    @Test void playerOnEastBoundaryNeverReceivesAZombieOnTheirBody() {
        for(long seed=0;seed<40;seed++) {
            var world=TestWorlds.flat(2,2);
            var enemies=new EnemyUpdateService(world,ZombieParameters.defaults());
            var horde=new HordeManager(new WaveRules(0,6,0,6,0,1,14,20),seed,world,enemies);
            horde.update(1.0/60,31.5,16.5);
            assertFalse(enemies.zombies().isEmpty(),"at least one safe direction exists, seed="+seed);
            for(var zombie:enemies.zombies()) assertTrue(Math.hypot(zombie.getX()-31.5,zombie.getZ()-16.5)>=2.5,
                    "no clamped zombie may appear on player: seed="+seed);
        }
    }
}
