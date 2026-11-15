package loadtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** El script de carga de punta a punta contra un doble del servidor que habla el protocolo v1. */
class NetworkLoadClientIT {
    @TempDir Path out;

    private Map<String, String> environment(int gamePort, int healthPort) {
        Map<String, String> env = new HashMap<>();
        env.put("MC2_SERVER_HOST", "127.0.0.1");
        env.put("MC2_SERVER_PORT", String.valueOf(gamePort));
        env.put("MC2_HEALTH_PORT", String.valueOf(healthPort));
        env.put("MC2_LOAD_CLIENTS", "2");
        env.put("MC2_LOAD_REPETITIONS", "2");
        env.put("MC2_LOAD_ZOMBIES", "80");
        env.put("MC2_LOAD_SECONDS", "2");
        env.put("MC2_LOAD_WARMUP_SECONDS", "2");
        env.put("MC2_LOAD_OUT", out.toString());
        return env;
    }

    @Test void compliantServerPassesAndWritesOneRowPerRepetition() throws Exception {
        // Arrange: 80 zombis y ticks de 1,5 ms.
        try (var server = new FakeGameServer(80, 0.0015)) {
            var env = environment(server.gamePort(), server.healthPort());
            // Esta prueba verifica la medición y el veredicto, no el rendimiento de la máquina que la ejecuta.
            env.put("MC2_SLO_RTT_P95_MS", "500");
            var settings = NetworkLoadClient.Settings.fromEnvironment(env);
            // Act
            int exit = NetworkLoadClient.run(settings);
            // Assert
            List<String> csv = Files.readAllLines(out.resolve("results.csv"));
            assertEquals(0, exit, String.join("\n", Files.readAllLines(out.resolve("verdict.txt"))));
            assertEquals(3, csv.size(), "cabecera y dos repeticiones");
            assertTrue(Files.readString(out.resolve("verdict.txt")).contains("verdict=CUMPLE"));
        }
    }

    @Test void aThresholdBrokenOnPurposeFails() throws Exception {
        try (var server = new FakeGameServer(80, 0.0015)) {
            var env = environment(server.gamePort(), server.healthPort());
            env.put("MC2_SLO_TICK_P95_MS", "0.5");
            assertEquals(LoadSloGate.EXIT_VIOLATION, NetworkLoadClient.run(NetworkLoadClient.Settings.fromEnvironment(env)));
            assertTrue(Files.readString(out.resolve("verdict.txt")).contains("SLO-CARGA-01"));
        }
    }

    @Test void populationBelowTargetFails() throws Exception {
        try (var server = new FakeGameServer(40, 0.0015)) {
            var env = environment(server.gamePort(), server.healthPort());
            env.put("MC2_LOAD_WARMUP_SECONDS", "0");
            assertEquals(LoadSloGate.EXIT_VIOLATION, NetworkLoadClient.run(NetworkLoadClient.Settings.fromEnvironment(env)));
            assertTrue(Files.readString(out.resolve("verdict.txt")).contains("población"));
        }
    }

    @Test void unreachableServerFailsInsteadOfPassingWithoutData() throws Exception {
        // Arrange: un puerto que se reserva y se libera para que nadie escuche en él.
        int closedPort;
        try (var probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        }
        var env = environment(closedPort, closedPort);
        env.put("MC2_LOAD_REPETITIONS", "1");
        env.put("MC2_LOAD_SECONDS", "1");
        env.put("MC2_LOAD_WARMUP_SECONDS", "0");
        // Act
        int exit = NetworkLoadClient.run(NetworkLoadClient.Settings.fromEnvironment(env));
        // Assert
        assertEquals(LoadSloGate.EXIT_VIOLATION, exit);
        assertTrue(Files.readString(out.resolve("verdict.txt")).contains("falta la medición"));
    }
}
