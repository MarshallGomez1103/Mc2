package manualtest;

import application.PlayerInteractionService;
import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.BlockChange;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;
import patterns.observer.Observer;
import patterns.singleton.WorldManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprobación manual de {@link PlayerInteractionService} (Participante 4) y,
 * a través de ella, de los criterios de terminado que le corresponden a
 * Estudiante 2 según {@code docs/decisiones-compartidas.md}:
 *
 * <ul>
 *   <li>CT-06 (Observer): colocar/eliminar un bloque existente notifica una
 *       vez con PLACED/REMOVED; eliminar aire no notifica.</li>
 *   <li>CT-07 (Singleton): {@code WorldManager.getInstance()} siempre es la
 *       misma instancia; solo un mundo activo a la vez.</li>
 * </ul>
 *
 * Igual que {@link WorldBlockOperationsTest} y {@link CollisionManualTest},
 * no hay JUnit configurado todavía (sección 0.8 del plan compartido): esta
 * clase ejecuta un {@code main()} que corre las seis verificaciones pedidas,
 * imprime PASA/FALLA de cada una y, al final, lanza {@link AssertionError}
 * con el resumen si alguna falló.
 */
public final class BlockInteractionAndPatternsManualTest {

    private static final List<String> RESULTS = new ArrayList<>();
    private static boolean allPassed = true;

    private BlockInteractionAndPatternsManualTest() {
    }

