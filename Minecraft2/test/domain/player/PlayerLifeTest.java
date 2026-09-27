package domain.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLifeTest {
    @Test
    void fallingBelowWorldCausesDeathAndRespawnUsesLastGroundedPosition() {
        Player player = new Player(8.5, 25, 8.5);
        PlayerLife life = new PlayerLife(player);
        player.setX(9.5);
        player.setY(22);
        player.setOnGround(true);
        life.update();

        player.setOnGround(false);
        player.setY(-8.1);
        player.setVelocityY(-30);
        life.update();
        assertTrue(life.isDead());
        assertEquals(PlayerLife.DeathCause.VOID, life.deathCause());

        life.respawn();
        assertFalse(life.isDead());
        assertEquals(PlayerLife.MAX_HEALTH, life.health());
        assertEquals(9.5, player.getX());
        assertEquals(22, player.getY());
        assertEquals(0, player.getVelocityY());
        assertFalse(player.isOnGround());
    }

    @Test
    void enemyCanUseTheSameDeathStateLaterWithoutChangingVoidRule() {
        Player player = new Player(0, 20, 0);
        PlayerLife life = new PlayerLife(player);
        player.setY(-8);
        life.update();
        assertFalse(life.isDead(), "el límite de caída es estrictamente menor que -8");

        life.die(PlayerLife.DeathCause.ENEMY);
        life.die(PlayerLife.DeathCause.VOID);
        assertEquals(PlayerLife.DeathCause.ENEMY, life.deathCause(),
                "la primera causa de muerte prevalece hasta reaparecer");
    }
    @Test
    void nonLethalHitsReduceHealthAndTheFourthHitKills() {
        PlayerLife life = new PlayerLife(new Player(0, 20, 0));
        assertEquals(100, life.health());
        for (int hit = 1; hit <= 3; hit++) {
            life.takeDamage(25, PlayerLife.DeathCause.ENEMY);
            assertEquals(100 - hit * 25, life.health());
            assertFalse(life.isDead());
        }
        assertEquals(.25, life.healthFraction());
        life.takeDamage(25, PlayerLife.DeathCause.ENEMY);
        assertTrue(life.isDead());
        assertEquals(0, life.health());
        assertEquals(PlayerLife.DeathCause.ENEMY, life.deathCause());
        life.takeDamage(200, PlayerLife.DeathCause.VOID);
        assertEquals(PlayerLife.DeathCause.ENEMY, life.deathCause());
        life.respawn();
        assertEquals(100, life.health());
        assertEquals(1, life.healthFraction());
    }

    @Test
    void damageMustBePositiveAndHealthNeverDropsBelowZero() {
        PlayerLife life = new PlayerLife(new Player(0, 20, 0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> life.takeDamage(0, PlayerLife.DeathCause.ENEMY));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> life.takeDamage(-1, PlayerLife.DeathCause.ENEMY));
        life.takeDamage(999, PlayerLife.DeathCause.ENEMY);
        assertEquals(0, life.health());
    }
    @Test
    void regeneratesOnlyAfterFiveSecondsWithoutDamageAndCapsAtFullHealth() {
        PlayerLife life = new PlayerLife(new Player(0, 20, 0));
        life.takeDamage(25, PlayerLife.DeathCause.ENEMY);
        assertEquals(.3, life.hitFlashSeconds());
        life.advance(4.9);
        assertEquals(75, life.health());
        assertEquals(0, life.hitFlashSeconds());
        life.advance(.3);
        assertEquals(76, life.health());
        life.advance(100);
        assertEquals(100, life.health());
    }

    @Test
    void anotherHitResetsRegenerationAndDeathFreezesTimersUntilRespawn() {
        PlayerLife life = new PlayerLife(new Player(0, 20, 0));
        life.takeDamage(25, PlayerLife.DeathCause.ENEMY);
        life.advance(6);
        assertEquals(80, life.health());
        life.takeDamage(10, PlayerLife.DeathCause.ENEMY);
        life.advance(5);
        assertEquals(70, life.health());
        life.advance(.2);
        assertEquals(71, life.health());
        life.takeDamage(100, PlayerLife.DeathCause.KAMIKAZE);
        life.advance(100);
        assertEquals(0, life.health());
        assertEquals(.3, life.hitFlashSeconds());
        assertEquals(PlayerLife.DeathCause.KAMIKAZE, life.deathCause());
        life.respawn();
        assertEquals(100, life.health());
        assertEquals(0, life.hitFlashSeconds());
    }

    @Test
    void invalidSimulationDeltaCannotAdvanceHealthOrFlash() {
        PlayerLife life = new PlayerLife(new Player(0, 20, 0));
        life.takeDamage(25, PlayerLife.DeathCause.ENEMY);
        for (double delta : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1, 0}) life.advance(delta);
        assertEquals(75, life.health());
        assertEquals(.3, life.hitFlashSeconds());
    }
}
