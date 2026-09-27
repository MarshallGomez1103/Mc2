package bootstrap;

import application.GameSettings;
import application.PlayerInteractionService;
import application.WorldApplicationService;
import patterns.factory.BlockFactory;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;
import presentation.MainMenu;
import presentation.game.GameWindow;
import java.nio.file.Path;


/** Punto de composición: UI gráfica normal, consola opcional de compatibilidad. */
public final class Minecraft2Application {
    private Minecraft2Application() {
    }

    public static void main(String[] args) {
        BlockFactory blockFactory = new BlockFactory();
        Path directory = WorldDirectory.resolve();
        GpuPreference.prepare(directory, args);
        WorldStorage storage = new JsonWorldStorage(directory, blockFactory);
        WorldApplicationService worldService = new WorldApplicationService(storage, blockFactory);
        GameWindow gameWindow = new GameWindow(new PlayerInteractionService(), new GameSettings());
        if (Boolean.getBoolean("mc2.console")) new MainMenu(worldService, gameWindow).show();
        else gameWindow.openMenu(worldService, directory);
    }
}
