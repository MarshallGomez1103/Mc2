package domain.enemy;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** One deterministic RNG draw per deep landing, injectable for repeatable policy tests. */
public final class KamikazePolicy {
    public static final double DEEP_FALL_DISTANCE = 8.0;
    public static final double CRAWLER_PROBABILITY = 0.30;
    private final DoubleSupplier random;

    public KamikazePolicy(DoubleSupplier random) { this.random = Objects.requireNonNull(random); }

    public void onLanding(Zombie zombie, double fallDistance) {
        if (fallDistance + 1e-5 < DEEP_FALL_DISTANCE || !zombie.isAlive()) return;
        zombie.markKamikaze();
        if (random.getAsDouble() < CRAWLER_PROBABILITY) zombie.becomeCrawler();
        else zombie.shatter();
    }
}
