package presentation.game;

import application.EnemyUpdateService;
import application.PlayerInteractionService;
import application.ZombieMeleeService;
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

class GameInputStaminaTest {
    private World world(Player player) {
        World world = new World("stamina", "stamina", 1, Instant.EPOCH, player);
        world.addChunk(new Chunk(0, 0));
        return world;
    }
    private GameInput input(World world) {
        return new GameInput(new PlayerInteractionService(), new ZombieMeleeService(world),
                new EnemyUpdateService(world, Difficulty.NORMAL.zombieParameters()));
    }
    private static final MovementInput SPRINT = new MovementInput(true, false, false, false, true);

    @Test void shiftStandingStillOrOppositeKeysConsumesNothing() {
        Player player = new Player(4.5, 20, 4.5);
        World world = world(player);
        GameInput input = input(world);
        input.updateMovement(player, world, new MovementInput(false, false, false, false, true), false, .05);
        input.updateMovement(player, world, new MovementInput(true, true, false, false, true), false, .05);
        assertEquals(100, input.stamina().current());
        assertFalse(input.isSprinting());
        assertFalse(input.isMoving());
    }

    @Test void blockedByWallConsumesNothing() {
        Player player = new Player(4.5, 1, 4.69);
        World world = world(player);
        BlockFactory factory = new BlockFactory();
        for (int y = 0; y < 3; y++) {
            world.placeBlock(0, 0, factory.create(BlockType.STONE, new Position(4, y, 5)));
        }
        world.placeBlock(0, 0, factory.create(BlockType.STONE, new Position(4, 0, 4)));
        GameInput input = input(world);
        input.updateMovement(player, world, SPRINT, false, .05);
        assertEquals(4.69, player.getZ());
        assertEquals(100, input.stamina().current());
        assertFalse(input.isSprinting());
    }

    @Test void sprintDistanceAndEnergyMatchAcrossFrameRates() {
        for (int fps : new int[] {30, 60, 144}) {
            Player player = new Player(4.5, 50, 4.5);
            World world = world(player);
            GameInput input = input(world);
            for (int i = 0; i < 10 * fps; i++) {
                input.updateMovement(player, world, SPRINT, false, 1.0 / fps);
            }
            assertEquals(4.5 + 10 * Player.MOVE_SPEED * PlayerMovementService.SPRINT_MULTIPLIER,
                    player.getZ(), 1e-7);
            assertEquals(0, input.stamina().current(), 1e-7);
            assertTrue(input.stamina().exhausted());
            double previous = player.getZ();
            input.updateMovement(player, world, SPRINT, false, .05);
            assertEquals(Player.MOVE_SPEED * .05, player.getZ() - previous, 1e-8);
            assertFalse(input.isSprinting());
            input.resetAfterRespawn();
            assertEquals(100, input.stamina().current());
            assertFalse(input.isMoving());
        }
    }
}
