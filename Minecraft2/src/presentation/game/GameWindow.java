package presentation.game;

import application.GameSettings;
import application.WorldApplicationService;
import java.nio.file.Path;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
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
    private final GameSettings settings;
    private boolean texturesEnabled = true;

    public GameWindow(PlayerInteractionService interactionService) {
        this(interactionService, new GameSettings());
    }

    public GameWindow(PlayerInteractionService interactionService, GameSettings settings) {
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
        this.settings = Objects.requireNonNull(settings, "settings no puede ser null");
    }

    /** Opciones de enemigos de la sesión, compartidas con el menú. */
    public GameSettings settings() {
        return settings;
    }

    /** Alterna entre el atlas visual y los colores planos originales. */
    public boolean toggleTextures() {
        texturesEnabled = !texturesEnabled;
        return texturesEnabled;
    }

    /** Estado que se aplicará al abrir la siguiente ventana de juego. */
    public boolean texturesEnabled() {
        return texturesEnabled;
    }

    /** Flujo normal integrado: una sola ventana conserva menú y partida. */
    public void openMenu(WorldApplicationService worlds, Path directory) {
        GraphicalGame shell = new GraphicalGame(worlds, this, interactionService, directory);
        Lwjgl3ApplicationConfiguration config = configuration("Minecraft 2");
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public boolean closeRequested() {
                shell.requestClose();
                return false;
            }
            @Override public void focusLost() { shell.focusLost(); }
        });
        new Lwjgl3Application(shell, config);
    }

    private static Lwjgl3ApplicationConfiguration configuration(String title) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(title);
        config.setWindowedMode(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        config.useVsync(true);
        config.setForegroundFPS(60);
        return config;
    }

    /** Abre la ventana y no regresa hasta que el jugador la cierra. */
    public void play(World world) {
        Objects.requireNonNull(world, "world no puede ser null");

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Minecraft 2 — " + world.getName());
        configuration.setWindowedMode(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(
                new VoxelGame(world, interactionService, texturesEnabled, settings), configuration);
    }
}
