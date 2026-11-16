package presentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Caja negra del JAR: consola y servidor headless con TCP/HTTP reales, sin ventana ni mocks. */
class GameFlowSmokeIT {
    @Test
    void createsListsSavesThenReloadsAndDeletesInANewProcess(@TempDir Path dir) throws Exception {
        // Arrange / Act: crear y cerrar usando solo la interfaz pública.
        String first = console(dir, "1", "smokeWorld", "1", "2", "4", "0");
        // Assert: el listado y el archivo confirman el guardado.
        assertTrue(first.contains("Mundo creado y cargado: smokeWorld"), first);
        assertTrue(first.contains("  - smokeWorld"), first);
        assertTrue(first.contains("Mundo actual guardado: smokeWorld"), first);
        assertTrue(Files.isRegularFile(dir.resolve("smokeWorld.json")));
        // Act: otro proceso debe leer el archivo, sin compartir el Singleton.
        String second = console(dir, "3", "smokeWorld", "2", "5", "smokeWorld", "s", "2", "0");
        // Assert
        assertTrue(second.contains("Mundo cargado: smokeWorld"), second);
        assertTrue(second.contains("  - smokeWorld"), second);
        assertTrue(second.contains("Mundo eliminado: smokeWorld"), second);
        assertTrue(second.contains("No hay mundos guardados."), second);
        assertFalse(Files.exists(dir.resolve("smokeWorld.json")));
    }

    @Test
    void cancellingDeletionKeepsTheWorldAvailableAfterRestart(@TempDir Path dir) throws Exception {
        // Arrange
        console(dir, "1", "conservado", "1", "0");
        byte[] saved = Files.readAllBytes(dir.resolve("conservado.json"));
        // Act
        String output = console(dir, "5", "conservado", "n", "3", "conservado", "2", "0");
        // Assert
        assertTrue(output.contains("Operación cancelada. No se eliminó ningún mundo."), output);
        assertTrue(output.contains("Mundo cargado: conservado"), output);
        assertTrue(output.contains("  - conservado"), output);
        assertArrayEquals(saved, Files.readAllBytes(dir.resolve("conservado.json")));
    }

    @Test
    void invalidJsonIsReportedWithoutReplacingTheCurrentWorld(@TempDir Path dir) throws Exception {
        // Arrange: el archivo inválido es una entrada externa, no un servicio simulado.
        console(dir, "1", "valido", "1", "0");
        Path invalid = dir.resolve("roto.json");
        Files.writeString(invalid, "{\"name\":\"incompleto\"}", StandardCharsets.UTF_8);
        // Act
        String output = console(dir, "3", "valido", "2", "3", "roto", "4", "0");
        // Assert: el mundo válido sigue activo y el archivo inválido no se destruye.
        assertTrue(output.contains("  - roto"), output);
        assertFalse(output.contains("Mundo cargado: roto"), output);
        assertTrue(output.contains("Problema con el archivo del mundo:"), output);
        assertTrue(output.contains("Mundo actual guardado: valido"), output);
        assertEquals("{\"name\":\"incompleto\"}", Files.readString(invalid));
    }

