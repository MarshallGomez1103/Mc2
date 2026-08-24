package bootstrap;

import application.PlayerInteractionService;
import application.WorldApplicationService;
import patterns.factory.BlockFactory;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;
import presentation.MainMenu;
import presentation.game.GameWindow;

import java.nio.file.Path;

/** Punto de composición e inicio de la aplicación de consola. */
public final class Minecraft2Application {
    private Minecraft2Application() {
    }

    public static void main(String[] args) {
        BlockFactory blockFactory = new BlockFactory();
        WorldStorage storage = new JsonWorldStorage(Path.of("worlds"), blockFactory);
        WorldApplicationService worldService = new WorldApplicationService(storage, blockFactory);
        GameWindow gameWindow = new GameWindow(new PlayerInteractionService());
        new MainMenu(worldService, gameWindow).show();
    }
}
