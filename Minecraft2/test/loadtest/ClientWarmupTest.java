package loadtest;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientWarmupTest {
    @Test void populationWaitAndActualClientWarmupAreIndependent() {
        var defaults = NetworkLoadClient.Settings.fromEnvironment(Map.of());
        assertEquals(Duration.ofSeconds(30), defaults.clientWarmup());
        var configured = NetworkLoadClient.Settings.fromEnvironment(Map.of(
                "MC2_LOAD_WARMUP_SECONDS", "60", "MC2_LOAD_CLIENT_WARMUP_SECONDS", "5"));
        assertEquals(Duration.ofSeconds(60), configured.warmup());
        assertEquals(Duration.ofSeconds(5), configured.clientWarmup());
    }
    @Test void rejectsNegativeClientWarmup() {
        assertThrows(IllegalArgumentException.class, () -> NetworkLoadClient.Settings.fromEnvironment(
                Map.of("MC2_LOAD_CLIENT_WARMUP_SECONDS", "-1")));
    }
}
