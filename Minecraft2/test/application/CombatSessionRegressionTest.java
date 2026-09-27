package application;

import domain.enemy.TestWorlds;
import domain.player.PlayerLife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatSessionRegressionTest {
    @Test void healthRecoveryAndDamageFlashFreezeWithSessionPause() {
        var world = TestWorlds.flat(1, 1);
        var settings = new GameSettings();
        settings.toggleEnemies();
        var session = new GameSession(world, settings);
        session.life().takeDamage(25, PlayerLife.DeathCause.ENEMY);
        double flash = session.life().hitFlashSeconds();
        session.setPaused(true);
        for (int frame = 0; frame < 1000; frame++) session.advance(1.0 / 60, () -> {});
        assertEquals(75, session.life().health());
        assertEquals(flash, session.life().hitFlashSeconds());
        session.setPaused(false);
        for (int frame = 0; frame < 360; frame++) session.advance(1.0 / 60, () -> {});
        assertTrue(session.life().health() >= 79 && session.life().health() <= 81);
        assertEquals(0, session.life().hitFlashSeconds());
    }

    @Test void pistolBecomesAvailableInActualThirdWaveAndNeverInEarlierWaves() {
        var world = TestWorlds.flat(2, 2); var player = world.getPlayer();
        player.setX(15.5); player.setY(21); player.setZ(15.5);
        var session = new GameSession(world, new GameSettings());
        assertFalse(session.pistol().appeared());
        for (int frame = 0; frame < 6000 && session.hordes().wave() < 3; frame++) {
            if (session.hordes().wave() < 3) assertFalse(session.pistol().appeared());
            session.advance(1.0 / 60, () -> {});
            session.enemies().zombies().stream().filter(z -> z.isAlive()).forEach(z -> z.takeDamage(100));
        }
        assertEquals(3, session.hordes().wave());
        assertTrue(session.pistol().appeared());
        assertTrue(session.pistol().pickupPosition().isPresent() || session.pistol().owned());
    }
    @Test void crawlerAttackReportsKamikazeDeathCause() {
        var world = TestWorlds.flat(1, 1); var player = world.getPlayer();
        player.setX(5.5); player.setY(21); player.setZ(5.5);
        var life = new PlayerLife(player);
        var enemies = new EnemyUpdateService(world, domain.enemy.ZombieParameters.defaults());
        var crawler = new domain.enemy.Zombie(6.5, 21, 5.5, 3);
        crawler.becomeCrawler(); enemies.register(crawler);
        for (int frame = 0; frame < 150 && !life.isDead(); frame++) enemies.update(life, 1.0 / 60);
        assertTrue(life.isDead());
        assertEquals(PlayerLife.DeathCause.KAMIKAZE, life.deathCause());
    }
}
