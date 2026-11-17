package loadtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gate del SLO desplegado: decide si una corrida de carga contra el servidor cumple
 * SLO-CARGA-01 (tick de IA) y SLO-RED-01 (ida y vuelta entrada → estado).
 *
 * <p>Es lógica pura: no abre red ni lee archivos. {@link NetworkLoadClient} mide cada repetición y
 * este gate convierte las mediciones en un veredicto y un código de salida. Una medición ausente
 * (NaN o sin muestras) cuenta como incumplimiento: no se puede aprobar lo que no se midió.
 */
public final class LoadSloGate {
    /** Código de salida cuando la corrida incumple el SLO o le falta un resultado. */
    public static final int EXIT_VIOLATION = 1;

    private LoadSloGate() {
    }

    /**
     * Umbrales del gate. Los valores por defecto son los del contrato común (TODO.md, CP0); cada uno
     * puede sustituirse por una variable de entorno para romperlo a propósito y demostrar que el job falla.
     */
    public record Thresholds(int repetitions, int clients, int zombies, double tickP95MaxMs, double rttP95MaxMs,
                             long maxTickFailures, long maxDeficitTicks, long maxNetworkErrors) {
        public static Thresholds contract() {
            return new Thresholds(3, 3, 80, 16.67, 100.0, 0, 0, 0);
        }

        /**
         * Lee {@code MC2_LOAD_REPETITIONS}, {@code MC2_LOAD_CLIENTS}, {@code MC2_LOAD_ZOMBIES},
         * {@code MC2_SLO_TICK_P95_MS} y {@code MC2_SLO_RTT_P95_MS}. Una variable ausente conserva el contrato;
         * un valor vacío, no numérico, negativo o no finito se rechaza en lugar de ignorarse.
         */
        public static Thresholds fromEnvironment(Map<String, String> environment) {
            Thresholds base = contract();
            return new Thresholds(
                    (int) wholeNumber(environment, "MC2_LOAD_REPETITIONS", base.repetitions(), 1),
                    (int) wholeNumber(environment, "MC2_LOAD_CLIENTS", base.clients(), 1),
                    (int) wholeNumber(environment, "MC2_LOAD_ZOMBIES", base.zombies(), 0),
                    milliseconds(environment, "MC2_SLO_TICK_P95_MS", base.tickP95MaxMs()),
                    milliseconds(environment, "MC2_SLO_RTT_P95_MS", base.rttP95MaxMs()),
                    base.maxTickFailures(), base.maxDeficitTicks(), base.maxNetworkErrors());
        }

        private static long wholeNumber(Map<String, String> environment, String name, long fallback, long minimum) {
            String raw = environment.get(name);
            if (raw == null) return fallback;
            try {
                long value = Long.parseLong(raw.trim());
                if (value < minimum || value > Integer.MAX_VALUE) throw invalid(name, raw);
                return value;
            } catch (NumberFormatException e) {
                throw invalid(name, raw);
            }
        }

        private static double milliseconds(Map<String, String> environment, String name, double fallback) {
            String raw = environment.get(name);
            if (raw == null) return fallback;
            try {
                double value = Double.parseDouble(raw.trim());
                if (!Double.isFinite(value) || value <= 0) throw invalid(name, raw);
                return value;
            } catch (NumberFormatException e) {
                throw invalid(name, raw);
            }
        }

        private static IllegalArgumentException invalid(String name, String raw) {
            return new IllegalArgumentException(name + " no es un valor válido: '" + raw + "'");
        }
    }

    /**
     * Mediciones de una repetición. {@code tickP95Ms} sale del histograma {@code mc2_tick_duration_seconds}
     * de {@code /metrics} (diferencia entre el inicio y el fin de la repetición); {@code rttP95Ms}, de los
     * clientes de red. Los contadores son incrementos durante la repetición.
     */
    public record Repetition(int index, int clientsConnected, int zombiesActiveMin, double tickP95Ms, long tickSamples,
                             double rttP95Ms, long rttSamples, long tickFailures, long deficitTicks, long networkErrors) {
    }

    /** Resultado del gate: aprobado si y solo si no hay incumplimientos. */
    public record Verdict(List<String> violations) {
        public Verdict {
            violations = List.copyOf(violations);
        }

        public boolean passed() {
            return violations.isEmpty();
        }

        public int exitCode() {
            return passed() ? 0 : EXIT_VIOLATION;
        }
    }

    /** Evalúa todas las repeticiones e informa cada incumplimiento, no solo el primero. */
    public static Verdict evaluate(Thresholds thresholds, List<Repetition> repetitions) {
        List<String> violations = new ArrayList<>();
        if (repetitions.size() < thresholds.repetitions()) {
            violations.add("faltan repeticiones: " + repetitions.size() + " de " + thresholds.repetitions());
        }
        for (Repetition r : repetitions) {
            String where = "repetición " + r.index() + ": ";
            if (r.clientsConnected() < thresholds.clients() || r.zombiesActiveMin() < thresholds.zombies()) {
                violations.add(where + "población no alcanzada (clientes " + r.clientsConnected() + "/" + thresholds.clients()
                        + ", zombis mínimos " + r.zombiesActiveMin() + "/" + thresholds.zombies() + ")");
            }
            if (missing(r.tickP95Ms(), r.tickSamples())) {
                violations.add(where + "falta la medición del tick (mc2_tick_duration_seconds sin muestras)");
            } else if (r.tickP95Ms() > thresholds.tickP95MaxMs()) {
                violations.add(where + "SLO-CARGA-01 p95 del tick " + r.tickP95Ms() + " ms > " + thresholds.tickP95MaxMs() + " ms");
            }
            if (missing(r.rttP95Ms(), r.rttSamples())) {
                violations.add(where + "falta la medición de ida y vuelta (sin respuestas de estado)");
            } else if (r.rttP95Ms() > thresholds.rttP95MaxMs()) {
                violations.add(where + "SLO-RED-01 p95 de ida y vuelta " + r.rttP95Ms() + " ms > " + thresholds.rttP95MaxMs() + " ms");
            }
            if (r.tickFailures() > thresholds.maxTickFailures()) {
                violations.add(where + "ticks fallidos " + r.tickFailures() + " > " + thresholds.maxTickFailures());
            }
            if (r.deficitTicks() > thresholds.maxDeficitTicks()) {
                violations.add(where + "ticks con déficit de población " + r.deficitTicks() + " > " + thresholds.maxDeficitTicks());
            }
            if (r.networkErrors() > thresholds.maxNetworkErrors()) {
                violations.add(where + "errores de red " + r.networkErrors() + " > " + thresholds.maxNetworkErrors());
            }
        }
        return new Verdict(violations);
    }

    private static boolean missing(double value, long samples) {
        return samples <= 0 || !Double.isFinite(value);
    }
}
