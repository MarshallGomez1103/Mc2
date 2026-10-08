package application;

import domain.Position;
import domain.block.BlockType;
import domain.enemy.Difficulty;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerMovementService;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PlayerControlStaminaTest {
    private World world(Player player) {
        World world = new World("stamina", "stamina", 1, Instant.EPOCH, player);
        world.addChunk(new Chunk(0, 0));
        return world;
    }
    private PlayerControlService input(World world) {
        return new PlayerControlService(world, new PlayerInteractionService(),
                new EnemyUpdateService(world, Difficulty.NORMAL.zombieParameters()), new PistolService(world));
    }
    private static final MovementInput SPRINT = new MovementInput(true, false, false, false, true);

    @Test void shiftStandingStillOrOppositeKeysConsumesNothing() {
        Player player = new Player(4.5, 20, 4.5);
        World world = world(player);
        PlayerControlService input = input(world);
        input.updateMovement(player, world, new MovementInput(false, false, false, false, true), false, .05);
        input.updateMovement(player, world, new MovementInput(true, true, false, false, true), false, .05);
        assertEquals(100, input.state().staminaCurrent());
        assertFalse(input.state().sprinting());
        assertFalse(input.state().moving());
    }

    @Test void blockedByWallConsumesNothing() {
        Player player = new Player(4.5, 1, 4.69);
        World world = world(player);
        BlockFactory factory = new BlockFactory();
        for (int y = 0; y < 3; y++) {
            world.placeBlock(0, 0, factory.create(BlockType.STONE, new Position(4, y, 5)));
        }
        world.placeBlock(0, 0, factory.create(BlockType.STONE, new Position(4, 0, 4)));
        PlayerControlService input = input(world);
        input.updateMovement(player, world, SPRINT, false, .05);
        assertEquals(4.69, player.getZ());
        assertEquals(100, input.state().staminaCurrent());
        assertFalse(input.state().sprinting());
    }

    @Test void sprintDistanceAndEnergyMatchAcrossFrameRates() {
        for (int fps : new int[] {30, 60, 144}) {
            Player player = new Player(4.5, 50, 4.5);
            World world = world(player);
            PlayerControlService input = input(world);
            for (int i = 0; i < 10 * fps; i++) {
                input.updateMovement(player, world, SPRINT, false, 1.0 / fps);
            }
            assertEquals(4.5 + 10 * Player.MOVE_SPEED * PlayerMovementService.SPRINT_MULTIPLIER,
                    player.getZ(), 1e-7);
            assertEquals(0, input.state().staminaCurrent(), 1e-7);
            assertTrue(input.state().exhausted());
            double previous = player.getZ();
            input.updateMovement(player, world, SPRINT, false, .05);
            assertEquals(Player.MOVE_SPEED * .05, player.getZ() - previous, 1e-8);
            assertFalse(input.state().sprinting());
            input.resetAfterRespawn();
            assertEquals(100, input.state().staminaCurrent());
            assertFalse(input.state().moving());
        }
    }
    @Test void largeDeltaIsClampedBeforeMovementAndStamina() {
        Player p = new Player(4.5, 50, 4.5);
        World w = world(p);
        PlayerControlService input = input(w);
        input.updateMovement(p, w, SPRINT, false, 2);
        assertEquals(4.5 + Player.MOVE_SPEED * PlayerMovementService.SPRINT_MULTIPLIER * .05, p.getZ(), 1e-9);
        assertEquals(99.5, input.state().staminaCurrent(), 1e-9);
    }
    @Test void invalidDeltaRejectsBeforeChangingPlayerOrEnergy() {
        for (double delta : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            Player p = new Player(4.5, 50, 4.5);
            World w = world(p);
            PlayerControlService input = input(w);
            assertThrows(IllegalArgumentException.class, () -> input.updateMovement(p, w, SPRINT, true, delta));
            assertEquals(4.5, p.getZ());
            assertEquals(0, p.getVelocityY());
            assertEquals(100, input.state().staminaCurrent());
        }
    }
}
