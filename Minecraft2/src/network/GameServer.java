package network;

import application.PlayerFrameInput;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded socket adapter: readers enqueue intentions, one tick thread owns the world, writers send snapshots. */
public final class GameServer implements AutoCloseable {
    private final ServerConfig config;
    private final ServerSimulation simulation;
    private final ServerMetrics metrics = new ServerMetrics();
    private final ServerSocket game;
    private final HttpServer http;
    private final ScheduledExecutorService ticks = Executors.newSingleThreadScheduledExecutor(threads("mc2-tick"));
    private final ThreadPoolExecutor readers = workers("mc2-reader", 8);
    private final ThreadPoolExecutor writers = workers("mc2-writer", 8);
    private final ThreadPoolExecutor requests = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), threads("mc2-http"), new ThreadPoolExecutor.AbortPolicy());
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final Thread acceptor;
    private volatile long lastTick;
    private volatile boolean fatal;
    private final Map<Integer, Connection> players = new java.util.LinkedHashMap<>(); // tick only
    private long tick;

    public GameServer(ServerConfig config) throws IOException {
        this.config = config;
        simulation = new ServerSimulation(config);
        game = new ServerSocket();
        HttpServer candidate = null;
        try {
            game.setReuseAddress(true);
            game.bind(new InetSocketAddress(config.gamePort()), 8);
            candidate = HttpServer.create(new InetSocketAddress(config.healthPort()), 8);
            http = candidate;
            http.setExecutor(requests);
            http.createContext("/health", e -> respond(e, ready() ? 200 : 503, "application/json",
                    ready() ? "{\"status\":\"ready\"}" : "{\"status\":\"unavailable\"}"));
            http.createContext("/metrics", e -> respond(e, 200, "text/plain; version=0.0.4", metrics.render()));
            acceptor = threads("mc2-accept").newThread(this::accept);
            http.start();
            ticks.scheduleAtFixedRate(this::advance, 0, 1_000_000_000L / config.tickHz(), TimeUnit.NANOSECONDS);
            acceptor.start();
        } catch (IOException | RuntimeException failure) {
            open.set(false); game.close();
            if (candidate != null) candidate.stop(0);
            ticks.shutdownNow(); readers.shutdownNow(); writers.shutdownNow(); requests.shutdownNow();
            throw failure;
        }
    }
    public int gamePort() { return game.getLocalPort(); }
    public int healthPort() { return http.getAddress().getPort(); }
    public boolean ready() {
        return open.get() && !fatal && lastTick != 0 && System.nanoTime() - lastTick < TimeUnit.SECONDS.toNanos(5);
    }
    private void accept() {
        while (open.get()) {
            try {
                Socket socket = game.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(5_000);
                Connection connection = new Connection(socket);
                if (connections.size() >= 8) { connection.close(); continue; }
                connections.add(connection);
                try {
                    writers.execute(connection::write);
                    readers.execute(connection::read);
                } catch (RejectedExecutionException overloaded) { connection.close(); }
            } catch (IOException failure) {
                if (open.get()) { fatal = true; System.err.println("Game listener failed"); }
                return;
            }
        }
    }
    private void advance() {
        if (!open.get() || fatal) return;
        long start = System.nanoTime();
        boolean success = false;
        try {
            simulation.initialize();
            var departed = players.entrySet().iterator();
            while (departed.hasNext()) {
                var entry = departed.next();
                if (entry.getValue().closed.get()) { simulation.leave(entry.getKey()); departed.remove(); }
            }
            for (Connection connection : connections) {
                if (connection.hello != null && !connection.admitted && !connection.closed.get()) {
                    connection.playerId = simulation.join(connection.hello.name(), connection.hello.skinId());
                    players.put(connection.playerId, connection);
                    connection.admitted = true;
                    connection.send(ProtocolCodec.encode(Map.of("type", "welcome", "playerId", connection.playerId,
                            "tickHz", config.tickHz(), "protocolVersion", 1, "world", simulation.worldInfo())));
                }
            }
            Map<Integer, PlayerFrameInput> inputs = new java.util.LinkedHashMap<>();
            Map<Integer, ProtocolCodec.Input> applied = new java.util.LinkedHashMap<>();
            players.forEach((id, connection) -> {
                var input = connection.inputs.poll();
                if (input != null) {
                    if (input.respawn()) simulation.respawn(id);
                    if (simulation.canApply(id)) { inputs.put(id, input.frame()); applied.put(id, input); }
                }
            });
            simulation.advance(1d / config.tickHz(), inputs);
            applied.forEach((id, input) -> players.get(id).ack = input.seq());
            tick++;
            Map<String,Object> common = simulation.state(tick);
            var changes = simulation.drainBlockChanges();
            players.forEach((id, connection) -> {
                Map<String,Object> state = new java.util.LinkedHashMap<>(common);
                state.put("ack", connection.ack);
                state.put("blocks", changes);
                if (connection.nextChunk < simulation.chunkCount()) {
                    state.put("chunks", java.util.List.of(simulation.chunk(connection.nextChunk++)));
                }
                connection.send(ProtocolCodec.encode(state));
            });
            success = true;
            lastTick = System.nanoTime();
        } catch (RuntimeException failure) {
            fatal = true;
            System.err.println("Simulation failed: " + failure.getClass().getSimpleName());
            connections.forEach(Connection::close);
        } finally {
            metrics.record(System.nanoTime() - start, success, simulation.activeZombies(), config.loadZombies(),
                    (int) players.values().stream().filter(p -> !p.closed.get()).count());
        }
    }
    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/health") && !path.equals("/metrics")) { status = 404; body = "Not found"; }
            else if (!exchange.getRequestMethod().equals("GET")) { status = 405; body = "Method not allowed"; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
    @Override public synchronized void close() {
        if (!open.compareAndSet(true, false)) return;
        http.stop(0);
        try { game.close(); } catch (IOException ignored) { }
        connections.forEach(Connection::close);
        ticks.shutdown();
        try {
            if (!ticks.awaitTermination(5, TimeUnit.SECONDS)) {
                ticks.shutdownNow();
                if (!ticks.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("Tick did not stop; world was not saved concurrently"); return;
                }
            }
            simulation.save();
        } catch (IOException failure) { System.err.println("Could not save server world"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        finally {
            readers.shutdownNow(); writers.shutdownNow(); requests.shutdownNow();
            try { acceptor.join(2_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    private final class Connection {
        final Socket socket;
        final AtomicBoolean closed = new AtomicBoolean();
        final ArrayBlockingQueue<ProtocolCodec.Input> inputs = new ArrayBlockingQueue<>(32);
        final ArrayBlockingQueue<String> output = new ArrayBlockingQueue<>(4);
        volatile ProtocolCodec.Hello hello;
        volatile boolean admitted;
        int playerId; // tick only
        int nextChunk; // tick only
        long ack; // tick only
        Connection(Socket socket) { this.socket = socket; }
        void read() {
            try {
                var in = new BufferedInputStream(socket.getInputStream());
                String line = ProtocolCodec.readLine(in);
                if (!(ProtocolCodec.decode(line) instanceof ProtocolCodec.Hello greeting)) throw new IOException("Expected hello");
                hello = greeting;
                socket.setSoTimeout(30_000);
                long received = 0;
                while (!closed.get() && (line = ProtocolCodec.readLine(in)) != null) {
                    var message = ProtocolCodec.decode(line);
                    if (message instanceof ProtocolCodec.Bye) return;
                    if (!(message instanceof ProtocolCodec.Input input) || !admitted) throw new IOException("Expected input");
                    if (input.seq() <= received) continue;
                    received = input.seq();
                    if (!inputs.offer(input)) throw new IOException("Input queue full");
                }
            } catch (IOException failure) {
                // Parser diagnostics can contain attacker-controlled field names and newlines.
                System.err.println("Peer " + playerId + " read closed: " + failure.getClass().getSimpleName());
                // Invalid, idle, disconnected or overloaded peer: close only this connection.
            } finally { close(); }
        }
        void write() {
            try {
                var out = new BufferedOutputStream(socket.getOutputStream());
                while (!closed.get()) {
                    String message = output.poll(100, TimeUnit.MILLISECONDS);
                    if (message == null) continue;
                    out.write(message.getBytes(StandardCharsets.UTF_8)); out.write('\n'); out.flush();
                }
            } catch (IOException ignored) { }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { close(); }
        }
        void send(String message) {
            if (!closed.get() && !output.offer(message)) {
                System.err.println("Peer " + playerId + " output queue full"); close();
            }
        }
        void close() {
            if (!closed.compareAndSet(false, true)) return;
            try { socket.close(); } catch (IOException ignored) { }
            connections.remove(this);
            inputs.clear(); output.clear();
        }
    }
    private static ThreadPoolExecutor workers(String name, int limit) {
        return new ThreadPoolExecutor(limit, limit, 0, TimeUnit.SECONDS, new SynchronousQueue<>(),
                threads(name), new ThreadPoolExecutor.AbortPolicy());
    }
    private static ThreadFactory threads(String name) {
        return runnable -> { Thread thread = new Thread(runnable, name); thread.setDaemon(true); return thread; };
    }
}
