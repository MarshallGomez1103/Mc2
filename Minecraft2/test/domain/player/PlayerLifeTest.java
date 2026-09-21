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
}
