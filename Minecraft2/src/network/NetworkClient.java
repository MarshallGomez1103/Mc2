package network;

import application.PlayerFrameInput;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** One connection per instance. DNS, connect, reads and writes never run on the graphics thread. */
public final class NetworkClient implements AutoCloseable {
    public enum Status { CONNECTING, CONNECTED, CLOSED, FAILED }
    private final ArrayBlockingQueue<String> outbound = new ArrayBlockingQueue<>(128);
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Socket socket = new Socket();
    private volatile Status status = Status.CONNECTING;
    private volatile String error;
    private volatile ClientProtocol.Welcome welcome;
    private volatile ClientProtocol.State latest;
    private long sequence;

    public NetworkClient(String host, int port, String name, int skinId) throws IOException {
        if (host == null || host.isBlank() || port < 1 || port > 65535) throw new IllegalArgumentException("Invalid server address");
        String hello = ClientProtocol.hello(name, skinId);
        Thread reader = new Thread(() -> receive(host, port, hello), "mc2-client-reader");
        reader.setDaemon(true); reader.start();
    }
    private void receive(String host, int port, String hello) {
        try {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setTcpNoDelay(true); socket.setSoTimeout(5000);
            var output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            write(output, hello);
            var input = new BufferedInputStream(socket.getInputStream());
            String line = ClientProtocol.readLine(input);
            if (!(ClientProtocol.decode(line) instanceof ClientProtocol.Welcome admitted)) throw new IOException("Expected welcome");
            welcome = admitted;
            synchronized (this) {
                if (stopped.get()) return;
                status = Status.CONNECTED;
            }
            Thread writer = new Thread(() -> transmit(output), "mc2-client-writer");
            writer.setDaemon(true); writer.start();
            long tick = -1, ack = 0;
            while (!stopped.get()) {
                line = ClientProtocol.readLine(input);
                if (line == null) throw new EOFException("Server disconnected");
                if (!(ClientProtocol.decode(line) instanceof ClientProtocol.State snapshot)) throw new IOException("Expected state");
                if (snapshot.tick() <= tick || snapshot.ack() < ack || snapshot.ack() > sentSequence()) throw new IOException("Invalid tick or acknowledgement");
                if (snapshot.players().stream().noneMatch(p -> p.playerId() == admitted.playerId())) throw new IOException("Missing local player");
                tick = snapshot.tick(); ack = snapshot.ack(); latest = snapshot;
            }
        } catch (IOException | RuntimeException e) { fail(e); }
        finally { stopSocket(); }
    }
    private void transmit(BufferedWriter output) {
        try {
            while (!stopped.get()) {
                String message = outbound.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (message != null) write(output, message);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            fail(e);
        }
    }
    private static void write(BufferedWriter output, String message) throws IOException {
        output.write(message); output.write('\n'); output.flush();
    }
    /** Nonblocking; overflow disconnects explicitly rather than silently losing discrete actions. */
    public synchronized boolean send(PlayerFrameInput input) {
        Objects.requireNonNull(input);
        if (status != Status.CONNECTED || stopped.get()) return false;
        try {
            String message = ClientProtocol.input(sequence + 1, input);
            if (!outbound.offer(message)) { fail(new IOException("Input queue full")); return false; }
            sequence++; return true;
        } catch (IOException e) { fail(e); return false; }
    }
    private synchronized long sentSequence() { return sequence; }
    public Status status() { return status; }
    public String error() { return error; }
    public ClientProtocol.Welcome welcome() { return welcome; }
    public ClientProtocol.State latestState() { return latest; }
    private synchronized void fail(Exception failure) {
        if (stopped.compareAndSet(false, true)) {
            error = failure.getMessage(); status = Status.FAILED; stopSocket();
        }
    }
    private void stopSocket() { try { socket.close(); } catch (IOException ignored) { } }
    /** Closing the transport also covers window disposal and connection loss on the server. */
    @Override public synchronized void close() {
        stopped.set(true); status = Status.CLOSED; outbound.clear(); stopSocket();
    }
}
