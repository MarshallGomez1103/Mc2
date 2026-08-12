package presentation;

import application.WorldApplicationService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Interfaz de consola inicial. Delega los casos de uso a la capa application. */
public final class MainMenu {
    private final WorldApplicationService worldService;
    private final Scanner scanner;

    public MainMenu(WorldApplicationService worldService) {
        this.worldService = Objects.requireNonNull(worldService, "worldService no puede ser null");
        this.scanner = new Scanner(System.in);
    }

    public void show() {
        boolean running = true;
        while (running) {
            printOptions();
            String option = scanner.nextLine().trim();
            try {
                running = handle(option);
            } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
                System.out.println("No fue posible completar la operación: " + exception.getMessage());
            }
        }
    }

    private boolean handle(String option) throws IOException {
        return switch (option) {
            case "1" -> {
                String name = askWorldName();
                worldService.createWorld(name);
                System.out.println("Mundo creado y cargado: " + name);
                yield true;
            }
            case "2" -> {
                List<String> worlds = worldService.listWorlds();
                System.out.println(worlds.isEmpty() ? "No hay mundos guardados." : String.join("\n", worlds));
                yield true;
            }
            case "3" -> {
                String name = askWorldName();
                worldService.loadWorld(name);
                System.out.println("Mundo cargado: " + name);
                yield true;
            }
            case "4" -> {
                worldService.saveCurrentWorld();
                System.out.println("Mundo actual guardado.");
                yield true;
            }
            case "5" -> {
                String name = askWorldName();
                worldService.deleteWorld(name);
                System.out.println("Mundo eliminado: " + name);
                yield true;
            }
            case "0" -> false;
            default -> {
                System.out.println("Opción no válida.");
                yield true;
            }
        };
    }

    private String askWorldName() {
        System.out.print("Nombre del mundo: ");
        return scanner.nextLine().trim();
    }

    private void printOptions() {
        System.out.println("""

                === Minecraft 2 ===
                1. Crear mundo
                2. Listar mundos
                3. Cargar mundo
                4. Guardar mundo actual
                5. Eliminar mundo
                0. Salir
                Seleccione una opción:
                """);
    }
}
