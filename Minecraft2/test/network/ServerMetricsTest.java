package network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerMetricsTest {
    @Test void cumulativeBucketsIncludeZerosAndFailuresDoNotBecomeLatencySamples() {
        ServerMetrics metrics = new ServerMetrics();
        assertTrue(metrics.render().contains("mc2_tick_duration_seconds_count 0\n"));
        metrics.record(1_000_000, true, 80, 80, 1);
        metrics.record(20_000_000, true, 80, 80, 1);
        metrics.record(500_000, false, 79, 80, 0);
        String text = metrics.render();
        assertTrue(text.contains("mc2_tick_duration_seconds_bucket{le=\"0.01667\"} 1\n"));
        assertTrue(text.contains("mc2_tick_duration_seconds_bucket{le=\"+Inf\"} 2\n"));
        assertTrue(text.contains("mc2_tick_duration_seconds_count 2\n"));
        assertTrue(text.contains("mc2_tick_failures_total 1\n"));
        assertTrue(text.contains("mc2_population_deficit_ticks_total 1\n"));
        assertTrue(text.contains("mc2_zombies_active 79\n"));
        assertTrue(text.contains("mc2_players_connected 0\n"));
    }
}
