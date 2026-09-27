package domain.enemy;

import domain.Position;
import domain.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieTest {
    @Test
    void zombieIsNotAPlayerAndDoesNotDependOnLibGdx() {
        assertFalse(Player.class.isAssignableFrom(Zombie.class));
        assertEquals(Object.class, Zombie.class.getSuperclass());
        for (var field : Zombie.class.getDeclaredFields()) {
            assertFalse(field.getType().getName().startsWith("com.badlogic"),
                    "el dominio no puede depender de LibGDX: " + field.getName());
        }
    }

    @Test
    void damageNeverGoesBelowZeroAndAliveFollowsHealth() {
        Zombie zombie = new Zombie(1.5, 20, 1.5, 3);
        assertTrue(zombie.isAlive());
        zombie.takeDamage(2);
        assertEquals(1, zombie.getHealth());
        zombie.takeDamage(5);
        assertEquals(0, zombie.getHealth());
        assertFalse(zombie.isAlive());
        assertThrows(IllegalArgumentException.class, () -> zombie.takeDamage(0));
        assertThrows(IllegalArgumentException.class, () -> new Zombie(0, 0, 0, 0));
    }

    @Test
    void stateChangesPreserveHealthAndPositionAndRejectNull() {
        Zombie zombie = new Zombie(1.5, 21, 2.5, 3);
        zombie.setState(ZombieState.ATTACK);
        zombie.setState(ZombieState.ATTACK);
        assertEquals(ZombieState.ATTACK, zombie.getState());
        zombie.setState(ZombieState.CHASE);
        assertEquals(ZombieState.CHASE, zombie.getState());
        assertEquals(3, zombie.getHealth());
        assertEquals(new Position(1, 21, 2), zombie.toPosition());
        assertThrows(NullPointerException.class, () -> zombie.setState(null));
    }

    @Test
    void positionUsesFloorLikePlayer() {
        Zombie zombie = new Zombie(-0.2, 20.9, 3.7, 3);
        assertEquals(new Position(-1, 20, 3), zombie.toPosition());
        assertEquals(5, zombie.distanceTo(-0.2, 20.9, 8.7), 1e-9);
    }
}
