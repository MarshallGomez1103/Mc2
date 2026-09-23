package domain.enemy;

import java.util.Objects;

/**
 * FSM pura: decide el siguiente ZombieState a partir del actual y de la percepción.
 * No ejecuta A*, no mueve al zombi ni conoce LibGDX; EnemyUpdateService actúa según el estado.
 *
 * <p>Prioridad de reglas: muerte del zombi &gt; jugador muerto (objetivo perdido) &gt; distancia.
 * Los límites son inclusivos al entrar en un rango ({@code <=}). La pérdida del objetivo usa un
 * rango mayor que la detección (histéresis) para que el zombi no oscile IDLE/CHASE en el borde.
 */
public final class ZombieStateMachine {
    private final double detectionRange;
    private final double attackRange;
    private final double loseTargetRange;

    public ZombieStateMachine(double detectionRange, double attackRange, double loseTargetRange) {
        if (attackRange <= 0 || attackRange > detectionRange || detectionRange > loseTargetRange) {
            throw new IllegalArgumentException(
                    "Se requiere 0 < attackRange <= detectionRange <= loseTargetRange");
        }
        this.detectionRange = detectionRange;
        this.attackRange = attackRange;
        this.loseTargetRange = loseTargetRange;
    }

    public static ZombieStateMachine from(ZombieParameters parameters) {
        return new ZombieStateMachine(parameters.detectionRange(), parameters.attackRange(),
                parameters.loseTargetRange());
    }

    public ZombieState next(ZombieState current, ZombiePerception perception) {
        Objects.requireNonNull(current, "current no puede ser null");
        Objects.requireNonNull(perception, "perception no puede ser null");
        if (current == ZombieState.DEAD || perception.health() <= 0) {
            return ZombieState.DEAD;
        }
        if (!perception.playerAlive()) {
            return ZombieState.IDLE;
        }
        double distance = perception.distanceToPlayer();
        return switch (current) {
            case IDLE -> fromIdle(distance);
            case CHASE -> fromChase(distance);
            case ATTACK -> fromAttack(distance);
            case DEAD -> ZombieState.DEAD;
        };
    }

    private ZombieState fromIdle(double distance) {
        if (distance <= attackRange) {
            return ZombieState.ATTACK;
        }
        return distance <= detectionRange ? ZombieState.CHASE : ZombieState.IDLE;
    }

    private ZombieState fromChase(double distance) {
        if (distance <= attackRange) {
            return ZombieState.ATTACK;
        }
        return distance <= loseTargetRange ? ZombieState.CHASE : ZombieState.IDLE;
    }

    private ZombieState fromAttack(double distance) {
        return distance <= attackRange ? ZombieState.ATTACK : ZombieState.CHASE;
    }
}
