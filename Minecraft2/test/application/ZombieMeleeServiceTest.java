package application;

import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static domain.enemy.TestWorlds.GROUND_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieMeleeServiceTest {
    private static final double FEET = GROUND_Y + 1;

    private final World world = TestWorlds.flat(1, 1);
    private final ZombieMeleeService melee = new ZombieMeleeService(world);
    /** yaw 0 mira hacia +Z (misma convención que Player.forwardVector). */
    private final Player player = new Player(5.5, FEET, 5.5);

    @Test
    void hitsNearestZombieInFrontWithinReach() {
        Zombie near = new Zombie(5.5, FEET, 8.5, 3);
        Zombie far = new Zombie(5.5, FEET, 9.3, 3);
        assertTrue(melee.strike(player, List.of(far, near)));
        assertEquals(2, near.getHealth());
        assertEquals(3, far.getHealth());
    }

    @Test
    void zombieBehindWallIsNotHit() {
        Zombie zombie = new Zombie(5.5, FEET, 8.5, 3);
        TestWorlds.wall(world, 5, 7);
        assertFalse(melee.strike(player, List.of(zombie)));
        assertEquals(3, zombie.getHealth());
    }

    @Test
    void outOfReachBehindOrDeadZombiesAreIgnored() {
        Zombie tooFar = new Zombie(5.5, FEET, 10.5, 3);
        Zombie behind = new Zombie(5.5, FEET, 3.5, 3);
        Zombie dead = new Zombie(5.5, FEET, 7.5, 3);
        dead.takeDamage(3);
        assertFalse(melee.strike(player, List.of(tooFar, behind, dead)));
        assertFalse(melee.strike(player, List.of()));
    }

    @Test
    void lookingDownAtZombieFeetStillCountsAsHit() {
        Zombie zombie = new Zombie(5.5, FEET, 7.5, 3);
        player.setPitch(-30f);
        assertTrue(melee.findTarget(player, List.of(zombie)).isPresent());
        player.setPitch(80f);
        assertTrue(melee.findTarget(player, List.of(zombie)).isEmpty(), "mirando al cielo no hay objetivo");
    }
}
