package application;
import domain.enemy.TestWorlds;
import domain.enemy.ZombieParameters;
import domain.player.PlayerLife;
import domain.block.BlockType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZombieContactRegressionTest {
    @Test void stationaryPlayerBehindOneLowBlockCanBeReachedOverIt() {
        var world=TestWorlds.flat(1,1);var p=world.getPlayer();
        p.setX(6.35);p.setY(21);p.setZ(5.5);
        var life=new PlayerLife(p);var enemies=new EnemyUpdateService(world,ZombieParameters.defaults());
        enemies.spawnAt(4.75,5.5).orElseThrow();
        TestWorlds.place(world,5,21,5,BlockType.STONE);
        enemies.update(life,1.0/60);
        assertEquals(75, life.health(), "El primer contacto tiene daño inmediato");
        for(int f=0;f<240;f++) enemies.update(life,1.0/60);
        assertTrue(life.isDead(),"Los brazos alcanzan por encima del bloque; jugador quieto no debe ser invulnerable");
    }
    @Test void stationaryPlayerNextToRaisedZombieIsHitWithoutMovingIntoIt() {
        var world = TestWorlds.flat(1, 1); var p = world.getPlayer();
        TestWorlds.place(world, 5, 21, 5, BlockType.STONE);
        p.setX(6.75); p.setY(21); p.setZ(5.5);
        var life = new PlayerLife(p);
        var enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
        enemies.spawnAt(5.5, 5.5).orElseThrow();
        enemies.update(life, 1.0 / 60);
        assertEquals(75, life.health());
        for (int f = 0; f < 240; f++) enemies.update(life, 1.0 / 60);
        assertTrue(life.isDead());
        assertEquals(6.75, p.getX());
    }
}
