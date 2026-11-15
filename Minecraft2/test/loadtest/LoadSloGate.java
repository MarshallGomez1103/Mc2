package loadtest;

import java.util.List;
import java.util.Map;

/** Gate del SLO desplegado. Esqueleto: todavía no evalúa ningún umbral. */
public final class LoadSloGate {
    private LoadSloGate() {
    }

    public record Thresholds(int repetitions, int clients, int zombies, double tickP95MaxMs, double rttP95MaxMs,
                             long maxTickFailures, long maxDeficitTicks, long maxNetworkErrors) {
        public static Thresholds contract() {
            return new Thresholds(0, 0, 0, 0, 0, 0, 0, 0);
        }

        public static Thresholds fromEnvironment(Map<String, String> environment) {
            return contract();
        }
    }

    public record Repetition(int index, int clientsConnected, int zombiesActiveMin, double tickP95Ms, long tickSamples,
                             double rttP95Ms, long rttSamples, long tickFailures, long deficitTicks, long networkErrors) {
    }

    public record Verdict(List<String> violations) {
        public boolean passed() {
            return true;
        }

        public int exitCode() {
            return 0;
        }
    }

    public static Verdict evaluate(Thresholds thresholds, List<Repetition> repetitions) {
        return new Verdict(List.of());
    }
}
