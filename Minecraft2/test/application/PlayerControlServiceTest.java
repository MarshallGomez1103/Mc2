package application;

import domain.Position;
import domain.block.BlockType;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Integración en memoria del coordinador con reglas, raycast, arma y mundo reales. */
class PlayerControlServiceTest {
    private World world;
    private Player player;
    private EnemyUpdateService enemies;
    private PistolService pistol;
    private PlayerControlService control;

    @BeforeEach void setup() {
        world = TestWorlds.flat(1, 1);
        player = world.getPlayer();
        player.setX(5.5); player.setY(21); player.setZ(5.5);
        player.setOnGround(true);
        enemies = new EnemyUpdateService(world, domain.enemy.ZombieParameters.defaults());
        pistol = new PistolService(world);
        control = new PlayerControlService(world, new PlayerInteractionService(), enemies, pistol);
    }
    private PlayerFrameInput action(boolean primary, boolean secondary, boolean toggle, BlockType selection) {
        return new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(), false, 0, 0,
                primary, secondary, toggle, selection);
    }
    private void acquirePistol() {
        pistol.update(3, .01);
        Position pickup = pistol.pickupPosition().orElseThrow();
        player.setX(pickup.x() + .5); player.setY(pickup.y()); player.setZ(pickup.z() + .5);
        pistol.update(3, .01);
        player.setX(5.5); player.setY(21); player.setZ(5.5);
        assertTrue(pistol.equipped());
    }
    @Test void meleeTakesPriorityOverTheBlockBehindTheTarget() {
        // Arrange
        Zombie zombie = new Zombie(5.5, 21, 7.5, 3);
        enemies.register(zombie);
        TestWorlds.place(world, 5, 22, 9, BlockType.STONE);
        // Act
        control.update(.01, action(true, false, false, null));
        // Assert
        assertEquals(2, zombie.getHealth());
        assertTrue(world.findChunk(0, 0).orElseThrow().getBlock(new Position(5, 22, 9)).isPresent());
    }
    @Test void primaryActionWithoutEnemyRemovesTheTargetedBlock() {
        TestWorlds.place(world, 5, 22, 8, BlockType.STONE);
        control.update(.01, action(true, false, false, null));
        assertTrue(world.findChunk(0, 0).orElseThrow().getBlock(new Position(5, 22, 8)).isEmpty());
    }
    @Test void selectedMaterialAppliesToSecondaryActionInTheSameTick() {
        TestWorlds.place(world, 5, 22, 8, BlockType.STONE);
        control.update(.01, action(false, true, false, BlockType.WOOD));
        assertEquals(BlockType.WOOD, control.state().selectedType());
        assertEquals(BlockType.WOOD, world.findChunk(0, 0).orElseThrow()
                .getBlock(new Position(5, 22, 7)).orElseThrow().getType());
    }
    @Test void pistolHitAndCooldownNeverFallBackToMining() {
        acquirePistol();
        Zombie zombie = new Zombie(5.5, 21, 7.5, 3);
        enemies.register(zombie);
        TestWorlds.place(world, 5, 22, 9, BlockType.STONE);
        control.update(.01, action(true, false, false, null));
        assertFalse(zombie.isAlive());
        double cooldown = pistol.cooldownSeconds();
        control.update(.01, action(true, false, false, null));
        assertEquals(cooldown, pistol.cooldownSeconds());
        assertTrue(world.findChunk(0, 0).orElseThrow().getBlock(new Position(5, 22, 9)).isPresent());
    }
    @Test void selectionWinsOverPistolToggleAndRestoresMeleeInTheSameTick() {
        acquirePistol();
        pistol.unequip();
        Zombie zombie = new Zombie(5.5, 21, 7.5, 3);
        enemies.register(zombie);
        control.update(.01, action(true, false, true, BlockType.LEAVES));
        assertFalse(pistol.equipped());
        assertEquals(2, zombie.getHealth());
        assertEquals(BlockType.LEAVES, control.state().selectedType());
    }
    @Test void lookChangesMovementDirectionBeforeResolvingCollisions() {
        var movement = new domain.player.MovementInput(true, false, false, false);
        control.update(.05, new PlayerFrameInput(movement, false, 90, 200,
                false, false, false, null));
        assertEquals(90, player.getYaw());
        assertEquals(89, player.getPitch());
        assertEquals(5.5 - Player.MOVE_SPEED * .05, player.getX(), 1e-9);
        assertEquals(5.5, player.getZ(), 1e-9);
    }
    @Test void jumpRequiresGroundAndNeverAddsASecondImpulseInTheAir() {
        var jump = new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(), true, 0, 0,
                false, false, false, null);
        control.update(.05, jump);
        double velocity = player.getVelocityY();
        assertFalse(player.isOnGround());
        assertTrue(player.getY() > 21);
        control.update(.05, jump);
        assertEquals(velocity + Player.GRAVITY * .05, player.getVelocityY(), 1e-9);
    }
    @Test void viewSnapshotDoesNotChangeWhenControlsAdvanceOrReset() {
        var before = control.state();
        var sprint = new domain.player.MovementInput(true, false, false, false, true);
        control.update(.05, new PlayerFrameInput(sprint, false, 0, 0, false, false, false, BlockType.SAND));
        assertEquals(100, before.staminaCurrent());
        assertEquals(BlockType.STONE, before.selectedType());
        assertEquals(99.5, control.state().staminaCurrent(), 1e-9);
        assertTrue(control.state().moving());
        control.resetAfterRespawn();
        assertEquals(100, control.state().staminaCurrent());
        assertEquals(BlockType.SAND, control.state().selectedType());
        assertFalse(control.state().moving());
        assertFalse(control.state().sprinting());
    }
}
