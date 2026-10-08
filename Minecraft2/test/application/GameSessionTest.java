package application;

import domain.block.BlockType;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.MovementInput;
import domain.player.PlayerLife;
import domain.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
    private World world() {
        World world = TestWorlds.flat(2, 2);
        world.getPlayer().setX(16.5);
        world.getPlayer().setY(21);
        world.getPlayer().setZ(16.5);
        return world;
    }
    private static final PlayerFrameInput SPRINT = new PlayerFrameInput(
            new MovementInput(true, false, false, false, true), false, 0, 0,
            false, false, false, null);

    @Test void pausedFreezesInputHordeAndEnemyTimers() {
        // Arrange
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        Zombie zombie = session.enemies().spawnAt(8.5, 8.5).orElseThrow();
        double enemyX = zombie.getX(), countdown = session.hordes().secondsToNextWave();
        var controls = session.controls();
        double playerZ = world.getPlayer().getZ(), playerY = world.getPlayer().getY();
        session.setPaused(true);
        // Act
        for (int i = 0; i < 500; i++) session.advance(.05, SPRINT);
        // Assert
        assertEquals(countdown, session.hordes().secondsToNextWave());
        assertEquals(enemyX, zombie.getX());
        assertEquals(0, zombie.getDeadSeconds());
        assertEquals(playerZ, world.getPlayer().getZ());
        assertEquals(playerY, world.getPlayer().getY());
        assertEquals(controls, session.controls());
        assertEquals(GameSession.State.PAUSED, session.state());
        session.setPaused(false);
        session.advance(.05, PlayerFrameInput.NEUTRAL);
        assertTrue(session.hordes().secondsToNextWave() < countdown);
    }

    @Test void deathFreezesAndRespawnRestoresRunningAndEnergy() {
        // Arrange
        World world = world();
        GameSettings settings = new GameSettings();
        settings.toggleEnemies();
        GameSession session = new GameSession(world, settings);
        session.advance(.05, SPRINT);
        assertEquals(99.5, session.controls().staminaCurrent(), 1e-9);
        session.life().die(PlayerLife.DeathCause.ENEMY);
        var controls = session.controls();
        double playerZ = world.getPlayer().getZ(), playerY = world.getPlayer().getY();
        // Act
        session.advance(.05, SPRINT);
        // Assert
        assertEquals(GameSession.State.DEAD, session.state());
        assertEquals(playerZ, world.getPlayer().getZ());
        assertEquals(playerY, world.getPlayer().getY());
        assertEquals(controls, session.controls());
        session.setPaused(true);
        session.setPaused(false);
        assertEquals(GameSession.State.DEAD, session.state());
        session.respawn();
        assertEquals(GameSession.State.RUNNING, session.state());
        assertEquals(100, session.controls().staminaCurrent());
        assertFalse(session.controls().moving());
        assertFalse(session.controls().sprinting());
        assertEquals(0, world.getPlayer().getVelocityY());
        assertEquals(100, session.life().health());
        session.advance(.05, SPRINT);
        assertTrue(session.controls().moving());
    }

    @Test void settingsKeepDifficultySnapshotAndApplyEnemyToggleImmediately() {
        GameSettings settings = new GameSettings();
        GameSession session = new GameSession(world(), settings);
        settings.cycleDifficulty();
        settings.toggleEnemies();
        session.applySettings(settings);
        assertNotEquals(settings.difficulty(), session.difficulty());
        assertFalse(session.enemies().isEnabled());
        assertEquals(session.difficulty().zombieParameters(), session.enemies().parameters());
        double countdown = session.hordes().secondsToNextWave();
        session.advance(.05, PlayerFrameInput.NEUTRAL);
        assertEquals(countdown, session.hordes().secondsToNextWave());
    }

    @Test void invalidTimeDoesNotExecuteInputOrValidateAnUnusedInput() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        double countdown = session.hordes().secondsToNextWave();
        for (double delta : new double[] {Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, -1, 0}) {
            assertDoesNotThrow(() -> session.advance(delta, null));
            session.advance(delta, SPRINT);
        }
        assertEquals(16.5, world.getPlayer().getZ());
        assertEquals(21, world.getPlayer().getY());
        assertEquals(100, session.controls().staminaCurrent());
        assertEquals(countdown, session.hordes().secondsToNextWave());
        assertThrows(NullPointerException.class, () -> session.advance(.05, null));
    }

    @ParameterizedTest @ValueSource(doubles = {.000001, .049999, .05, .050001, 4})
    void allComponentsUseTheSameBoundedPositiveDelta(double rawDelta) {
        // Arrange
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        double countdown = session.hordes().secondsToNextWave();
        double delta = Math.min(rawDelta, .05);
        // Act
        session.advance(rawDelta, SPRINT);
        // Assert
        assertEquals(16.5 + domain.player.Player.MOVE_SPEED * 1.65 * delta,
                world.getPlayer().getZ(), 1e-9);
        assertEquals(100 - 10 * delta, session.controls().staminaCurrent(), 1e-9);
        assertEquals(countdown - delta, session.hordes().secondsToNextWave(), 1e-9);
    }

    @Test void pauseAndDeathIgnoreLookActionsAndWeaponCooldown() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        session.pistol().update(3, .01);
        var pickup = session.pistol().pickupPosition().orElseThrow();
        world.getPlayer().setX(pickup.x() + .5);
        world.getPlayer().setY(pickup.y());
        world.getPlayer().setZ(pickup.z() + .5);
        session.pistol().update(3, .01);
        session.pistol().shoot(world.getPlayer(), java.util.List.of());
        double cooldown = session.pistol().cooldownSeconds();
        PlayerFrameInput actions = new PlayerFrameInput(SPRINT.movement(), true, 15, 10,
                true, true, true, BlockType.WOOD);
        for (boolean dead : new boolean[] {false, true}) {
            if (dead) {
                session.setPaused(false);
                session.life().die(PlayerLife.DeathCause.ENEMY);
            } else session.setPaused(true);
            session.advance(.05, actions);
            assertEquals(0, world.getPlayer().getYaw());
            assertEquals(0, world.getPlayer().getPitch());
            assertEquals(BlockType.STONE, session.controls().selectedType());
            assertEquals(cooldown, session.pistol().cooldownSeconds());
            assertTrue(session.pistol().equipped());
        }
    }

    @Test void respawnConservesSelectedMaterialAndDoesNotResumeAPausedSession() {
        GameSession session = new GameSession(world(), new GameSettings());
        session.advance(.01, new PlayerFrameInput(PlayerFrameInput.NEUTRAL.movement(),
                false, 0, 0, false, false, false, BlockType.WOOD));
        session.life().die(PlayerLife.DeathCause.VOID);
        session.setPaused(true);
        session.respawn();
        assertEquals(BlockType.WOOD, session.controls().selectedType());
        assertEquals(GameSession.State.PAUSED, session.state());
        assertFalse(session.life().isDead());
    }
}
