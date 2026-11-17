package loadtest;

import persistence.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Carga contra el servidor desplegado (C3-04.3): conecta N clientes del protocolo v1, mide la ida y vuelta
 * entrada → estado (SLO-RED-01) y lee {@code /metrics} antes y después de cada repetición (SLO-CARGA-01).
 * El veredicto y el código de salida los decide {@link LoadSloGate}.
 *
 * <p>Ida y vuelta: tiempo desde que el cliente envía {@code input{seq}} hasta que recibe el primer
 * {@code state} que la reconoce. Si el estado trae {@code ack} (último {@code seq} aplicado a ese
 * jugador), se usa; si no, el siguiente {@code state} recibido cierra todas las entradas pendientes.
 *
 * <p>Uso: {@code java -cp target/classes:target/test-classes loadtest.NetworkLoadClient}. Se configura con
 * las variables del contrato común ({@code MC2_SERVER_HOST}, {@code MC2_SERVER_PORT}, {@code MC2_HEALTH_PORT},
 * {@code MC2_LOAD_ZOMBIES}) y las propias de la carga ({@code MC2_LOAD_CLIENTS}, {@code MC2_LOAD_REPETITIONS},
 * {@code MC2_LOAD_SECONDS}, {@code MC2_LOAD_WARMUP_SECONDS}, {@code MC2_LOAD_OUT}).
 */
