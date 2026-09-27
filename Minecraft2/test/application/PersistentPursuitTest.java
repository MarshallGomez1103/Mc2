package application;
import domain.enemy.*;
import domain.player.PlayerLife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PersistentPursuitTest {
    @Test void detectedPlayerRemainsTargetAfterRunningBeyondFormerLossDistance() {
        var world = TestWorlds.flat(4, 1); var player = world.getPlayer();
        player.setX(4.5); player.setY(21); player.setZ(5.5);
        var life = new PlayerLife(player);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        var zombie = enemies.spawnAt(10.5, 5.5).orElseThrow();
        enemies.update(life, 1.0 / 60);
        assertEquals(ZombieState.CHASE, zombie.getState());
        player.setX(60.5);
        double oldX = zombie.getX();
        for (int frame = 0; frame < 120; frame++) enemies.update(life, 1.0 / 60);
        assertEquals(ZombieState.CHASE, zombie.getState());
        assertTrue(zombie.getX() > oldX + 5);
        assertTrue(enemies.pathfinder().getSearchCount() < 6);
    }
}
