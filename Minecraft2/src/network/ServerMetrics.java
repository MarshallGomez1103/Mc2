package network;

/** One coherent Prometheus snapshot; failed ticks never become successful latency samples. */
public final class ServerMetrics {
    private static final double[] LIMITS = {.0001,.00025,.0005,.001,.002,.004,.008,.012,.01667,.02,.025,.05,.1,.25,.5,1,Double.POSITIVE_INFINITY};
    private final long[] buckets = new long[LIMITS.length];
    private long count, failures, deficit;
    private double seconds;
    private int zombies, players;
    public synchronized void record(long nanos, boolean success, int active, int target, int connected) {
        zombies = active; players = connected;
        if (active < target) deficit++;
        if (!success) { failures++; return; }
        double elapsed = nanos / 1e9;
        count++; seconds += elapsed;
        for (int i = 0; i < LIMITS.length; i++) if (elapsed <= LIMITS[i]) buckets[i]++;
    }
    public synchronized String render() {
        StringBuilder out = new StringBuilder("# TYPE mc2_tick_duration_seconds histogram\n");
        for (int i = 0; i < LIMITS.length; i++) {
            out.append("mc2_tick_duration_seconds_bucket{le=\"")
                    .append(Double.isInfinite(LIMITS[i]) ? "+Inf" : Double.toString(LIMITS[i]))
                    .append("\"} ").append(buckets[i]).append('\n');
        }
        out.append("mc2_tick_duration_seconds_count ").append(count).append('\n');
        out.append("mc2_tick_duration_seconds_sum ").append(seconds).append('\n');
        sample(out, "mc2_tick_failures_total", "counter", failures);
        sample(out, "mc2_zombies_active", "gauge", zombies);
        sample(out, "mc2_players_connected", "gauge", players);
        sample(out, "mc2_population_deficit_ticks_total", "counter", deficit);
        return out.toString();
    }
    private static void sample(StringBuilder out, String name, String type, long value) {
        out.append("# TYPE ").append(name).append(' ').append(type).append('\n')
                .append(name).append(' ').append(value).append('\n');
    }
}
