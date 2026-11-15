package loadtest;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Doble de prueba del servidor: habla el protocolo v1 del contrato común (líneas JSON por TCP) y expone
 * {@code /metrics} con las métricas del contrato. Solo existe para probar {@link NetworkLoadClient} sin el
 * servidor real; no reemplaza la prueba contra el entorno desplegado. Emite estado cada 10 ms para que la
 * prueba verifique la medición y el veredicto sin depender de la espera de medio tick de un servidor a 20 Hz.
 */
final class FakeGameServer implements AutoCloseable {
    private static final Pattern SEQ = Pattern.compile("\"seq\"\\s*:\\s*(\\d+)");
    private static final double[] BOUNDS = {0.001, 0.002, 0.004, 0.008, 0.016, 0.032};

    private final ServerSocket game;
    private final HttpServer http;
    private final int zombies;
    private final double tickSeconds;
    private final Map<Socket, AtomicLong> clients = new ConcurrentHashMap<>();
    private final long[] bucketCounts = new long[BOUNDS.length];
    private volatile long ticks;
    private volatile boolean running = true;
    private final Thread ticker;
    private final Thread acceptor;

    FakeGameServer(int zombies, double tickSeconds) throws IOException {
        this.zombies = zombies;
        this.tickSeconds = tickSeconds;
        game = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        http.createContext("/metrics", exchange -> {
            byte[] body = metrics().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        http.start();
        acceptor = new Thread(this::accept, "fake-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        ticker = new Thread(this::tick, "fake-ticker");
        ticker.setDaemon(true);
        ticker.start();
    }

    int gamePort() {
        return game.getLocalPort();
    }

    int healthPort() {
        return http.getAddress().getPort();
    }

    private void accept() {
        while (running) {
            try {
                Socket socket = game.accept();
                Thread session = new Thread(() -> serve(socket), "fake-session");
                session.setDaemon(true);
                session.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket socket) {
        try (socket; BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("\"hello\"")) {
                    write(socket, "{\"type\":\"welcome\",\"playerId\":" + socket.getPort() + ",\"tickHz\":20}");
                    clients.put(socket, new AtomicLong());
                } else if (line.contains("\"input\"")) {
                    Matcher seq = SEQ.matcher(line);
                    AtomicLong last = clients.get(socket);
                    if (seq.find() && last != null) last.set(Long.parseLong(seq.group(1)));
                } else if (line.contains("\"bye\"")) {
                    break;
                }
            }
        } catch (IOException ignored) {
            // El cliente se desconectó.
        } finally {
            clients.remove(socket);
        }
    }

    private void tick() {
        while (running) {
            synchronized (bucketCounts) {
                for (int i = 0; i < BOUNDS.length; i++) if (tickSeconds <= BOUNDS[i]) bucketCounts[i]++;
                ticks++;
            }
            for (Map.Entry<Socket, AtomicLong> client : clients.entrySet()) {
                write(client.getKey(), "{\"type\":\"state\",\"tick\":" + ticks + ",\"ack\":" + client.getValue().get()
                        + ",\"players\":[],\"zombies\":[]}");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static void write(Socket socket, String line) {
        try {
            synchronized (socket) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
                out.print(line + "\n");
                out.flush();
            }
        } catch (IOException ignored) {
            // El cliente cerró la conexión entre dos ticks.
        }
    }

    private String metrics() {
        StringBuilder text = new StringBuilder("# TYPE mc2_tick_duration_seconds histogram\n");
        synchronized (bucketCounts) {
            for (int i = 0; i < BOUNDS.length; i++) {
                text.append(String.format(Locale.ROOT, "mc2_tick_duration_seconds_bucket{le=\"%s\"} %d%n", BOUNDS[i], bucketCounts[i]));
            }
            text.append("mc2_tick_duration_seconds_bucket{le=\"+Inf\"} ").append(ticks).append('\n');
            text.append(String.format(Locale.ROOT, "mc2_tick_duration_seconds_sum %.6f%n", ticks * tickSeconds));
            text.append("mc2_tick_duration_seconds_count ").append(ticks).append('\n');
        }
        text.append("mc2_tick_failures_total 0\n");
        text.append("mc2_zombies_active ").append(zombies).append('\n');
        text.append("mc2_players_connected ").append(clients.size()).append('\n');
        text.append("mc2_population_deficit_ticks_total 0\n");
        return text.toString();
    }

    @Override
    public void close() throws IOException {
        running = false;
        ticker.interrupt();
        http.stop(0);
        game.close();
        for (Socket socket : clients.keySet()) socket.close();
    }
}
