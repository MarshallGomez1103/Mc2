package bootstrap;

import application.WorldApplicationService;
import persistence.JsonWorldStorage;
import persistence.WorldStorage;
import presentation.MainMenu;

import java.nio.file.Path;

/** Punto de composición e inicio de la aplicación de consola. */
public final class Minecraft2Application {
    private Minecraft2Application() {
    }

    public static void main(String[] args) {
        WorldStorage storage = new JsonWorldStorage(Path.of("worlds"));
        WorldApplicationService worldService = new WorldApplicationService(storage);
        new MainMenu(worldService).show();
    }
}
