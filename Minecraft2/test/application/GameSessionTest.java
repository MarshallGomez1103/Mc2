package application;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.PlayerLife;
import domain.world.World;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
    private World world() {
        World w = TestWorlds.flat(2,2);
        w.getPlayer().setX(16.5); w.getPlayer().setY(21); w.getPlayer().setZ(16.5);
        return w;
    }
    @Test void pausedFreezesInputHordeAndEnemyTimers() {
        GameSession s = new GameSession(world(), new GameSettings());
        Zombie z = s.enemies().spawnAt(8.5,8.5).orElseThrow();
        double x = z.getX(), countdown = s.hordes().secondsToNextWave();
        s.setPaused(true);
        for (int i=0;i<500;i++) s.advance(.05, () -> fail("pausa no ejecuta input/physics/stamina"));
        assertEquals(countdown,s.hordes().secondsToNextWave()); assertEquals(x,z.getX());
        assertEquals(0,z.getDeadSeconds()); assertEquals(GameSession.State.PAUSED,s.state());
        s.setPaused(false); s.advance(.05, () -> {});
        assertTrue(s.hordes().secondsToNextWave()<countdown);
    }
    @Test void deathFreezesAndRespawnRestoresRunning() {
        GameSession s = new GameSession(world(),new GameSettings());
        s.life().die(PlayerLife.DeathCause.ENEMY);
        s.advance(.05, () -> fail("muerto no avanza")); assertEquals(GameSession.State.DEAD,s.state());
        s.setPaused(true); s.setPaused(false); assertEquals(GameSession.State.DEAD,s.state());
        s.respawn(); assertEquals(GameSession.State.RUNNING,s.state());
    }
    @Test void settingsKeepDifficultySnapshotAndApplyEnemyToggleImmediately() {
        GameSettings settings = new GameSettings(); GameSession s = new GameSession(world(),settings);
        settings.cycleDifficulty(); settings.toggleEnemies(); s.applySettings(settings);
        assertNotEquals(settings.difficulty(),s.difficulty()); assertFalse(s.enemies().isEnabled());
        assertEquals(s.difficulty().zombieParameters(),s.enemies().parameters());
        double t=s.hordes().secondsToNextWave(); s.advance(.05,()->{});
        assertEquals(t,s.hordes().secondsToNextWave());
    }
    @Test void invalidTimeDoesNotExecuteInput() {
        GameSession s=new GameSession(world(),new GameSettings());
        s.advance(Double.NaN,()->fail()); s.advance(-1,()->fail()); s.advance(0,()->fail());
    }
}
