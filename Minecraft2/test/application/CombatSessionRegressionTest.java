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
        for (int frame = 0; frame < 1000; frame++) session.advance(1.0 / 60, PlayerFrameInput.NEUTRAL);
        assertEquals(75, session.life().health());
        assertEquals(flash, session.life().hitFlashSeconds());
        session.setPaused(false);
        for (int frame = 0; frame < 360; frame++) session.advance(1.0 / 60, PlayerFrameInput.NEUTRAL);
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
            session.advance(1.0 / 60, PlayerFrameInput.NEUTRAL);
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
    @Test void pauseFreezesPersistentPiecesWithoutDeletingThem() {
        var world=TestWorlds.flat(1,1);
        var session=new GameSession(world,new GameSettings());
        var zombie=new domain.enemy.Zombie(5.5,21,5.5,3); zombie.shatter();
        session.enemies().register(zombie);
        session.advance(1.0/60,PlayerFrameInput.NEUTRAL);
        assertEquals(1,session.enemies().debris().size());
        var fragment=session.enemies().debris().get(0).fragments().get(0);
        double x=fragment.x(), y=fragment.y();
        session.setPaused(true);
        for(int frame=0;frame<600;frame++) session.advance(1.0/60,PlayerFrameInput.NEUTRAL);
        assertEquals(x,fragment.x()); assertEquals(y,fragment.y());
        assertEquals(1,session.enemies().debris().size());
    }
    @Test void permanentPiecesDoNotPreventNextWave() {
        var world=TestWorlds.flat(2,2);
        var life=new PlayerLife(world.getPlayer());
        var enemies=new EnemyUpdateService(world,domain.enemy.ZombieParameters.defaults());
        var horde=new HordeManager(new domain.enemy.WaveRules(0,1,0,1,0,1,8,12),world,enemies);
        horde.update(.01,15.5,15.5);
        assertEquals(1,enemies.zombies().size());
        enemies.zombies().get(0).shatter();
        enemies.update(life,.01); horde.update(.01,15.5,15.5);
        assertEquals(1,enemies.debris().size());
        assertEquals(HordeManager.Phase.PAUSE,horde.phase());
        horde.update(1,15.5,15.5);
        assertEquals(2,horde.wave()); assertEquals(1,enemies.debris().size());
    }
}
