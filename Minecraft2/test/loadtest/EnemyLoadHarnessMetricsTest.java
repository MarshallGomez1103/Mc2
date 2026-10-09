package loadtest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyLoadHarnessMetricsTest {
    @Test void failedTicksCountAsErrorsButDoNotLowerLatencyPercentiles() {
        // Arrange: dos ticks correctos y uno fallido.
        var stats = new EnemyLoadHarness.Stats();
        stats.record(1_000_000, 80);
        stats.errors++;
        stats.record(-1, 79);
        stats.record(3_000_000, 80);
        // Assert: latencia en ms; capacidad solo sobre tiempo de updates correctos.
        assertEquals(3, stats.attempted);
        assertEquals(2, stats.frames);
        assertEquals(2, stats.meanMs());
        assertEquals(3, stats.percentileMs(95));
        assertEquals(3, stats.maxMs());
        assertEquals(500, stats.capacityTicksPerSecond());
        assertEquals(1.0 / 3, stats.errorRate());
        assertEquals(239.0 / 3, stats.averageActive());
        assertEquals(79, stats.minActive);
    }
    @Test void recoveryRejectsInvalidWallLimitsBeforeCreatingAnOutput() {
        for (String invalid : new String[] {"0", "-1", "NaN", "Infinity", "-Infinity"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> EnemyLoadHarness.main(new String[] {"REC_BASELINE", invalid}));
        }
    }
}
