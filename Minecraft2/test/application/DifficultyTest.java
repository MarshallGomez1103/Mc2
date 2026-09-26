package application;

import domain.enemy.Difficulty;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.enemy.ZombieState;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static domain.enemy.TestWorlds.GROUND_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La dificultad cambia la conducta observable de los zombis con los mismos algoritmos: solo
 * cambian los parámetros que recibe {@link EnemyUpdateService}.
 */
class DifficultyTest {
    private static final double FRAME = 1.0 / 60.0;
    private static final double FEET = GROUND_Y + 1;

    /** Mundo plano de 32×16 con el jugador en (2.5, 5.5) y un servicio para la dificultad dada. */
    private static final class Session {
        final World world = TestWorlds.flat(2, 1);
        final PlayerLife life;
        final EnemyUpdateService enemies;

        Session(Difficulty difficulty) {
            Player player = world.getPlayer();
            player.setX(2.5);
            player.setY(FEET);
            player.setZ(5.5);
            life = new PlayerLife(player);
            enemies = new EnemyUpdateService(world, difficulty.zombieParameters());
        }

        void run(double seconds) {
            for (int frame = 0; frame < Math.round(seconds / FRAME); frame++) {
                enemies.update(life, FRAME);
            }
        }
    }

    @Test
    void veryHardZombieNoticesPlayerThatNormalZombieIgnores() {
        Session normal = new Session(Difficulty.NORMAL);
        Session veryHard = new Session(Difficulty.VERY_HARD);
        Zombie calm = normal.enemies.spawnAt(17.5, 5.5).orElseThrow();
        Zombie alert = veryHard.enemies.spawnAt(17.5, 5.5).orElseThrow();

        normal.run(0.1);
        veryHard.run(0.1);

        assertEquals(ZombieState.IDLE, calm.getState(), "a 15 bloques NORMAL todavía no detecta");
        assertEquals(ZombieState.CHASE, alert.getState(), "a 15 bloques VERY_HARD ya persigue");
    }

    @Test
    void veryHardZombieClosesDistanceFaster() {
        Session normal = new Session(Difficulty.NORMAL);
        Session veryHard = new Session(Difficulty.VERY_HARD);
        Zombie slow = normal.enemies.spawnAt(12.5, 5.5).orElseThrow();
        Zombie fast = veryHard.enemies.spawnAt(12.5, 5.5).orElseThrow();

        normal.run(1.5);
        veryHard.run(1.5);

        assertTrue(fast.getX() < slow.getX(),
                "VERY_HARD debe avanzar más: " + fast.getX() + " frente a " + slow.getX());
    }

    @Test
    void veryHardZombieKillsSooner() {
        Session normal = new Session(Difficulty.NORMAL);
        Session veryHard = new Session(Difficulty.VERY_HARD);
        normal.enemies.spawnAt(3.5, 5.5).orElseThrow();
        veryHard.enemies.spawnAt(3.5, 5.5).orElseThrow();

        normal.run(0.75);
        veryHard.run(0.75);

        assertFalse(normal.life.isDead(), "NORMAL espera 1 s antes del primer golpe");
        assertTrue(veryHard.life.isDead(), "VERY_HARD golpea a los 0.6 s");
    }

    @Test
    void veryHardZombieSurvivesHitsThatKillNormalZombie() {
        Zombie normal = new Session(Difficulty.NORMAL).enemies.spawnAt(8.5, 5.5).orElseThrow();
        Zombie veryHard = new Session(Difficulty.VERY_HARD).enemies.spawnAt(8.5, 5.5).orElseThrow();

        for (int hit = 0; hit < 3; hit++) {
            normal.takeDamage(ZombieMeleeService.DAMAGE);
            veryHard.takeDamage(ZombieMeleeService.DAMAGE);
        }

        assertFalse(normal.isAlive());
        assertTrue(veryHard.isAlive());
    }

    @Test
    void veryHardWavesAreBiggerAndArriveSooner() {
        var normal = Difficulty.NORMAL.waveRules();
        var veryHard = Difficulty.VERY_HARD.waveRules();

        assertTrue(veryHard.firstWaveDelaySeconds() < normal.firstWaveDelaySeconds());
        assertTrue(veryHard.pauseSeconds() < normal.pauseSeconds());
        for (int wave = 1; wave <= 10; wave++) {
            assertTrue(veryHard.countFor(wave) > normal.countFor(wave), "oleada " + wave);
        }
    }

    @Test
    void normalKeepsTheParametersTheAiWasTestedWith() {
        assertEquals(domain.enemy.ZombieParameters.defaults(), Difficulty.NORMAL.zombieParameters());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void everyDifficultyCyclesThroughAllLevels(Difficulty start) {
        Difficulty current = start;
        for (int step = 0; step < Difficulty.values().length; step++) {
            current = current.next();
        }
        assertEquals(start, current);
        assertTrue(start.next() != start);
    }
}
