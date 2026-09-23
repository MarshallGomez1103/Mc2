package domain.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reglas acordadas antes de escribir conducta (ver docs/corte2/testing/tdd-zombie-fsm.md):
 * detección 10, ataque 1.5, pérdida de objetivo 14; límites inclusivos al entrar en un rango;
 * DEAD tiene prioridad absoluta y es absorbente; jugador muerto equivale a objetivo perdido.
 */
class ZombieStateMachineTest {
    private static final double DETECTION = 10.0;
    private static final double ATTACK = 1.5;
    private static final double LOSE = 14.0;

    private final ZombieStateMachine fsm = new ZombieStateMachine(DETECTION, ATTACK, LOSE);

    private static ZombiePerception alive(double distance) {
        return new ZombiePerception(3, distance, true);
    }

    @Test
    void idleShouldRemainIdleWithoutTargetInRange() {
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.IDLE, alive(DETECTION + 0.01)));
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.IDLE, alive(50)));
    }

    @Test
    void idleShouldChangeToChaseWhenPlayerDetectedInclusiveLimit() {
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.IDLE, alive(DETECTION)));
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.IDLE, alive(4)));
    }

    @Test
    void idleShouldJumpDirectlyToAttackIfPlayerAlreadyAdjacent() {
        assertEquals(ZombieState.ATTACK, fsm.next(ZombieState.IDLE, alive(1.0)));
    }

    @Test
    void chaseShouldChangeToAttackInsideAttackRangeInclusive() {
        assertEquals(ZombieState.ATTACK, fsm.next(ZombieState.CHASE, alive(ATTACK)));
        assertEquals(ZombieState.ATTACK, fsm.next(ZombieState.CHASE, alive(0.5)));
    }

    @Test
    void chaseShouldKeepChasingBetweenAttackAndLoseRange() {
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.CHASE, alive(ATTACK + 0.01)));
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.CHASE, alive(DETECTION + 2)));
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.CHASE, alive(LOSE)));
    }

    @Test
    void chaseShouldReturnToIdleWhenTargetIsLost() {
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.CHASE, alive(LOSE + 0.01)));
    }

    @Test
    void attackShouldReturnToChaseWhenPlayerMovesAway() {
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.ATTACK, alive(ATTACK + 0.01)));
        assertEquals(ZombieState.CHASE, fsm.next(ZombieState.ATTACK, alive(LOSE + 5)));
    }

    @Test
    void attackShouldStayInAttackWhileInRange() {
        assertEquals(ZombieState.ATTACK, fsm.next(ZombieState.ATTACK, alive(ATTACK)));
    }

    @Test
    void deadPlayerMeansTargetLostFromAnyLivingState() {
        ZombiePerception playerDead = new ZombiePerception(3, 0.5, false);
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.IDLE, playerDead));
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.CHASE, playerDead));
        assertEquals(ZombieState.IDLE, fsm.next(ZombieState.ATTACK, playerDead));
    }

    @Test
    void anyLivingStateShouldBecomeDeadWhenHealthIsZeroOrBelow() {
        assertEquals(ZombieState.DEAD, fsm.next(ZombieState.IDLE, new ZombiePerception(0, 50, true)));
        assertEquals(ZombieState.DEAD, fsm.next(ZombieState.CHASE, new ZombiePerception(0, 5, true)));
        assertEquals(ZombieState.DEAD, fsm.next(ZombieState.ATTACK, new ZombiePerception(-2, 0.5, true)));
    }

    @Test
    void deadShouldRemainDeadEvenWithPositiveHealthAndPlayerAdjacent() {
        assertEquals(ZombieState.DEAD, fsm.next(ZombieState.DEAD, alive(0.5)));
        assertEquals(ZombieState.DEAD, fsm.next(ZombieState.DEAD, new ZombiePerception(0, 0.5, false)));
    }

    @Test
    void rangesMustBeOrderedAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> new ZombieStateMachine(10, 0, 14));
        assertThrows(IllegalArgumentException.class, () -> new ZombieStateMachine(10, 11, 14));
        assertThrows(IllegalArgumentException.class, () -> new ZombieStateMachine(10, 1.5, 9));
    }

    @Test
    void perceptionRejectsInvalidDistances() {
        assertThrows(IllegalArgumentException.class, () -> new ZombiePerception(3, -1, true));
        assertThrows(IllegalArgumentException.class, () -> new ZombiePerception(3, Double.NaN, true));
    }
}
