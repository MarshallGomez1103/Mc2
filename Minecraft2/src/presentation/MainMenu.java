package presentation;

import application.WorldApplicationService;
import domain.world.World;
import presentation.game.GameWindow;

import java.io.IOException;
import java.util.List;
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
        try {
            worldService.createWorld(name.get());
            console.info("Mundo creado y cargado: " + name.get());
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
        console.info("WASD para moverte, espacio para saltar, clic para colocar y eliminar bloques.");
        console.info("Pulsa ESC o cierra la ventana para volver a este menú.");

        gameWindow.play(world.get());

        console.blank();
        console.info("De vuelta en el menú. Usa \"4. Guardar mundo actual\" para conservar los cambios.");
    }

    // ------------------------------------------------------------------ utilidades

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
                0. Salir""");
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
