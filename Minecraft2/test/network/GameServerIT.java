package network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GameServerIT {
    @TempDir Path temporary;
    @Test void helloThenInputAppliesExactlyOnceAndAckDoesNotRegress() throws Exception {
        try (GameServer server = server(0); Peer peer = new Peer(server.gamePort())) {
            peer.send("{\"type\":\"hello\",\"name\":\"Jasub\",\"skinId\":2}");
            assertEquals("welcome", peer.next().get("type"));
            Map<?, ?> before = peer.state(0);
            assertEquals(2L, player(before).get("skinId"));
            double yaw = number(player(before).get("yaw"));
            peer.send("{\"type\":\"input\",\"seq\":1,\"deltaYaw\":10}");
            var applied = peer.state(1);
            assertEquals((yaw + 10) % 360, number(player(applied).get("yaw")), 1e-6);
            peer.send("{\"type\":\"input\",\"seq\":1,\"deltaYaw\":100}");
            peer.send("{\"type\":\"input\",\"seq\":2}");
            var after = peer.state(2);
            assertEquals(2L, after.get("ack"));
            assertEquals(number(player(applied).get("yaw")), number(player(after).get("yaw")), 1e-6);
            assertEquals(200, http(server, "/health").statusCode());
            String metrics = http(server, "/metrics").body();
            assertTrue(metrics.contains("mc2_players_connected 1\n"));
            assertTrue(metrics.contains("mc2_tick_duration_seconds_count "));
            assertEquals(404, http(server, "/health/extra").statusCode());
            peer.send("{\"type\":\"bye\"}");
        }
    }
    @Test void invalidPeerDoesNotTakeOverOrStopTheServer() throws Exception {
        try (GameServer server = server(0); Peer owner = new Peer(server.gamePort())) {
            owner.send("{\"type\":\"hello\",\"name\":\"owner\",\"skinId\":0}");
            assertEquals("welcome", owner.next().get("type"));
            try (Peer busy = new Peer(server.gamePort())) {
                busy.send("{\"type\":\"hello\",\"name\":\"busy\",\"skinId\":0}");
                assertEquals("welcome", busy.next().get("type"));
                busy.send("{\"type\":\"bye\"}");
            }
            try (Peer invalid = new Peer(server.gamePort())) {
                invalid.send("{\"type\":\"input\",\"seq\":1}");
                assertNull(invalid.in.readLine());
            }
            owner.send("{\"type\":\"input\",\"seq\":1}");
            assertEquals(1L, owner.state(1).get("ack"));
            assertEquals(200, http(server, "/health").statusCode());
        }
    }
    @Test void realLoadPopulationAndAiRemainObservable() throws Exception {
        try (GameServer server = server(80)) {
            long end = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            String metrics;
            do {
                metrics = http(server, "/metrics").body();
                if (metrics.contains("mc2_zombies_active 80\n")) break;
                Thread.sleep(50);
            } while (System.nanoTime() < end);
            assertTrue(metrics.contains("mc2_zombies_active 80\n"), metrics);
            assertTrue(metrics.contains("mc2_tick_failures_total 0\n"));
            assertTrue(server.ready());
        }
    }
    @Test void closeReleasesPortsAndPreservesNormalWorldForNextStart() throws Exception {
        GameServer first = server(0);
        int game = first.gamePort(), health = first.healthPort();
        first.close(); first.close();
        try (ServerSocket gameProbe = new ServerSocket(game); ServerSocket healthProbe = new ServerSocket(health)) {
            assertTrue(Files.isRegularFile(temporary.resolve("server.json")));
        }
        try (GameServer next = new GameServer(new ServerConfig(game, health, 20, 0, temporary))) {
            assertEquals(game, next.gamePort());
        }
    }
    @Test void packagedJarStartsWithoutGraphicsAndUsesEnvironmentPorts() throws Exception {
        int game, health;
        try (ServerSocket a = new ServerSocket(0); ServerSocket b = new ServerSocket(0)) {
            game = a.getLocalPort(); health = b.getLocalPort();
        }
        Path jar;
        try (var files = Files.list(Path.of("target"))) {
            jar = files.filter(p -> p.getFileName().toString().matches("minecraft2-[0-9].*\\.jar"))
                    .findFirst().orElseThrow().toAbsolutePath();
        }
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaExecutable, "-Djava.awt.headless=true", "-Dmc2.server=true", "-jar", jar.toString());
        builder.environment().putAll(Map.of("MC2_SERVER_PORT", "" + game, "MC2_HEALTH_PORT", "" + health,
                "MC2_TICK_HZ", "20", "MC2_LOAD_ZOMBIES", "0", "MC2_WORLD_DIR", temporary.toString()));
        var log = new java.io.ByteArrayOutputStream();
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Thread capture = new Thread(() -> {
            try (var stdout = process.getInputStream()) { stdout.transferTo(log); }
            catch (java.io.IOException closed) { /* process cleanup closes the pipe */ }
        }, "server-test-output");
        capture.setDaemon(true);
        capture.start();
        try {
            HttpResponse<String> response = null;
            long end = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            do {
                try { response = get("http://127.0.0.1:" + health + "/health"); }
                catch (java.io.IOException notReady) { }
                if (response != null && response.statusCode() == 200) break;
                assertTrue(process.isAlive(), () -> log.toString(StandardCharsets.UTF_8));
                Thread.sleep(50);
            } while (System.nanoTime() < end);
            assertNotNull(response, () -> log.toString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), () -> log.toString(StandardCharsets.UTF_8));
            try (Peer peer = new Peer(game)) {
                peer.send("{\"type\":\"hello\",\"name\":\"process\",\"skinId\":1}");
                assertEquals("welcome", peer.next().get("type"));
                peer.send("{\"type\":\"input\",\"seq\":1}");
                assertEquals(1L, peer.state(1).get("ack"));
            }
            assertFalse(log.toString(StandardCharsets.UTF_8).contains("GLFW"));
        } finally {
            process.destroy();
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                assertTrue(process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS), "Server process did not terminate");
            }
            process.getOutputStream().close();
            capture.join(2_000);
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }
    private GameServer server(int target) throws Exception {
        return new GameServer(new ServerConfig(0, 0, 20, target, temporary));
    }
    private static HttpResponse<String> http(GameServer server, String path) throws Exception {
        return get("http://127.0.0.1:" + server.healthPort() + path);
    }
    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build().send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private static Map<?, ?> player(Map<?, ?> state) { return (Map<?, ?>) ((List<?>)state.get("players")).get(0); }
    private static double number(Object value) { return ((Number)value).doubleValue(); }
    private static final class Peer implements AutoCloseable {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;
        Peer(int port) throws Exception {
            socket = new Socket("127.0.0.1", port); socket.setSoTimeout(5_000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        void send(String line) { out.println(line); }
        Map<?, ?> next() throws Exception {
            String line = in.readLine(); assertNotNull(line, "Unexpected EOF");
            return (Map<?, ?>) JsonParser.parse(line);
        }
        Map<?, ?> state(long ack) throws Exception {
            for (int i = 0; i < 200; i++) {
                var state = next();
                if ("state".equals(state.get("type")) && ((Number)state.get("ack")).longValue() >= ack) return state;
            }
            throw new AssertionError("No applied acknowledgement");
        }
        public void close() throws Exception { socket.close(); }
    }
}
