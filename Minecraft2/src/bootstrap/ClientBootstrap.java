package bootstrap;

import application.SkinSelection;
import com.badlogic.gdx.backends.lwjgl3.*;
import network.NetworkClient;
import presentation.game.VoxelGame;
import java.io.IOException;
import java.util.Map;

/** Remote mode selected explicitly or by the front container's server destination. */
public final class ClientBootstrap {
    private ClientBootstrap() { }
    public static boolean requested(Map<String,String> environment) {
        return Boolean.getBoolean("mc2.client") || !environment.getOrDefault("MC2_SERVER_HOST", "").isBlank();
    }
    public static void run() {
        Map<String,String> env=System.getenv();
        String host=env.getOrDefault("MC2_SERVER_HOST","127.0.0.1");
        int port=Integer.parseInt(env.getOrDefault("MC2_SERVER_PORT","7777"));
        try (NetworkClient client=new NetworkClient(host,port,env.getOrDefault("MC2_PLAYER_NAME","Jugador"),SkinSelection.fromEnvironment(env))) {
            Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
            config.setTitle("Minecraft2 - "+host+":"+port);config.setWindowedMode(1280,720);config.useVsync(true);config.setForegroundFPS(60);
            new Lwjgl3Application(new VoxelGame(client),config);
        } catch(IOException e) { throw new IllegalStateException("Could not start remote client",e); }
    }
}
