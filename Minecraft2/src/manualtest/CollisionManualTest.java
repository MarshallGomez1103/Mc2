package manualtest;

import domain.Position;
import domain.block.BlockType;
import domain.player.CollisionResolver;
import domain.player.Player;
import domain.player.PlayerPhysics;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.time.Instant;

/**
 * Comprobación manual de {@link CollisionResolver} (Participante 3).
 *
 * No hay JUnit configurado todavía (ver sección 0.8 del plan compartido), así
 * que, igual que {@link WorldBlockOperationsTest}, esta clase simplemente
 * ejecuta un {@code main()} que lanza {@link AssertionError} si algo falla.
 *
 * Verifica, tal como pide el plan para el Participante 3:
 * (a) el jugador cae y se detiene sobre un piso sólido, con onGround = true;
 * (b) el jugador no puede atravesar una pared lateral;
 * (c) tras aterrizar, un salto lo separa del piso (onGround vuelve a false
 *     mientras está en el aire) y luego vuelve a aterrizar.
 */
public final class CollisionManualTest {

    private static final double DELTA_SECONDS = 0.05;
    private static final int MAX_STEPS = 500;

    private CollisionManualTest() {
    }

    public static void main(String[] args) {
        World world = buildTestWorld();
        CollisionResolver resolver = new CollisionResolver();
        PlayerPhysics physics = new PlayerPhysics();

        verifyFallsAndLandsOnFloor(world, resolver, physics);
        verifyCannotPassThroughWall(world, resolver);
        verifyJumpLeavesGroundThenLandsAgain(world, resolver, physics);

        System.out.println("Prueba de colisión superada: caída sobre piso, pared lateral y salto.");
    }

    /** (a) El jugador cae desde el aire y se detiene sobre el piso, con onGround = true. */
    private static void verifyFallsAndLandsOnFloor(World world, CollisionResolver resolver, PlayerPhysics physics) {
        Player player = new Player(2.0, 6.0, 2.0); // por encima del piso en (2, *, 2)

        runUntilGroundedOrTimeout(player, world, resolver, physics);

        verify(player.isOnGround(), "El jugador debería estar en el suelo tras caer");
        // El piso ocupa el bloque (x, 0, z); el jugador debería quedar apoyado
        // justo encima, con y cercano a 1.0 (permitiendo el margen del paso de simulación).
        verify(player.getY() >= 1.0 - 1e-6 && player.getY() < 1.5,
                "Y inesperado tras aterrizar: " + player.getY());
        System.out.println("(a) Caída y aterrizaje: onGround=" + player.isOnGround() + ", y=" + player.getY());
    }

    /** (b) El jugador no puede atravesar una pared lateral. */
    private static void verifyCannotPassThroughWall(World world, CollisionResolver resolver) {
        // Ya apoyado en el piso (y = 1.0), z = 3, avanzando en +X hacia la pared en x = 5.
        Player player = new Player(4.0, 1.0, 3.0);
        player.setOnGround(true);

        for (int step = 0; step < 20; step++) {
            resolver.resolveAndApply(player, world, 0.1, 0.0, 0.0);
        }

        verify(player.getX() < 4.8, "El jugador atravesó la pared, x=" + player.getX());
        verify(player.getX() > 4.0, "El jugador no avanzó en absoluto hacia la pared, x=" + player.getX());
        System.out.println("(b) Bloqueo por pared lateral: x final=" + player.getX());
    }

    /** (c) Tras aterrizar, un salto separa al jugador del piso y luego vuelve a aterrizar. */
    private static void verifyJumpLeavesGroundThenLandsAgain(World world, CollisionResolver resolver,
                                                               PlayerPhysics physics) {
        Player player = new Player(2.0, 1.0, 2.0);
        player.setOnGround(true);

        physics.jump(player);
        verify(!player.isOnGround(), "El salto debería separar al jugador del piso");
        verify(player.getVelocityY() > 0, "El salto debería dar velocidad vertical positiva");

        // Un par de ticks de ascenso: debe seguir sin tocar el piso.
        for (int i = 0; i < 5; i++) {
            physics.applyGravity(player, DELTA_SECONDS);
            double dy = physics.computeIntendedDeltaY(player, DELTA_SECONDS);
            resolver.resolveAndApply(player, world, 0.0, dy, 0.0);
        }
        verify(!player.isOnGround(), "El jugador no debería estar en el suelo mientras asciende");

        // Deja que la gravedad lo traiga de vuelta y aterrice otra vez.
        runUntilGroundedOrTimeout(player, world, resolver, physics);
        verify(player.isOnGround(), "El jugador debería volver a aterrizar tras el salto");
        System.out.println("(c) Salto y reaterrizaje: onGround=" + player.isOnGround() + ", y=" + player.getY());
    }

    private static void runUntilGroundedOrTimeout(Player player, World world, CollisionResolver resolver,
                                                    PlayerPhysics physics) {
        int steps = 0;
        while (!player.isOnGround() && steps < MAX_STEPS) {
            physics.applyGravity(player, DELTA_SECONDS);
            double dy = physics.computeIntendedDeltaY(player, DELTA_SECONDS);
            resolver.resolveAndApply(player, world, 0.0, dy, 0.0);
            steps++;
        }
        verify(steps < MAX_STEPS, "La simulación no convergió a onGround dentro del límite de pasos");
    }

    /** Piso sólido de 8x8 en (0..7, y=0, 0..7) y una pared de 3 bloques en x=5, z=3, y=1..3. */
    private static World buildTestWorld() {
        BlockFactory blockFactory = new BlockFactory();
        World world = new World("collision-test-world", 0L, Instant.EPOCH);
        Chunk chunk = new Chunk(0, 0);
        world.addChunk(chunk);

        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                chunk.addBlock(blockFactory.create(BlockType.STONE, new Position(x, 0, z)));
            }
        }

        for (int y = 1; y <= 3; y++) {
            chunk.addBlock(blockFactory.create(BlockType.STONE, new Position(5, y, 3)));
        }

        return world;
    }

    private static void verify(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
