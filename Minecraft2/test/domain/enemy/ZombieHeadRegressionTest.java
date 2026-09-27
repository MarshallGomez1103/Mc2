package domain.enemy;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ZombieHeadRegressionTest {
    @Test void feetJustAboveAnotherHeadStillReserveHorizontalSpace() {
        var separation=new ZombieSeparation();
        var lower=new Zombie(5.5,21,5.5,3);
        var upper=new Zombie(5.5,23,5.5,3);
        assertTrue(separation.overlaps(lower,upper),"No se permite caminar a 0.2 bloques sobre otra cabeza");
        upper.setPosition(5.5,25,5.5);
        assertFalse(separation.overlaps(lower,upper),"Pisos claramente separados no deben bloquearse");
    }
    @Test void crowdCorrectionCannotPushBodiesUpOntoSteps() {
        var world = TestWorlds.flat(1, 1);
        TestWorlds.place(world, 5, 21, 5, domain.block.BlockType.STONE);
        TestWorlds.wall(world, 4, 4);
        TestWorlds.wall(world, 4, 6);
        var first = new Zombie(4.69, 21, 5.5, 3);
        var second = new Zombie(4.69, 21, 5.5, 3);
        new ZombieSeparation().resolve(List.of(first, second), new NavigationGrid(world));
        assertEquals(21, first.getY());
        assertEquals(21, second.getY());
    }
}
