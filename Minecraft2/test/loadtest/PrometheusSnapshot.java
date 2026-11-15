package loadtest;

/** Lectura de /metrics en formato de texto de Prometheus. Esqueleto: todavía no interpreta nada. */
public final class PrometheusSnapshot {
    private PrometheusSnapshot() {
    }

    public static PrometheusSnapshot parse(String text) {
        return new PrometheusSnapshot();
    }

    public double value(String name) {
        return 0;
    }

    public double delta(PrometheusSnapshot before, String name) {
        return -1;
    }

    public double histogramQuantileMs(PrometheusSnapshot before, String histogram, double quantile) {
        return 0;
    }
}
