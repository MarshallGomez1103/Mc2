package loadtest;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lectura de {@code /metrics} en el formato de texto de Prometheus, sin dependencias externas.
 *
 * <p>Solo interpreta lo que necesita el gate del SLO: valores sin etiquetas (gauges y contadores) y los
 * cubos {@code _bucket{le="..."}} de un histograma. El cuantil se calcula sobre la diferencia entre dos
 * lecturas, con la misma interpolación lineal que {@code histogram_quantile} de PromQL, para que el p95
 * corresponda solo a los ticks de la repetición medida.
 */
public final class PrometheusSnapshot {
    private static final Pattern SAMPLE = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{([^}]*)})?\\s+(\\S+)");
    private static final Pattern LE = Pattern.compile("(?:^|,)\\s*le=\"([^\"]*)\"");

    private final Map<String, Double> plain;
    private final Map<String, TreeMap<Double, Double>> buckets;

    private PrometheusSnapshot(Map<String, Double> plain, Map<String, TreeMap<Double, Double>> buckets) {
        this.plain = plain;
        this.buckets = buckets;
    }

    public static PrometheusSnapshot parse(String text) {
        Map<String, Double> plain = new HashMap<>();
        Map<String, TreeMap<Double, Double>> buckets = new HashMap<>();
        for (String rawLine : text.split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            Matcher sample = SAMPLE.matcher(line);
            if (!sample.find()) continue;
            String name = sample.group(1);
            String labels = sample.group(3);
            double value = number(sample.group(4));
            if (name.endsWith("_bucket") && labels != null) {
                Matcher le = LE.matcher(labels);
                if (le.find()) {
                    String histogram = name.substring(0, name.length() - "_bucket".length());
                    buckets.computeIfAbsent(histogram, k -> new TreeMap<>()).merge(number(le.group(1)), value, Double::sum);
                }
            } else {
                plain.merge(name, value, Double::sum);
            }
        }
        return new PrometheusSnapshot(plain, buckets);
    }

    /** Valor de una serie; NaN si no aparece (una métrica ausente no se confunde con cero). */
    public double value(String name) {
        return plain.getOrDefault(name, Double.NaN);
    }

    /** Incremento de un contador entre {@code before} y esta lectura; una lectura previa sin la serie cuenta como 0. */
    public double delta(PrometheusSnapshot before, String name) {
        double previous = before.plain.getOrDefault(name, 0.0);
        return value(name) - previous;
    }

    /**
     * Cuantil en milisegundos de un histograma en segundos, sobre los ticks registrados entre {@code before}
     * y esta lectura. NaN si el histograma no existe o no hubo muestras. Si el cuantil cae en el cubo
     * {@code +Inf} se devuelve el último límite finito, como hace PromQL.
     */
    public double histogramQuantileMs(PrometheusSnapshot before, String histogram, double quantile) {
        TreeMap<Double, Double> now = buckets.get(histogram);
        if (now == null || now.isEmpty()) return Double.NaN;
        TreeMap<Double, Double> then = before.buckets.getOrDefault(histogram, new TreeMap<>());
        double total = now.lastEntry().getValue() - then.getOrDefault(now.lastKey(), 0.0);
        if (!(total > 0)) return Double.NaN;

        double rank = quantile * total;
        double lowerBound = 0;
        double lowerCount = 0;
        for (Map.Entry<Double, Double> bucket : now.entrySet()) {
            double upperBound = bucket.getKey();
            double count = bucket.getValue() - then.getOrDefault(upperBound, 0.0);
            if (count >= rank) {
                if (Double.isInfinite(upperBound)) return lowerBound * 1000.0;
                if (count == lowerCount) return upperBound * 1000.0;
                return (lowerBound + (upperBound - lowerBound) * (rank - lowerCount) / (count - lowerCount)) * 1000.0;
            }
            lowerBound = upperBound;
            lowerCount = count;
        }
        return lowerBound * 1000.0;
    }

    private static double number(String raw) {
        return switch (raw) {
            case "+Inf", "Inf" -> Double.POSITIVE_INFINITY;
            case "-Inf" -> Double.NEGATIVE_INFINITY;
            default -> Double.parseDouble(raw);
        };
    }
}
