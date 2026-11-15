package loadtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lectura del formato de texto de Prometheus que expone /metrics del servidor. */
class PrometheusSnapshotTest {
    private static final String BEFORE = """
            # HELP mc2_tick_duration_seconds Duración del tick del servidor.
            # TYPE mc2_tick_duration_seconds histogram
            mc2_tick_duration_seconds_bucket{le="0.001"} 0
            mc2_tick_duration_seconds_bucket{le="0.002"} 0
            mc2_tick_duration_seconds_bucket{le="0.004"} 0
            mc2_tick_duration_seconds_bucket{le="+Inf"} 0
            mc2_tick_duration_seconds_sum 0
            mc2_tick_duration_seconds_count 0
            mc2_tick_failures_total 2
            mc2_zombies_active 0
            mc2_players_connected 0
            mc2_population_deficit_ticks_total 5
            """;

    private static final String AFTER = """
            # TYPE mc2_tick_duration_seconds histogram
            mc2_tick_duration_seconds_bucket{le="0.001"} 50
            mc2_tick_duration_seconds_bucket{le="0.002"} 90
            mc2_tick_duration_seconds_bucket{le="0.004"} 100
            mc2_tick_duration_seconds_bucket{le="+Inf"} 100
            mc2_tick_duration_seconds_sum 0.15
            mc2_tick_duration_seconds_count 100
            mc2_tick_failures_total 2
            mc2_zombies_active 80
            mc2_players_connected 3
            mc2_population_deficit_ticks_total 5
            """;

    @Test void readsGaugesAndCounters() {
        // Act
        var snapshot = PrometheusSnapshot.parse(AFTER);
        // Assert
        assertEquals(80, snapshot.value("mc2_zombies_active"));
        assertEquals(3, snapshot.value("mc2_players_connected"));
        assertEquals(2, snapshot.value("mc2_tick_failures_total"));
    }

    @Test void anAbsentMetricIsNaNNotZero() {
        assertTrue(Double.isNaN(PrometheusSnapshot.parse(AFTER).value("mc2_no_existe")));
    }

    @Test void counterDeltaBetweenTwoSnapshots() {
        // Arrange
        var before = PrometheusSnapshot.parse(BEFORE);
        var after = PrometheusSnapshot.parse(AFTER);
        // Act / Assert: los contadores no cambiaron durante la repetición.
        assertEquals(0, after.delta(before, "mc2_tick_failures_total"));
        assertEquals(0, after.delta(before, "mc2_population_deficit_ticks_total"));
        assertEquals(100, after.delta(before, "mc2_tick_duration_seconds_count"));
    }

    @Test void p95IsInterpolatedInsideTheBucketLikeHistogramQuantile() {
        // Arrange: 100 ticks; el puesto 95 cae en el cubo (0,002; 0,004] que va de 90 a 100.
        var before = PrometheusSnapshot.parse(BEFORE);
        var after = PrometheusSnapshot.parse(AFTER);
        // Act
        double p95Ms = after.histogramQuantileMs(before, "mc2_tick_duration_seconds", 0.95);
        // Assert: 2 ms + (4 − 2) ms × (95 − 90) / (100 − 90) = 3 ms.
        assertEquals(3.0, p95Ms, 1e-9);
    }

    @Test void quantileUsesOnlyTicksOfTheRepetition() {
        // Arrange: la primera lectura ya tenía 100 ticks rápidos; la repetición añade 10 lentos.
        var before = PrometheusSnapshot.parse(AFTER);
        var after = PrometheusSnapshot.parse(AFTER
                .replace("{le=\"0.004\"} 100", "{le=\"0.004\"} 100")
                .replace("{le=\"+Inf\"} 100", "{le=\"+Inf\"} 110")
                .replace("_count 100", "_count 110"));
        // Act
        double p95Ms = after.histogramQuantileMs(before, "mc2_tick_duration_seconds", 0.95);
        // Assert: todos los ticks de la repetición superan el último límite finito; se informa ese límite.
        assertEquals(4.0, p95Ms, 1e-9);
    }

    @Test void quantileWithoutSamplesIsNaN() {
        var snapshot = PrometheusSnapshot.parse(BEFORE);
        assertTrue(Double.isNaN(snapshot.histogramQuantileMs(snapshot, "mc2_tick_duration_seconds", 0.95)));
    }

    @Test void quantileOfAnAbsentHistogramIsNaN() {
        var snapshot = PrometheusSnapshot.parse("mc2_zombies_active 1\n");
        assertTrue(Double.isNaN(snapshot.histogramQuantileMs(snapshot, "mc2_tick_duration_seconds", 0.95)));
    }

    @Test void toleratesExtraLabelsScientificNotationAndCarriageReturns() {
        // Arrange
        var text = "mc2_tick_duration_seconds_bucket{instance=\"a\",le=\"1.0E-3\"} 4\r\n"
                + "mc2_tick_duration_seconds_bucket{le=\"+Inf\",instance=\"a\"} 4\r\n"
                + "mc2_zombies_active 8.0e1\r\n";
        // Act
        var snapshot = PrometheusSnapshot.parse(text);
        var empty = PrometheusSnapshot.parse("");
        // Assert
        assertEquals(80, snapshot.value("mc2_zombies_active"));
        // El primer cubo se interpola desde 0: puesto 2 de 4 en (0; 1 ms] → 0,5 ms.
        assertEquals(0.5, snapshot.histogramQuantileMs(empty, "mc2_tick_duration_seconds", 0.5), 1e-9);
    }
}
