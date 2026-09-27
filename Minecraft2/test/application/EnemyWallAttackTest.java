package application;

import domain.enemy.TestWorlds;
import domain.enemy.ZombieParameters;
import domain.player.PlayerLife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyWallAttackTest {
    @Test void solidWallPreventsDamageAndRemovingItAllowsImmediateFirstContact() {
        var world=TestWorlds.flat(1,1);
        var player=world.getPlayer();player.setX(6.35);player.setY(TestWorlds.GROUND_Y+1);player.setZ(5.5);
        var life=new PlayerLife(player);
        var service=new EnemyUpdateService(world,ZombieParameters.defaults());
        service.spawnAt(4.75,5.5).orElseThrow();
        for(int z=0;z<16;z++) TestWorlds.wall(world,5,z);
        for(int frame=0;frame<120;frame++) service.update(life,1.0/60);
        assertEquals(100, life.health(), "The wall prevents melee damage despite close distance");
        TestWorlds.remove(world,5,TestWorlds.GROUND_Y+1,5);
        TestWorlds.remove(world,5,TestWorlds.GROUND_Y+2,5);
        service.update(life,1.0/60);
        assertEquals(75, life.health(), "El primer contacto sin pared no necesita esperar");
        for(int frame=0;frame<10;frame++) service.update(life,1.0/60);
        assertEquals(75, life.health(), "El golpe siguiente respeta el cooldown");
        for(int frame=0;frame<240;frame++) service.update(life,1.0/60);
        assertTrue(life.isDead());
    }
    @Test
    void zombieAtMeleeDistanceRoutesAroundWallInsteadOfFreezing() {
        var world = TestWorlds.flat(1, 1);
        var player = world.getPlayer();
        player.setX(6.2); player.setY(TestWorlds.GROUND_Y + 1); player.setZ(5.5);
        var life = new PlayerLife(player);
        var service = new EnemyUpdateService(world, ZombieParameters.defaults());
        var zombie = service.spawnAt(4.65, 5.5).orElseThrow();
        TestWorlds.wall(world, 5, 5);
        service.update(life, 1.0 / 60);
        assertEquals(100, life.health());
        assertTrue(service.pathfinder().getSearchCount() > 0,
                "Un enemigo cercano con la pared en medio sigue buscando al jugador");
        for (int frame = 0; frame < 180; frame++) service.update(life, 1.0 / 60);
        assertTrue(zombie.getX() > 5.0, "El zombi rodea la pared");
        assertTrue(life.health() < 100, "Al alcanzar al jugador sí hace daño");
    }
}
