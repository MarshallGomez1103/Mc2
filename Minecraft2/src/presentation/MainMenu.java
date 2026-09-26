package presentation;

import application.GameSettings;
import application.WorldApplicationService;
import application.WorldSize;
import domain.enemy.Difficulty;
import domain.world.World;
import presentation.game.GameWindow;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Menú principal de consola. Es el enrutador de las pantallas: muestra opciones, captura datos
 * y presenta resultados. Toda la lógica vive en {@link WorldApplicationService}.
 *
 * <p>Cada pantalla es un método privado y el reparto se hace con un {@code switch}. No hay una
 * interfaz {@code Screen} con despacho polimórfico a propósito: eso se leería como Command o
 * Strategy, y {@code TODO.md} solo admite Factory, Singleton y Observer.
 */
public final class MainMenu {
    private final WorldApplicationService worldService;
    private final ConsoleIO console;
    private final GameWindow gameWindow;

    public MainMenu(WorldApplicationService worldService, GameWindow gameWindow) {
        this(worldService, gameWindow, new ConsoleIO());
    }

    public MainMenu(WorldApplicationService worldService, GameWindow gameWindow, ConsoleIO console) {
        this.worldService = Objects.requireNonNull(worldService, "worldService no puede ser null");
        this.gameWindow = Objects.requireNonNull(gameWindow, "gameWindow no puede ser null");
        this.console = Objects.requireNonNull(console, "console no puede ser null");
    }

    public void show() {
        boolean running = true;
        while (running) {
            printHeader();
            Optional<String> option = console.prompt("Seleccione una opción");
            if (option.isEmpty()) {
                console.blank();
                console.info("Entrada finalizada. Hasta luego.");
                return;
            }
            running = handle(option.get());
        }
    }

    private boolean handle(String option) {
        return switch (option) {
            case "1" -> {
                createScreen();
                yield true;
            }
            case "2" -> {
                listScreen();
                yield true;
            }
            case "3" -> {
                loadScreen();
                yield true;
            }
            case "4" -> {
                saveScreen();
                yield true;
            }
            case "5" -> {
                deleteScreen();
                yield true;
            }
            case "6" -> {
                playScreen();
                yield true;
            }
            case "7" -> {
                toggleTextures();
                yield true;
            }
            case "8" -> {
                enemySettingsScreen();
                yield true;
            }
            case "0" -> {
                console.info("Hasta luego.");
                yield false;
            }
            default -> {
                console.error("Opción no válida: '" + option + "'.");
                yield true;
            }
        };
    }

    // ------------------------------------------------------------------ pantallas

    private void createScreen() {
        console.title("Crear mundo");
        Optional<String> name = console.promptRequired("Nombre del mundo");
        if (name.isEmpty()) {
            return;
        }
        console.info("Tamaño: 1. Pequeño (2x2) · 2. Mediano (10x10) · 3. Grande (16x16)");
        Optional<String> selectedSize = console.prompt("Elija tamaño [1 por defecto]");
        if (selectedSize.isEmpty()) {
            return;
        }
        WorldSize size = switch (selectedSize.get()) {
            case "", "1" -> WorldSize.SMALL;
            case "2" -> WorldSize.MEDIUM;
            case "3" -> WorldSize.LARGE;
            default -> null;
        };
        if (size == null) {
            console.error("Tamaño no válido. No se creó el mundo.");
            return;
        }
        try {
            worldService.createWorld(name.get(), size);
            console.info("Mundo creado y cargado: " + name.get() + " (" + size.label()
                    + ", " + size.totalChunks() + " chunks)");
        } catch (IOException failure) {
            reportFileProblem(failure);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            reportRejected(failure);
        }
    }

    private void listScreen() {
        console.title("Mundos guardados");
        try {
            List<String> worlds = worldService.listWorlds();
            if (worlds.isEmpty()) {
                console.info("No hay mundos guardados.");
                return;
            }
            worlds.forEach(world -> console.info("  - " + world));
            console.info("Total: " + worlds.size());
        } catch (IOException failure) {
            reportFileProblem(failure);
        }
    }

    private void loadScreen() {
        console.title("Cargar mundo");
        Optional<String> name = console.promptRequired("Nombre del mundo");
        if (name.isEmpty()) {
            return;
        }
        try {
            worldService.loadWorld(name.get());
            console.info("Mundo cargado: " + name.get());
        } catch (IOException failure) {
            reportFileProblem(failure);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            reportRejected(failure);
        }
    }

    private void saveScreen() {
        console.title("Guardar mundo actual");
        try {
            worldService.saveCurrentWorld();
            console.info("Mundo actual guardado: "
                    + worldService.currentWorldName().orElse("(desconocido)"));
        } catch (IOException failure) {
            reportFileProblem(failure);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            reportRejected(failure);
        }
    }

