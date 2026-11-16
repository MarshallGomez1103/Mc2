package network;

import application.PlayerFrameInput;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkClientIT {
    @Test void exchangesActionsWithRealServer(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary) throws Exception {
        try (GameServer server = new GameServer(new ServerConfig(0, 0, 20, 0, temporary));
             NetworkClient client = new NetworkClient("127.0.0.1", server.gamePort(), "Jasub", 2)) {
            await(() -> client.latestState() != null);
            var before = client.latestState().players().get(0);
            var turn = new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(), false, 10, 0, false, false, false, null);
            assertTrue(client.send(turn));
            await(() -> client.latestState().ack() == 1);
            assertEquals((before.yaw() + 10) % 360, client.latestState().players().get(0).yaw(), .001);
            assertEquals(2, client.latestState().players().get(0).skinId());
        }
    }
    @Test void sendsIntentionsReceivesSnapshotsAndClosesTransport() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Long> received = new CompletableFuture<>();
            CompletableFuture<Integer> eof = new CompletableFuture<>();
            Thread peer = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(3000);
                    var in = socket.getInputStream();
                    assertEquals(new ProtocolCodec.Hello("Jasub", 2), ProtocolCodec.decode(ProtocolCodec.readLine(in)));
                    socket.getOutputStream().write("{\"type\":\"welcome\",\"playerId\":4,\"tickHz\":20}\n".getBytes(StandardCharsets.UTF_8));
                    var input = (ProtocolCodec.Input) ProtocolCodec.decode(ProtocolCodec.readLine(in));
                    received.complete(input.seq());
                    socket.getOutputStream().write(("{\"type\":\"state\",\"tick\":1,\"ack\":1,\"players\":[" +
                            "{\"playerId\":4,\"name\":\"Jasub\",\"skinId\":2,\"x\":1,\"y\":2,\"z\":3,\"yaw\":0,\"pitch\":0,\"health\":100,\"dead\":false}],\"zombies\":[]}\n").getBytes(StandardCharsets.UTF_8));
                    eof.complete(in.read());
                } catch (Throwable e) { received.completeExceptionally(e); eof.completeExceptionally(e); }
            });
            peer.setDaemon(true); peer.start();
            try (NetworkClient client = new NetworkClient("127.0.0.1", server.getLocalPort(), "Jasub", 2)) {
                await(() -> client.status() != NetworkClient.Status.CONNECTING);
                assertEquals(NetworkClient.Status.CONNECTED, client.status(), client.error());
                assertTrue(client.send(PlayerFrameInput.NEUTRAL));
                assertEquals(1L, received.get(3, TimeUnit.SECONDS));
                await(() -> client.latestState() != null);
                assertEquals(1, client.latestState().ack());
                assertEquals(4, client.welcome().playerId());
                client.close();
                assertEquals(-1, eof.get(3, TimeUnit.SECONDS));
                assertFalse(client.send(PlayerFrameInput.NEUTRAL));
            }
        }
    }
    @Test void stalledHandshakeDoesNotBlockCallerAndCanBeCancelled() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            NetworkClient client = assertTimeoutPreemptively(Duration.ofSeconds(1),
                    () -> new NetworkClient("127.0.0.1", server.getLocalPort(), "Jasub", 0));
            try (Socket ignored = server.accept()) {
                assertTimeoutPreemptively(Duration.ofMillis(300), client::close);
                assertEquals(NetworkClient.Status.CLOSED, client.status());
            } finally { client.close(); }
        }
    }
    @Test void invalidHandshakeBecomesVisibleFailure() throws Exception {
        try (ServerSocket server = new ServerSocket(0);
             NetworkClient client = new NetworkClient("127.0.0.1", server.getLocalPort(), "Jasub", 0);
             Socket socket = server.accept()) {
            socket.getOutputStream().write("{}\n".getBytes(StandardCharsets.UTF_8));
            await(() -> client.status() == NetworkClient.Status.FAILED);
            assertNotNull(client.error());
        }
    }
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(10);
        assertTrue(condition.getAsBoolean(), "Timed out");
    }
}
