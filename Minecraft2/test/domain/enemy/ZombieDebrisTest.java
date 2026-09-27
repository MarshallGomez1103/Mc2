package domain.enemy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZombieDebrisTest {
    @Test void piecesScatterSettleAboveFloorAndRemainStationary() {
        var grid=new NavigationGrid(TestWorlds.flat(1,1));
        var source=new Zombie(5.5,21,5.5,3); source.shatter();
        var remains=new ZombieDebris(source);
        for (int frame=0;frame<600;frame++) remains.advance(grid,1.0/60);
        assertTrue(remains.isSettled());
        assertEquals(18,remains.fragments().size());
        assertTrue(remains.fragments().stream().allMatch(f -> f.visible() && f.y() >= 21+ZombieDebris.RADIUS));
        assertTrue(remains.fragments().stream().anyMatch(f -> Math.abs(f.x()-5.5)>.4));
        double x=remains.fragments().get(0).x(), y=remains.fragments().get(0).y();
        remains.advance(grid,60);
        assertEquals(x,remains.fragments().get(0).x());
        assertEquals(y,remains.fragments().get(0).y());
    }
    @Test void crawlerMovesAtTwentyEightPercentOfNormalSpeed() {
        var grid=new NavigationGrid(TestWorlds.flat(1,1));
        var crawler=new Zombie(3.5,21,5.5,3); crawler.becomeCrawler();
        var normal=new Zombie(3.5,21,5.5,3);
        var movement=new ZombieMovement();
        movement.toward(crawler,grid,10.5,5.5,4.3,.1);
        movement.toward(normal,grid,10.5,5.5,4.3,.1);
        assertEquals(.28,(crawler.getX()-3.5)/(normal.getX()-3.5),1e-8);
    }
}