    private void deleteScreen() {
        console.title("Eliminar mundo");
        Optional<String> name = console.promptRequired("Nombre del mundo");
        if (name.isEmpty()) {
            return;
        }
        if (!console.confirm("Se eliminará \"" + name.get() + "\" de forma permanente. ¿Continuar?")) {
            console.info("Operación cancelada. No se eliminó ningún mundo.");
            return;
        }
        try {
            worldService.deleteWorld(name.get());
            console.info("Mundo eliminado: " + name.get());
        } catch (IOException failure) {
            reportFileProblem(failure);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            reportRejected(failure);
        }
    }

    private void playScreen() {
        console.title("Jugar");
        Optional<World> world = worldService.currentWorld();
        if (world.isEmpty()) {
            console.error("No hay ningún mundo cargado. Cree uno o cargue uno antes de jugar.");
            return;
        }

        console.info("Abriendo el mundo \"" + world.get().getName() + "\"...");
        console.info("WASD para moverte, Shift para correr, espacio para saltar, clic para colocar y eliminar bloques.");
        console.info("J/K cambian distancia visible; R reaparece después de morir; FPS en pantalla.");
        console.info(enemySummary(gameWindow.settings()) + ". Clic izquierdo golpea al zombi que miras.");
        console.info("Pulsa ESC o cierra la ventana para volver a este menú.");

        try {
            gameWindow.play(world.get());
        } catch (RuntimeException failure) {
            // Un fallo gráfico no debe cerrar el programa: el mundo sigue en memoria y se puede guardar.
            console.blank();
            console.error("El juego se cerró por un error: " + failure.getMessage());
        }

        console.blank();
        console.info("De vuelta en el menú. Usa \"4. Guardar mundo actual\" para conservar los cambios.");
    }

    private void toggleTextures() {
        boolean enabled = gameWindow.toggleTextures();
        console.info("Texturas " + (enabled ? "activadas" : "desactivadas")
                + ". Se aplicarán al abrir el juego.");
    }

    /** Opciones de enemigos de la sesión; se aplican al abrir la siguiente ventana de juego. */
    private void enemySettingsScreen() {
        GameSettings settings = gameWindow.settings();
        while (true) {
            console.title("Opciones de enemigos");
            console.info("""
                    1. Enemigos: %s (cambiar)
                    2. Texturas de enemigos: %s (cambiar)
                    3. Dificultad: %s (cambiar)
                    0. Volver""".formatted(onOff(settings.enemiesEnabled()),
                    onOff(settings.enemyTexturesEnabled()), settings.difficulty()));
            Optional<String> option = console.prompt("Seleccione una opción");
            if (option.isEmpty() || option.get().equals("0")) {
                return;
            }
            switch (option.get()) {
                case "1" -> console.info("Enemigos " + onOff(settings.toggleEnemies())
                        + ". Se aplicará al abrir el juego.");
                case "2" -> console.info("Texturas de enemigos " + onOff(settings.toggleEnemyTextures())
                        + ". Se aplicará al abrir el juego.");
                case "3" -> console.info("Dificultad " + settings.cycleDifficulty()
                        + ". " + describe(settings.difficulty()));
                default -> console.error("Opción no válida: '" + option.get() + "'.");
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static String enemySummary(GameSettings settings) {
        return "Enemigos: " + onOff(settings.enemiesEnabled())
                + " · Texturas enemigos: " + onOff(settings.enemyTexturesEnabled())
                + " · Dificultad: " + settings.difficulty();
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    /** Resume la dificultad con sus propios parámetros, sin condicionales por nivel. */
    private static String describe(Difficulty difficulty) {
        return String.format(Locale.ROOT,
                "Primera oleada a los %.0f s con %d zombis (+%d por oleada, máx. %d); velocidad %.1f, %d golpes.",
                difficulty.waveRules().firstWaveDelaySeconds(), difficulty.waveRules().baseCount(),
                difficulty.waveRules().extraPerWave(), difficulty.waveRules().maxCount(),
                difficulty.zombieParameters().moveSpeed(), difficulty.zombieParameters().maxHealth());
    }

    private void printHeader() {
        console.title("Minecraft 2");
        console.info(worldService.currentWorldName()
                .map(name -> "Mundo cargado: " + name)
                .orElse("No hay ningún mundo cargado."));
        console.blank();
        console.info("""
                1. Crear mundo
                2. Listar mundos
                3. Cargar mundo
                4. Guardar mundo actual
                5. Eliminar mundo
                6. Jugar
                7. Texturas: %s (cambiar)
                8. %s (configurar)
                0. Salir""".formatted(gameWindow.texturesEnabled() ? "ACTIVADAS" : "DESACTIVADAS",
                enemySummary(gameWindow.settings())));
    }

    /** Fallo al leer o escribir el archivo: dañado, ilegible o con un esquema que no cumple. */
    private void reportFileProblem(IOException failure) {
        console.error("Problema con el archivo del mundo: " + failure.getMessage());
    }

    /** La operación se rechazó por los datos recibidos o por el estado actual. */
    private void reportRejected(RuntimeException failure) {
        console.error("No fue posible completar la operación: " + failure.getMessage());
    }
}
