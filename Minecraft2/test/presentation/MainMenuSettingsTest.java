package presentation;

import application.GameSettings;
import application.PlayerInteractionService;
import application.WorldApplicationService;
import domain.enemy.Difficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import patterns.factory.BlockFactory;
import patterns.singleton.WorldManager;
import persistence.JsonWorldStorage;
import presentation.game.GameWindow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El submenú de enemigos cambia la configuración de la sesión sin tocar el resto del menú. */
class MainMenuSettingsTest {
    @TempDir
    Path worldsDirectory;

    private final GameSettings settings = new GameSettings();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @AfterEach
    void descargarMundoActual() {
        WorldManager.getInstance().unload();
    }

    /** Ejecuta el menú con las líneas dadas como entrada y devuelve lo que imprimió. */
    private String run(String... lines) {
        BlockFactory blockFactory = new BlockFactory();
        WorldApplicationService worldService =
                new WorldApplicationService(new JsonWorldStorage(worldsDirectory, blockFactory), blockFactory);
        GameWindow window = new GameWindow(new PlayerInteractionService(), settings);
        ConsoleIO console = new ConsoleIO(
                new ByteArrayInputStream(String.join("\n", lines).concat("\n").getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        new MainMenu(worldService, window, console).show();
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void headerShowsTheCurrentEnemyOptions() {
        String printed = run("0");

        assertTrue(printed.contains("8. Enemigos: ON · Texturas enemigos: ON · Dificultad: NORMAL (configurar)"),
                printed);
    }

    @Test
    void submenuTogglesEachOptionAndReturns() {
        String printed = run("8", "1", "2", "3", "0", "0");

        assertFalse(settings.enemiesEnabled());
        assertFalse(settings.enemyTexturesEnabled());
        assertEquals(Difficulty.VERY_HARD, settings.difficulty());
        assertTrue(printed.contains("8. Enemigos: OFF · Texturas enemigos: OFF · Dificultad: VERY_HARD"),
                "el menú principal refleja los cambios al volver");
        assertTrue(printed.contains("Primera oleada a los 4 s con 4 zombis"), "describe VERY_HARD con sus valores");
    }

    @Test
    void invalidSubmenuOptionChangesNothing() {
        String printed = run("8", "9", "abc", "0", "0");

        assertTrue(settings.enemiesEnabled());
        assertTrue(settings.enemyTexturesEnabled());
        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertTrue(printed.contains("Opción no válida: '9'."));
    }

    @Test
    void endOfInputInsideSubmenuDoesNotHang() {
        run("8", "1");

        assertFalse(settings.enemiesEnabled());
    }

    @Test
    void crudStillWorksNextToTheNewOption() {
        String printed = run("1", "prueba", "1", "8", "3", "0", "2", "0");

        assertTrue(printed.contains("Mundo creado y cargado: prueba"), printed);
        assertTrue(printed.contains("  - prueba"), "el listado sigue funcionando");
        assertEquals(Difficulty.VERY_HARD, settings.difficulty());
    }
}