public final class NetworkLoadClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Configuración de una corrida. */
    public record Settings(String host, int gamePort, int healthPort, LoadSloGate.Thresholds thresholds,
                           Duration measure, Duration warmup, Duration clientWarmup, Path out) {
        public static Settings fromEnvironment(Map<String, String> environment) {
            return new Settings(
                    environment.getOrDefault("MC2_SERVER_HOST", "localhost"),
                    port(environment, "MC2_SERVER_PORT", 7777),
                    port(environment, "MC2_HEALTH_PORT", 8080),
                    LoadSloGate.Thresholds.fromEnvironment(environment),
                    Duration.ofSeconds(seconds(environment, "MC2_LOAD_SECONDS", 30)),
                    Duration.ofSeconds(seconds(environment, "MC2_LOAD_WARMUP_SECONDS", 30)),
                    Duration.ofSeconds(seconds(environment, "MC2_LOAD_CLIENT_WARMUP_SECONDS", 30)),
                    Path.of(environment.getOrDefault("MC2_LOAD_OUT", "target/load-results/slo")));
        }

        private static int port(Map<String, String> environment, String name, int fallback) {
            long value = seconds(environment, name, fallback);
            if (value < 1 || value > 65_535) throw new IllegalArgumentException(name + " fuera de rango: " + value);
            return (int) value;
        }

        private static long seconds(Map<String, String> environment, String name, long fallback) {
            String raw = environment.get(name);
            if (raw == null) return fallback;
            try {
                long value = Long.parseLong(raw.trim());
                if (value < 0) throw new IllegalArgumentException(name + " no puede ser negativo: " + raw);
                return value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " no es un entero: '" + raw + "'");
            }
        }
    }

    private NetworkLoadClient() {
    }

    public static void main(String[] args) throws Exception {
        Settings settings = Settings.fromEnvironment(System.getenv());
        int exit = run(settings);
        System.exit(exit);
    }

    /** Ejecuta todas las repeticiones, escribe {@code results.csv} y {@code verdict.txt} y devuelve el código de salida. */
    public static int run(Settings settings) throws IOException, InterruptedException {
        Files.createDirectories(settings.out());
        LoadSloGate.Thresholds t = settings.thresholds();
        System.out.printf(Locale.ROOT, "Carga contra %s:%d (métricas :%d): %d clientes, %d zombis, %d repeticiones de %d s%n",
                settings.host(), settings.gamePort(), settings.healthPort(), t.clients(), t.zombies(), t.repetitions(),
                settings.measure().toSeconds());
        List<LoadSloGate.Repetition> repetitions = new ArrayList<>();
        for (int i = 1; i <= t.repetitions(); i++) {
            LoadSloGate.Repetition r = repetition(settings, i);
            System.out.printf(Locale.ROOT, "repetición %d: clientes=%d zombis_min=%d tick_p95=%.3f ms (%d ticks) "
                            + "rtt_p95=%.3f ms (%d muestras) fallidos=%d deficit=%d errores_red=%d%n",
                    r.index(), r.clientsConnected(), r.zombiesActiveMin(), r.tickP95Ms(), r.tickSamples(), r.rttP95Ms(),
                    r.rttSamples(), r.tickFailures(), r.deficitTicks(), r.networkErrors());
            repetitions.add(r);
        }
        LoadSloGate.Verdict verdict = LoadSloGate.evaluate(t, repetitions);
        writeResults(settings, repetitions, verdict);
        if (verdict.passed()) {
            System.out.println("SLO cumplido en las " + repetitions.size() + " repeticiones");
        } else {
            verdict.violations().forEach(v -> System.out.println("INCUMPLE: " + v));
        }
        return verdict.exitCode();
    }

    private static LoadSloGate.Repetition repetition(Settings settings, int index) throws InterruptedException {
        LoadSloGate.Thresholds t = settings.thresholds();
        AtomicLong networkErrors = new AtomicLong();
        waitForPopulation(settings, t.zombies());

        List<SessionClient> clients = new ArrayList<>();
        for (int c = 0; c < t.clients(); c++) {
            SessionClient client = new SessionClient("carga-" + index + "-" + c, c % 3, networkErrors);
            if (client.connect(settings)) clients.add(client);
        }
        clients.forEach(SessionClient::startSending);
        // Calentamiento del generador de carga: se descartan las muestras de la conexión y del JIT del cliente,
        // que no son tiempo del servidor. La ventana medida (y la lectura inicial de /metrics) empieza después.
        if (!clients.isEmpty()) {
            Thread.sleep(settings.clientWarmup().toMillis());
            clients.forEach(SessionClient::discardSamples);
        }
        PrometheusSnapshot before = scrape(settings, networkErrors);

        int zombiesMin = Integer.MAX_VALUE;
        long end = System.nanoTime() + settings.measure().toNanos();
        do {
            PrometheusSnapshot sample = scrape(settings, networkErrors);
            double zombies = sample == null ? Double.NaN : sample.value("mc2_zombies_active");
            zombiesMin = Math.min(zombiesMin, Double.isFinite(zombies) ? (int) zombies : 0);
            Thread.sleep(Math.max(0, Math.min(1_000, (end - System.nanoTime()) / 1_000_000)));
        } while (System.nanoTime() < end);

        int stillConnected = 0;
        List<Double> rtts = new ArrayList<>();
        for (SessionClient client : clients) {
            if (client.isHealthy()) stillConnected++;
            client.close();
            rtts.addAll(client.roundTripsMs());
        }
        PrometheusSnapshot after = scrape(settings, networkErrors);

        double tickP95 = Double.NaN;
        long tickSamples = 0;
        long failures = 0;
        long deficit = 0;
        if (before != null && after != null) {
            tickP95 = after.histogramQuantileMs(before, "mc2_tick_duration_seconds", 0.95);
            tickSamples = finiteLong(after.delta(before, "mc2_tick_duration_seconds_count"));
            failures = finiteLong(after.delta(before, "mc2_tick_failures_total"));
            deficit = finiteLong(after.delta(before, "mc2_population_deficit_ticks_total"));
        }
        return new LoadSloGate.Repetition(index, stillConnected, zombiesMin == Integer.MAX_VALUE ? 0 : zombiesMin,
                tickP95, tickSamples, percentile(rtts, 95), rtts.size(), failures, deficit, networkErrors.get());
    }

    /** Espera a que el servidor tenga la población objetivo antes de medir; si no llega, la medición la registra. */
    private static void waitForPopulation(Settings settings, int zombies) throws InterruptedException {
        long deadline = System.nanoTime() + settings.warmup().toNanos();
        while (System.nanoTime() < deadline) {
            PrometheusSnapshot snapshot = scrape(settings, new AtomicLong());
            if (snapshot != null && snapshot.value("mc2_zombies_active") >= zombies) return;
            Thread.sleep(250);
        }
    }

    private static PrometheusSnapshot scrape(Settings settings, AtomicLong networkErrors) {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://" + settings.host() + ":" + settings.healthPort() + "/metrics"))
                    .timeout(CONNECT_TIMEOUT).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                networkErrors.incrementAndGet();
                return null;
            }
            return PrometheusSnapshot.parse(response.body());
        } catch (IOException e) {
            networkErrors.incrementAndGet();
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Percentil por el método del rango más cercano; NaN sin muestras. */
    static double percentile(List<Double> samples, int percentile) {
        if (samples.isEmpty()) return Double.NaN;
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(percentile / 100.0 * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    private static long finiteLong(double value) {
        return Double.isFinite(value) ? Math.round(value) : 0;
    }

    private static void writeResults(Settings settings, List<LoadSloGate.Repetition> repetitions,
                                     LoadSloGate.Verdict verdict) throws IOException {
        LoadSloGate.Thresholds t = settings.thresholds();
        StringBuilder csv = new StringBuilder("repetition,clients_connected,zombies_active_min,tick_p95_ms,tick_samples,"
                + "rtt_p95_ms,rtt_samples,tick_failures,deficit_ticks,network_errors\n");
        for (LoadSloGate.Repetition r : repetitions) {
            csv.append(String.format(Locale.ROOT, "%d,%d,%d,%.4f,%d,%.4f,%d,%d,%d,%d%n", r.index(), r.clientsConnected(),
                    r.zombiesActiveMin(), r.tickP95Ms(), r.tickSamples(), r.rttP95Ms(), r.rttSamples(), r.tickFailures(),
                    r.deficitTicks(), r.networkErrors()));
        }
        Files.writeString(settings.out().resolve("results.csv"), csv, StandardCharsets.UTF_8);
        StringBuilder text = new StringBuilder();
        text.append("finished_utc=").append(Instant.now()).append('\n');
        text.append(String.format(Locale.ROOT, "thresholds=repetitions:%d clients:%d zombies:%d tick_p95_ms:%.2f rtt_p95_ms:%.2f%n",
                t.repetitions(), t.clients(), t.zombies(), t.tickP95MaxMs(), t.rttP95MaxMs()));
        text.append("verdict=").append(verdict.passed() ? "CUMPLE" : "INCUMPLE").append('\n');
        verdict.violations().forEach(v -> text.append("violation=").append(v).append('\n'));
        Files.writeString(settings.out().resolve("verdict.txt"), text, StandardCharsets.UTF_8);
    }

    /** Un jugador del protocolo v1: envía entradas al ritmo del tick y mide cuándo un estado las reconoce. */
    private static final class SessionClient {
        private final String name;
        private final int skinId;
        private final AtomicLong networkErrors;
        private final ConcurrentSkipListMap<Long, Long> pending = new ConcurrentSkipListMap<>();
        private final ConcurrentLinkedQueue<Double> roundTrips = new ConcurrentLinkedQueue<>();
        private Socket socket;
        private BufferedWriter writer;
        private volatile boolean running;
        private volatile boolean failed;
        private int tickHz = 20;
        private Thread sender;
        private Thread reader;

        SessionClient(String name, int skinId, AtomicLong networkErrors) {
            this.name = name;
            this.skinId = skinId;
            this.networkErrors = networkErrors;
        }

        boolean connect(Settings settings) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(settings.host(), settings.gamePort()), (int) CONNECT_TIMEOUT.toMillis());
                socket.setTcpNoDelay(true);
                socket.setSoTimeout((int) CONNECT_TIMEOUT.toMillis());
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                send("{\"type\":\"hello\",\"name\":\"" + name + "\",\"skinId\":" + skinId + "}");
                String line;
                while ((line = in.readLine()) != null) {
                    Map<?, ?> message = parse(line);
                    if (message != null && "welcome".equals(message.get("type"))) {
                        if (message.get("tickHz") instanceof Number hz && hz.intValue() > 0) tickHz = hz.intValue();
                        break;
                    }
                }
                if (line == null) throw new IOException("el servidor cerró la conexión antes de welcome");
                running = true;
                reader = new Thread(() -> readStates(in), name + "-lector");
                reader.setDaemon(true);
                reader.start();
                return true;
            } catch (IOException e) {
                networkErrors.incrementAndGet();
                System.out.println(name + ": no se pudo conectar: " + e.getMessage());
                closeQuietly();
                return false;
            }
        }

        void startSending() {
            sender = new Thread(this::sendInputs, name + "-emisor");
            sender.setDaemon(true);
            sender.start();
        }

        private void sendInputs() {
            long periodNanos = 1_000_000_000L / tickHz;
            long seq = 0;
            long next = System.nanoTime();
            while (running) {
                seq++;
                boolean forward = (seq / tickHz) % 2 == 0;
                pending.put(seq, System.nanoTime());
                try {
                    send(String.format(Locale.ROOT, "{\"type\":\"input\",\"seq\":%d,\"forward\":%b,\"backward\":false,"
                            + "\"left\":false,\"right\":false,\"sprint\":false,\"jump\":false,\"deltaYaw\":0.5,"
                            + "\"deltaPitch\":0.0,\"primary\":false,\"secondary\":false,\"togglePistol\":false}", seq, forward));
                } catch (IOException e) {
                    // Si ya se pidió el cierre, el socket cerrado es el cierre propio y no un error de red.
                    if (running) fail(e);
                    return;
                }
                next += periodNanos;
                long sleep = next - System.nanoTime();
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }

        private void readStates(BufferedReader in) {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    long now = System.nanoTime();
                    Map<?, ?> message = parse(line);
                    if (message == null || !"state".equals(message.get("type"))) continue;
                    long acknowledged = message.get("ack") instanceof Number ack ? ack.longValue() : Long.MAX_VALUE;
                    var answered = pending.headMap(acknowledged, true);
                    for (Long sentAt : answered.values()) roundTrips.add((now - sentAt) / 1_000_000.0);
                    answered.clear();
                }
                if (running) fail(new IOException("el servidor cerró la conexión"));
            } catch (IOException e) {
                if (running) fail(e);
            }
        }

        private synchronized void send(String line) throws IOException {
            writer.write(line);
            writer.write('\n');
            writer.flush();
        }

        private void fail(IOException e) {
            if (!failed) {
                failed = true;
                networkErrors.incrementAndGet();
                System.out.println(name + ": error de red: " + e.getMessage());
            }
            running = false;
        }

        boolean isHealthy() {
            return running && !failed;
        }

        void discardSamples() {
            roundTrips.clear();
        }

        List<Double> roundTripsMs() {
            return new ArrayList<>(roundTrips);
        }

        void close() {
            boolean wasRunning = running;
            running = false;
            if (wasRunning) {
                try {
                    send("{\"type\":\"bye\"}");
                } catch (IOException ignored) {
                    // La conexión ya estaba cerrada; el error, si lo hubo, ya se contó.
                }
            }
            closeQuietly();
            try {
                if (sender != null) sender.join(1_000);
                if (reader != null) reader.join(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void closeQuietly() {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {
                // Cierre de mejor esfuerzo.
            }
        }

        private static Map<?, ?> parse(String line) {
            try {
                return JsonParser.parse(line) instanceof Map<?, ?> map ? map : null;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