    public static void main(String[] args) {
        PlayerInteractionService interactionService = new PlayerInteractionService();

        // (1) Mundo con un chunk y un piso sólido, más dos bloques de pared
        // separados (uno para probar eliminar, otro para probar colocar, sin
        // que un test interfiera con el rayo del otro).
        World world = buildTestWorld();

        // (2) Observer de prueba que cuenta notificaciones y guarda la última.
        RecordingObserver observer = new RecordingObserver();
        world.addObserver(observer);

        // (3) Jugador apuntando al primer bloque de pared -> eliminar.
        // x/z centrados en 2.5 (no 2.0) a propósito: con yaw exactamente en un
        // múltiplo de 90°, el componente perpendicular de la dirección no da
        // matemáticamente cero por redondeo de punto flotante (es un épsilon
        // minúsculo, p. ej. -1.8e-16). Si el jugador estuviera parado justo
        // sobre una coordenada entera, ese épsilon podría empujar el piso justo
        // por debajo del entero y cambiar a qué bloque redondea el rayo. Centrar
        // al jugador en el bloque (x.5, z.5) evita ese caso límite.
        Player removerPlayer = new Player(2.5, 1.0, 2.5);
        removerPlayer.setYaw(270f); // mirando hacia +X, según forwardVector()
        removerPlayer.setPitch(0f);
        Position wallA = new Position(5, 2, 2);

        interactionService.removeTargetedBlock(removerPlayer, world);
        check("(3) Eliminar bloque apuntado notifica una vez con REMOVED",
                observer.count == 1
                        && observer.last != null
                        && observer.last.type() == BlockChange.Type.REMOVED
                        && observer.last.block().getPosition().equals(wallA)
                        && world.findChunk(wallA).flatMap(chunk -> chunk.getBlock(wallA)).isEmpty());

        // (4) Intentar eliminar de nuevo en el mismo lugar (ahora aire): no
        // debe notificar otra vez (CT-06: eliminar aire no notifica). Como ya
        // no hay nada sólido en la trayectoria del rayo, raycast() devuelve
        // Optional.empty() y removeTargetedBlock no hace nada.
        int countBeforeSecondRemove = observer.count;
        interactionService.removeTargetedBlock(removerPlayer, world);
        check("(4) Eliminar aire no genera una notificación adicional (CT-06)",
                observer.count == countBeforeSecondRemove);

        // (5) Jugador apuntando al segundo bloque de pared -> colocar uno nuevo
        // adyacente (en la posición de aire justo antes del bloque apuntado).
        Player placerPlayer = new Player(2.5, 1.0, 5.5);
        placerPlayer.setYaw(270f);
        placerPlayer.setPitch(0f);
        Position wallB = new Position(5, 2, 5);
        Position expectedPlacePosition = new Position(4, 2, 5);

        int countBeforePlace = observer.count;
        interactionService.placeBlockOfType(placerPlayer, world, BlockType.WOOD);
        Block placedBlock = world.findChunk(expectedPlacePosition)
                .flatMap(chunk -> chunk.getBlock(expectedPlacePosition))
                .orElse(null);
        check("(5) Colocar un bloque nuevo notifica una vez con PLACED",
                observer.count == countBeforePlace + 1
                        && observer.last != null
                        && observer.last.type() == BlockChange.Type.PLACED
                        && placedBlock != null
                        && placedBlock.getType() == BlockType.WOOD
                        && placedBlock.getPosition().equals(expectedPlacePosition));
        // wallB debe seguir intacto: solo se agregó el vecino, no se tocó el apuntado.
        check("(5b) El bloque apuntado (wallB) no se modifica al colocar el vecino",
                world.findChunk(wallB).flatMap(chunk -> chunk.getBlock(wallB))
                        .map(block -> block.getType() == BlockType.STONE)
                        .orElse(false));

        // (6) CT-07: WorldManager siempre la misma instancia; solo un mundo activo.
        WorldManager managerRef1 = WorldManager.getInstance();
        WorldManager managerRef2 = WorldManager.getInstance();
        check("(6a) WorldManager.getInstance() siempre devuelve la misma instancia (CT-07)",
                managerRef1 == managerRef2);

        World worldA = new World("ct07-world-a");
        worldA.addChunk(new Chunk(0, 0));
        World worldB = new World("ct07-world-b");
        worldB.addChunk(new Chunk(0, 0));

        managerRef1.load(worldA);
        boolean loadedA = managerRef1.getCurrentWorld().map(w -> w == worldA).orElse(false);
        managerRef1.load(worldB);
        boolean nowB = managerRef1.getCurrentWorld().map(w -> w == worldB).orElse(false);
        boolean noLongerA = managerRef1.getCurrentWorld().map(w -> w != worldA).orElse(false);
        check("(6b) Cargar un mundo B reemplaza al mundo A como único mundo activo (CT-07)",
                loadedA && nowB && noLongerA);
        managerRef1.unload(); // deja el singleton limpio para otras pruebas del mismo proceso.

        // (7) Resumen PASA/FALLA.
        System.out.println();
        System.out.println("=== Resumen: interacción del jugador (CT-06 / CT-07) ===");
        RESULTS.forEach(System.out::println);

        if (!allPassed) {
            throw new AssertionError("Al menos una verificación de interacción/patrones falló; ver resumen arriba.");
        }
        System.out.println("Todas las verificaciones de interacción y patrones (CT-06, CT-07) pasaron.");
    }

    /** Piso sólido 8x8 en (0..7, y=0, 0..7) y dos bloques de pared separados en z, a la altura del ojo. */
    private static World buildTestWorld() {
        BlockFactory blockFactory = new BlockFactory();
        World world = new World("interaction-test-world");
        Chunk chunk = new Chunk(0, 0);
        world.addChunk(chunk);

        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                chunk.addBlock(blockFactory.create(BlockType.STONE, new Position(x, 0, z)));
            }
        }

        chunk.addBlock(blockFactory.create(BlockType.STONE, new Position(5, 2, 2))); // wallA (eliminar)
        chunk.addBlock(blockFactory.create(BlockType.STONE, new Position(5, 2, 5))); // wallB (colocar vecino)

        return world;
    }

    private static void check(String description, boolean condition) {
        String outcome = condition ? "PASA" : "FALLA";
        RESULTS.add(outcome + " -> " + description);
        if (!condition) {
            allPassed = false;
        }
    }

    /** Observer de prueba: cuenta notificaciones y guarda la última recibida. */
    private static final class RecordingObserver implements Observer<BlockChange> {
        private int count;
        private BlockChange last;

        @Override
        public void update(BlockChange notification) {
            count++;
            last = notification;
        }
    }
}
