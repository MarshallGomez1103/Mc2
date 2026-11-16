package loadtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Gate del SLO desplegado (SLO-CARGA-01 y SLO-RED-01): decide el código de salida del script de carga. */
class LoadSloGateTest {
    private static final LoadSloGate.Thresholds CONTRACT = LoadSloGate.Thresholds.contract();

    private static LoadSloGate.Repetition compliant(int index) {
        // 80 zombis activos, 3 clientes, p95 del tick 2 ms y p95 de ida y vuelta 12 ms, sin errores.
        return new LoadSloGate.Repetition(index, 3, 80, 2.0, 1_200, 12.0, 600, 0, 0, 0);
    }

    @Test void threeCompliantRepetitionsPassWithExitCodeZero() {
        // Arrange
        var repetitions = List.of(compliant(1), compliant(2), compliant(3));
        // Act
        var verdict = LoadSloGate.evaluate(CONTRACT, repetitions);
        // Assert
        assertTrue(verdict.passed(), verdict.violations()::toString);
        assertEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().isEmpty());
    }

    @Test void contractValuesMatchTheCommonContract() {
        assertEquals(3, CONTRACT.repetitions());
        assertEquals(80, CONTRACT.zombies());
        assertEquals(16.67, CONTRACT.tickP95MaxMs());
        assertEquals(100.0, CONTRACT.rttP95MaxMs());
        assertEquals(0, CONTRACT.maxTickFailures());
        assertEquals(0, CONTRACT.maxDeficitTicks());
        assertEquals(0, CONTRACT.maxNetworkErrors());
    }

    @Test void aMissingRepetitionFails() {
        // Arrange: solo dos de las tres repeticiones obligatorias.
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(compliant(1), compliant(2)));
        // Assert
        assertFalse(verdict.passed());
        assertNotEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().stream().anyMatch(v -> v.contains("repeticiones")), verdict.violations()::toString);
    }

    @Test void noRepetitionsAtAllFails() {
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of()).exitCode());
    }

    @Test void aMissingTickMeasurementFails() {
        // Arrange: /metrics no devolvió el histograma del tick (sin muestras y p95 desconocido).
        var missing = new LoadSloGate.Repetition(2, 3, 80, Double.NaN, 0, 12.0, 600, 0, 0, 0);
        // Act
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(compliant(1), missing, compliant(3)));
        // Assert
        assertNotEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().stream().anyMatch(v -> v.contains("repetición 2")), verdict.violations()::toString);
    }

    @Test void aMissingRoundTripMeasurementFails() {
        var missing = new LoadSloGate.Repetition(3, 3, 80, 2.0, 1_200, Double.NaN, 0, 0, 0, 0);
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(compliant(1), compliant(2), missing)).exitCode());
    }

    @Test void zombiePopulationBelowTargetFails() {
        // Arrange: el servidor nunca llegó a 80 zombis activos.
        var shortPopulation = new LoadSloGate.Repetition(1, 3, 79, 2.0, 1_200, 12.0, 600, 0, 0, 0);
        // Act
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(shortPopulation, compliant(2), compliant(3)));
        // Assert
        assertNotEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().stream().anyMatch(v -> v.contains("población")), verdict.violations()::toString);
    }

    @Test void clientsBelowTargetFail() {
        var fewClients = new LoadSloGate.Repetition(1, 2, 80, 2.0, 1_200, 12.0, 600, 0, 0, 0);
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(fewClients, compliant(2), compliant(3))).exitCode());
    }

    @Test void tickP95AboveBudgetFails() {
        var slowTick = new LoadSloGate.Repetition(2, 3, 80, 16.68, 1_200, 12.0, 600, 0, 0, 0);
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(compliant(1), slowTick, compliant(3)));
        assertNotEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().stream().anyMatch(v -> v.contains("SLO-CARGA-01")), verdict.violations()::toString);
    }

    @Test void tickP95ExactlyAtBudgetPasses() {
        var atBudget = new LoadSloGate.Repetition(1, 3, 80, 16.67, 1_200, 100.0, 600, 0, 0, 0);
        assertEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(atBudget, compliant(2), compliant(3))).exitCode());
    }

    @Test void roundTripP95AboveBudgetFails() {
        var slowNetwork = new LoadSloGate.Repetition(3, 3, 80, 2.0, 1_200, 100.01, 600, 0, 0, 0);
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(compliant(1), compliant(2), slowNetwork));
        assertNotEquals(0, verdict.exitCode());
        assertTrue(verdict.violations().stream().anyMatch(v -> v.contains("SLO-RED-01")), verdict.violations()::toString);
    }

    @Test void anyFailedTickFails() {
        var failed = new LoadSloGate.Repetition(1, 3, 80, 2.0, 1_200, 12.0, 600, 1, 0, 0);
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(failed, compliant(2), compliant(3))).exitCode());
    }

    @Test void anyDeficitTickFails() {
        var deficit = new LoadSloGate.Repetition(1, 3, 80, 2.0, 1_200, 12.0, 600, 0, 1, 0);
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(deficit, compliant(2), compliant(3))).exitCode());
    }

    @Test void anyNetworkErrorFails() {
        var networkError = new LoadSloGate.Repetition(1, 3, 80, 2.0, 1_200, 12.0, 600, 0, 0, 1);
        assertNotEquals(0, LoadSloGate.evaluate(CONTRACT, List.of(networkError, compliant(2), compliant(3))).exitCode());
    }

    @Test void everyViolationIsReportedNotOnlyTheFirst() {
        // Arrange: una repetición rompe cuatro umbrales a la vez.
        var broken = new LoadSloGate.Repetition(1, 3, 10, 40.0, 1_200, 110.0, 600, 2, 0, 0);
        // Act
        var verdict = LoadSloGate.evaluate(CONTRACT, List.of(broken, compliant(2), compliant(3)));
        // Assert
        assertEquals(4, verdict.violations().size(), verdict.violations()::toString);
    }

    @Test void environmentOverridesAThresholdToBreakItOnPurpose() {
        // Arrange: el umbral roto a propósito para la evidencia del job que falla.
        var thresholds = LoadSloGate.Thresholds.fromEnvironment(Map.of("MC2_SLO_TICK_P95_MS", "0.5"));
        // Act
        var verdict = LoadSloGate.evaluate(thresholds, List.of(compliant(1), compliant(2), compliant(3)));
        // Assert
        assertEquals(0.5, thresholds.tickP95MaxMs());
        assertNotEquals(0, verdict.exitCode());
    }

    @Test void environmentWithoutOverridesKeepsTheContract() {
        assertEquals(CONTRACT, LoadSloGate.Thresholds.fromEnvironment(Map.of()));
    }

    @Test void loadZombiesFromTheContractVariableSetsThePopulationTarget() {
        assertEquals(20, LoadSloGate.Thresholds.fromEnvironment(Map.of("MC2_LOAD_ZOMBIES", "20")).zombies());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "-1", "NaN", "Infinity"})
    void invalidOverridesAreRejected(String invalid) {
        assertThrows(IllegalArgumentException.class,
                () -> LoadSloGate.Thresholds.fromEnvironment(Map.of("MC2_SLO_RTT_P95_MS", invalid)));
    }
}
