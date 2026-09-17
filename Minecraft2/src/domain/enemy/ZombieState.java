package domain.enemy;

/** Vocabulario compartido del Corte 2; no implementa transiciones ni comportamiento. */
public enum ZombieState {
    IDLE,
    CHASE,
    ATTACK,
    DEAD
}
