package presentation.game;

import application.PlayerInteractionService;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import domain.world.World;

import java.util.Objects;

/**
 * Abre la ventana de juego para un mundo ya cargado.
 *
 * <p>{@code Lwjgl3Application} bloquea hasta que la ventana se cierra, que es justo el
 * comportamiento que quiere el menú: se juega, se cierra y se vuelve a la consola con el mismo
 * mundo en memoria, listo para guardarse.
 */
public final class GameWindow {
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    private final PlayerInteractionService interactionService;

    public GameWindow(PlayerInteractionService interactionService) {
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
    }

    /** Abre la ventana y no regresa hasta que el jugador la cierra. */
    public void play(World world) {
        Objects.requireNonNull(world, "world no puede ser null");

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Minecraft 2 — " + world.getName());
        configuration.setWindowedMode(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(new VoxelGame(world, interactionService), configuration);
    }
}