    @Test
    @org.junit.jupiter.api.Timeout(90)
    void packagedServerSharesOneWorldConfirmsActionsAndRemovesDisconnectedPlayers(@TempDir Path dir) throws Exception {
        // First build a saved world through the same public console used by a player.
        console(dir, "1", "server", "1", "0");
        Path jar = Path.of("target", "minecraft2-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "Failsafe must execute the JAR packaged by this build");
        try (var archive = new java.util.jar.JarFile(jar.toFile())) {
            assertEquals("bootstrap.Minecraft2Application", archive.getManifest().getMainAttributes().getValue("Main-Class"));
            assertNotNull(archive.getEntry("network/GameServer.class"));
        }
        String fingerprint = jarSha256(jar);
        try (PackagedServer server = PackagedServer.start(jar, dir)) {
            assertEquals(200, server.get("/health").statusCode());
            try (ServerPeer jasub = new ServerPeer(server.gamePort);
                 ServerPeer teammate = new ServerPeer(server.gamePort)) {
                long jasubId = jasub.hello("JasubD", 0);
                jasub.state(s -> players(s).size() == 1);
                long teammateId = teammate.hello("EquipoD", 2);
                assertNotEquals(jasubId, teammateId);
                var before = jasub.state(s -> players(s).size() == 2);
                teammate.state(s -> players(s).size() == 2);
                double ownYaw = number(player(before, jasubId), "yaw");
                double otherYaw = number(player(before, teammateId), "yaw");
                assertEquals(0, number(player(before, jasubId), "skinId"));
                assertEquals(2, number(player(before, teammateId), "skinId"));
                server.awaitPlayers(2);

                jasub.send("{\"type\":\"input\",\"seq\":1,\"deltaYaw\":15}");
                var applied = jasub.state(s -> number(s, "ack") == 1);
                assertEquals((ownYaw + 15) % 360, number(player(applied, jasubId), "yaw"), 1e-6);
                var observed = teammate.state(s -> Math.abs(number(player(s, jasubId), "yaw") - (ownYaw + 15) % 360) < 1e-6);
                assertEquals(0, number(observed, "ack"), "Another player's input must not acknowledge my connection");
                assertEquals(otherYaw, number(player(observed, teammateId), "yaw"), 1e-6);

                jasub.send("{\"type\":\"bye\"}");
                server.awaitPlayers(1);
                teammate.state(s -> players(s).size() == 1 && player(s, teammateId) != null);
                teammate.send("{\"type\":\"bye\"}");
                server.awaitPlayers(0);
                try (ServerPeer rejoined = new ServerPeer(server.gamePort)) {
                    long newId = rejoined.hello("ReingresoD", 1);
                    assertEquals(teammateId, newId, "The last vacant body is reclaimed without a ghost");
                    var state = rejoined.state(s -> players(s).size() == 1);
                    assertEquals(0, number(state, "ack"));
                    assertEquals("ReingresoD", player(state, newId).get("name"));
                    rejoined.send("{\"type\":\"bye\"}");
                    server.awaitPlayers(0);
                }
            }
            assertEquals(200, server.get("/health").statusCode());
            assertTrue(server.get("/metrics").body().contains("mc2_tick_failures_total 0\n"));
            System.out.println("D_JAR_SMOKE sha256=" + fingerprint + " flow=console-world,health,two-clients,input,ack,bye,rejoin players=0");
        }
        assertTrue(Files.isRegularFile(dir.resolve("server.json")));
        assertInstanceOf(java.util.Map.class, persistence.JsonParser.parse(Files.readString(dir.resolve("server.json"))));
    }

    private static String jarSha256(Path jar) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        try (var input = new java.security.DigestInputStream(Files.newInputStream(jar), digest)) {
            input.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    private static double number(java.util.Map<?,?> object, String key) { return ((Number)object.get(key)).doubleValue(); }
    private static java.util.List<?> players(java.util.Map<?,?> state) { return (java.util.List<?>)state.get("players"); }
    private static java.util.Map<?,?> player(java.util.Map<?,?> state, long id) {
        return players(state).stream().map(p -> (java.util.Map<?,?>)p)
                .filter(p -> ((Number)p.get("playerId")).longValue() == id).findFirst()
                .orElseThrow(() -> new AssertionError("Missing player " + id));
    }

    /** Uses raw public JSON/TCP; never constructs GameServer/GameSession in the test JVM. */
    private static final class ServerPeer implements AutoCloseable {
        final java.net.Socket socket;
        final java.io.BufferedReader input;
        final java.io.PrintWriter output;
        ServerPeer(int port) throws IOException {
            socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 2_000);
            socket.setSoTimeout(3_000);
            input = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        void send(String json) { output.println(json); assertFalse(output.checkError(), "Could not send public message"); }
        java.util.Map<?,?> next() throws IOException {
            String line = input.readLine();
            assertNotNull(line, "Packaged server disconnected unexpectedly");
            return assertInstanceOf(java.util.Map.class, persistence.JsonParser.parse(line));
        }
        long hello(String name, int skin) throws IOException {
            send("{\"type\":\"hello\",\"name\":\"" + name + "\",\"skinId\":" + skin + "}");
            var welcome = next(); assertEquals("welcome", welcome.get("type"));
            return ((Number)welcome.get("playerId")).longValue();
        }
        java.util.Map<?,?> state(java.util.function.Predicate<java.util.Map<?,?>> expected) throws IOException {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                var state = next(); assertEquals("state", state.get("type"));
                if (expected.test(state)) return state;
            } while (System.nanoTime() < end);
            throw new AssertionError("Public server state did not reach the expected condition");
        }
        @Override public void close() throws IOException { socket.close(); }
    }

    /** Ports come from the child banner after binding port 0, avoiding fixed/free-port races. */
    private static final class PackagedServer implements AutoCloseable {
        final Process process;
        final java.io.ByteArrayOutputStream log = new java.io.ByteArrayOutputStream();
        final Thread capture;
        final Path logFile;
        final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(1)).build();
        int gamePort, healthPort;
        PackagedServer(Path jar, Path directory) throws IOException {
            Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", java.io.File.separatorChar == '\\' ? "java.exe" : "java");
            ProcessBuilder builder = new ProcessBuilder(javaExecutable.toString(), "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true",
                    "-Dmc2.server=true", "-jar", jar.toString()).directory(directory.toFile()).redirectErrorStream(true);
            builder.environment().putAll(java.util.Map.of("MC2_SERVER_PORT", "0", "MC2_HEALTH_PORT", "0",
                    "MC2_TICK_HZ", "20", "MC2_LOAD_ZOMBIES", "0", "MC2_WORLD_DIR", directory.toAbsolutePath().toString()));
            logFile = directory.resolve("packaged-server.log");
            process = builder.start(); process.getOutputStream().close();
            capture = new Thread(() -> {
                try (var stdout = process.getInputStream()) { stdout.transferTo(log); }
                catch (IOException closed) { /* process cleanup closes the pipe */ }
            }, "phase-d-jar-output");
            capture.setDaemon(true); capture.start();
        }
        static PackagedServer start(Path jar, Path directory) throws Exception {
            PackagedServer server = new PackagedServer(jar, directory);
            try {
                var pattern = java.util.regex.Pattern.compile("MC2 server: game=(\\d+) health=(\\d+)");
                long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (System.nanoTime() < end) {
                    assertTrue(server.process.isAlive(), () -> server.log.toString(StandardCharsets.UTF_8));
                    var match = pattern.matcher(server.log.toString(StandardCharsets.UTF_8));
                    if (match.find()) {
                        server.gamePort = Integer.parseInt(match.group(1)); server.healthPort = Integer.parseInt(match.group(2));
                        try { if (server.get("/health").statusCode() == 200) return server; }
                        catch (IOException notReady) { /* wait for readiness, not a fixed warmup */ }
                    }
                    Thread.sleep(25);
                }
                throw new AssertionError("Packaged server did not become ready: " + server.log.toString(StandardCharsets.UTF_8));
            } catch (Exception | AssertionError failure) { server.close(); throw failure; }
        }
        java.net.http.HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + healthPort + path))
                    .timeout(java.time.Duration.ofSeconds(1)).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        }
        void awaitPlayers(int expected) throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < end) {
                if (get("/metrics").body().contains("mc2_players_connected " + expected + "\n")) return;
                Thread.sleep(25);
            }
            throw new AssertionError("Connected-player metric did not reach " + expected);
        }
        @Override public void close() throws IOException, InterruptedException {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly(); assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Server child did not stop");
            }
            process.getInputStream().close(); process.getErrorStream().close();
            capture.join(2_000);
            assertFalse(capture.isAlive(), "Output capture must stop before temporary-file cleanup");
            Files.writeString(logFile, log.toString(StandardCharsets.UTF_8));
        }
    }

    private static String console(Path dir, String... options) throws IOException, InterruptedException {
        Path jar = Path.of("target", "minecraft2-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "mvn verify debe empaquetar el JAR antes de Failsafe");
        Path outputFile = Files.createTempFile(dir, "console-", ".txt");
        String executable = java.io.File.separatorChar == '\\' ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Process process = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8",
                "-Dmc2.console=true", "-Dmc2.gpu=AUTO", "-Dmc2.worlds.dir=" + dir.toAbsolutePath(),
                "-jar", jar.toString()).directory(dir.toFile()).redirectErrorStream(true)
                .redirectOutput(outputFile.toFile()).start();
        try {
            try (var stdin = process.getOutputStream()) {
                stdin.write((String.join("\n", options) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            // La salida va a archivo: leer stdout no puede bloquear el timeout.
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                fail("El proceso de consola excedió 30 s:\n" + Files.readString(outputFile));
            }
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("Hasta luego."), output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
