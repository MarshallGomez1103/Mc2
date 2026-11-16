package bootstrap;

import network.GameServer;
import network.ServerConfig;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/** Selected before any GPU, window or graphical client is initialized. */
public final class ServerBootstrap {
    private ServerBootstrap() { }
    public static void run() {
        ServerConfig config = ServerConfig.fromEnvironment(System.getenv(), WorldDirectory.resolve().toString());
        try (GameServer server = new GameServer(config)) {
            Thread hook = new Thread(server::close, "mc2-shutdown");
            Runtime.getRuntime().addShutdownHook(hook);
            System.out.println("MC2 server: game=" + server.gamePort() + " health=" + server.healthPort()
                    + " tickHz=" + config.tickHz() + " capacity=1");
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(hook); }
                catch (IllegalStateException shuttingDown) { /* shutdown hook already owns cleanup */ }
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not start MC2 server", failure);
        }
    }
}
