package domain.enemy;

import application.EnemyUpdateService;
import domain.player.PlayerLife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ZombieSeparationTest {
    private final ZombieSeparation separation = new ZombieSeparation();
    private final domain.world.World world = TestWorlds.flat(2, 2);
    private final NavigationGrid grid = new NavigationGrid(world);
    private Zombie body(double x, double z) { return new Zombie(x, TestWorlds.GROUND_Y + 1, z, 3); }
    private void assertSeparate(List<Zombie> bodies) {
        for (int i=0; i<bodies.size(); i++) for(int j=i+1; j<bodies.size(); j++)
            assertFalse(separation.overlaps(bodies.get(i), bodies.get(j)), "pair "+i+":"+j);
    }
    @Test void coincidentPairSeparatesDeterministically() {
        var a=List.of(body(5.5,5.5),body(5.5,5.5));
        var b=List.of(body(5.5,5.5),body(5.5,5.5));
        separation.resolve(a,grid); separation.resolve(b,grid);
        assertSeparate(a);
        assertEquals(a.get(0).getX(),b.get(0).getX());
        assertEquals(a.get(0).getZ(),b.get(0).getZ());
    }
    @Test void twentyCoincidentBodiesResolveWithoutLeavingTerrain() {
        var bodies=new ArrayList<Zombie>();
        for(int i=0;i<20;i++) bodies.add(body(12.5,12.5));
        for(int frame=0;frame<60;frame++) separation.resolve(bodies,grid);
        assertSeparate(bodies);
        bodies.forEach(z -> assertTrue(grid.nodeUnder(z.getX(),z.getY(),z.getZ()).isPresent()));
    }
    @Test void wallCannotBeCrossedWhenResolvingOverlap() {
        for(int z=0;z<16;z++) TestWorlds.wall(world,5,z);
        var bodies=List.of(body(4.6,5.5),body(4.6,5.5));
        separation.resolve(bodies,grid);
        assertSeparate(bodies);
        bodies.forEach(z -> assertTrue(z.getX()+Zombie.WIDTH/2<=5));
    }
    @Test void corridorQueuesBodiesWithoutPassingThroughWalls() {
        for(int x=0;x<16;x++) {TestWorlds.wall(world,x,4);TestWorlds.wall(world,x,6);}
        var bodies=List.of(body(7.5,5.5),body(7.5,5.5),body(7.5,5.5));
        for(int f=0;f<20;f++) separation.resolve(bodies,grid);
        assertSeparate(bodies);
        bodies.forEach(z -> assertTrue(z.getZ()>=5.3&&z.getZ()<=5.7));
    }
    @Test void deadBodiesDoNotBlockSpawnOrLivingBodies() {
        var service=new EnemyUpdateService(world,ZombieParameters.defaults());
        var dead=service.spawnAt(5.5,5.5).orElseThrow(); dead.takeDamage(99);
        assertTrue(service.spawnAt(5.5,5.5).isPresent());
        assertFalse(separation.overlaps(dead,service.zombies().get(1)));
    }
    @Test void occupiedSpawnIsRejectedAndBodyNearWallIsRejected() {
        var service=new EnemyUpdateService(world,ZombieParameters.defaults());
        assertTrue(service.spawnAt(5.5,5.5).isPresent());
        assertTrue(service.spawnAt(5.5,5.5).isEmpty());
        TestWorlds.wall(world,8,5);
        assertTrue(service.spawnAt(7.9,5.5).isEmpty());
    }
    @Test void twentyConvergingBodiesKeepTheirDistinctHitboxes() {
        world.getPlayer().setX(12.5);world.getPlayer().setY(TestWorlds.GROUND_Y+1);world.getPlayer().setZ(12.5);
        var service=new EnemyUpdateService(world,ZombieParameters.defaults());
        for(int i=0;i<20;i++) service.spawnAt(5.5+i%5,5.5+i/5).orElseThrow();
        var life=new PlayerLife(world.getPlayer());
        for(int f=0;f<120;f++) service.update(life,1.0/60);
        assertSeparate(service.zombies());
        assertEquals(20,service.zombies().size());
    }
}
