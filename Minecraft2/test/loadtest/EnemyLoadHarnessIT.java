package loadtest;

import domain.enemy.Difficulty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba población viva real con IA/mundo, sin ejecutar el protocolo de carga. */
class EnemyLoadHarnessIT {
    @Test void deadRegisteredZombiesDoNotReplaceTheTargetLivingPopulation() {
        // Arrange
        var session = new EnemyLoadHarness.Session(4, Difficulty.NORMAL, false);
        session.refill(3);
        assertEquals(3, session.activeCount());
        session.enemies.zombies().forEach(z -> z.takeDamage(z.getHealth()));
        assertEquals(0, session.activeCount());
        // Act: aún hay cadáveres registrados; deben aparecer tres zombis vivos nuevos.
        session.refill(3);
        // Assert
        assertEquals(3, session.activeCount());
        assertTrue(session.enemies.zombies().size() > 3);
        var stats = session.runConstant(3, 2.0 / 60);
        assertEquals(2, stats.attempted);
        assertEquals(3, stats.minActive);
        assertEquals(0, stats.populationShortfallTicks);
        assertEquals(0, stats.errors);
    }
}
