package application;

import domain.block.BlockType;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import patterns.factory.BlockFactory;
import persistence.WorldJsonCodec;
import static org.junit.jupiter.api.Assertions.*;

/** Several players in one session: the multiplayer rules of the application layer. */
class GameSessionMultiPlayerTest {
    private static final double TICK = 1.0 / 20;
    private static final PlayerFrameInput FORWARD = new PlayerFrameInput(
            new MovementInput(true, false, false, false), false, 0, 0, false, false, false, null);
    private static final PlayerFrameInput SPRINT = new PlayerFrameInput(
            new MovementInput(true, false, false, false, true), false, 0, 0,
            false, false, false, null);

    private static World world() {
        World world = TestWorlds.flat(2, 2);
        world.getPlayer().setX(16.5);
        world.getPlayer().setY(21);
        world.getPlayer().setZ(16.5);
        return world;
    }

    private static GameSession quiet(World world) {
        GameSettings settings = new GameSettings();
        settings.toggleEnemies();
        return new GameSession(world, settings);
    }

    private static void teleport(Player player, double x, double z) {
        player.setX(x);
        player.setY(21);
        player.setZ(z);
    }

    private static void run(GameSession session, int ticks, Map<Integer, PlayerFrameInput> inputs) {
        for (int tick = 0; tick < ticks; tick++) session.advanceAll(TICK, inputs);
    }

    /** Deterministic input script that moves, looks around, jumps, shoots and swaps material. */
    private static PlayerFrameInput scripted(int tick) {
        return new PlayerFrameInput(new MovementInput(tick % 40 < 30, false, false, tick % 90 > 60,
                tick % 200 < 60), tick % 37 == 0, (tick % 11) - 5, (tick % 7) - 3, tick % 13 == 0,
                tick % 29 == 0, tick % 150 == 0, tick % 100 == 0 ? BlockType.WOOD : null);
    }

    private static String snapshot(GameSession session, World world) {
        StringBuilder out = new StringBuilder(String.valueOf(session.state()));
        world.players().forEach((id, p) -> out.append('|').append(id).append(':').append(p.getX())
                .append(',').append(p.getY()).append(',').append(p.getZ()).append(',')
                .append(p.getYaw()).append(',').append(session.life(id).health()).append(',')
                .append(session.controls(id)));
        session.enemies().zombies().forEach(z -> out.append(';').append(z.getX()).append(',')
                .append(z.getZ()).append(',').append(z.getState()).append(',').append(z.getHealth()));
        return out.toString();
    }

    @Test void mapOverloadWithOnlyThePrimaryMatchesTheOldOverloadOnEveryTick() {
        World oldWorld = world();
        World mapWorld = world();
        GameSession oldSession = new GameSession(oldWorld, new GameSettings());
        GameSession mapSession = new GameSession(mapWorld, new GameSettings());
        oldSession.enemies().spawnAt(20.5, 20.5).orElseThrow();
        mapSession.enemies().spawnAt(20.5, 20.5).orElseThrow();
        List<String> before = new ArrayList<>();
        List<String> after = new ArrayList<>();
        for (int tick = 0; tick < 200; tick++) {
            oldSession.advance(TICK, scripted(tick));
            mapSession.advanceAll(TICK, Map.of(0, scripted(tick)));
            before.add(snapshot(oldSession, oldWorld));
            after.add(snapshot(mapSession, mapWorld));
        }
        assertEquals(before, after);
        assertNotEquals(before.get(0), before.get(199), "the script must actually change the state");
    }

    @Test void joinAddsAPlayerAtTheFirstLivingPlayersPositionWithItsSkin() {
        World world = world();
        GameSession session = quiet(world);
        int id = session.join(2);
        assertEquals(1, id);
        Player joined = world.player(id).orElseThrow();
        assertNotSame(world.getPlayer(), joined);
        assertEquals(16.5, joined.getX());
        assertEquals(21, joined.getY());
        assertEquals(16.5, joined.getZ());
        assertEquals(2, joined.getSkinId());
        assertEquals(0, world.getPlayer().getSkinId());
        assertEquals(PlayerLife.MAX_HEALTH, session.life(id).health());
    }

    @Test void joinWithSkinOutsideZeroToTwoIsRejectedAndAddsNoPlayer() {
        World world = world();
        GameSession session = quiet(world);
        assertThrows(IllegalArgumentException.class, () -> session.join(-1));
        assertThrows(IllegalArgumentException.class, () -> session.join(Player.MAX_SKIN_ID + 1));
        assertEquals(1, world.players().size());
        assertEquals(1, session.join(0), "the rejected joins must not consume an id");
    }

    @Test void eachPlayerMovesOnlyWithItsOwnInputAndAMissingInputIsNeutral() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        Player primary = world.getPlayer();
        Player second = world.player(b).orElseThrow();
        run(session, 10, Map.of(0, FORWARD));
        assertTrue(primary.getZ() > 16.5, "the primary used its own input");
        assertEquals(16.5, second.getZ(), "a missing input is neutral");
        double primaryZ = primary.getZ();
        run(session, 10, Map.of(b, FORWARD));
        assertEquals(primaryZ, primary.getZ(), "the primary has no input now");
        assertTrue(second.getZ() > 16.5);
    }

    @Test void sprintDrainsOnlyTheSprintingPlayersStamina() {
        GameSession session = quiet(world());
        int b = session.join(1);
        run(session, 10, Map.of(b, SPRINT));
        assertTrue(session.controls(b).staminaCurrent() < session.controls(b).staminaMaximum());
        assertTrue(session.controls(b).sprinting());
        assertEquals(session.controls(0).staminaMaximum(), session.controls(0).staminaCurrent());
        assertFalse(session.controls(0).sprinting());
    }

    @Test void unknownIdsInTheInputMapAreIgnored() {
        World world = world();
        GameSession session = quiet(world);
        Map<Integer, PlayerFrameInput> inputs = new HashMap<>();
        inputs.put(0, FORWARD);
        inputs.put(99, FORWARD);
        inputs.put(-5, FORWARD);
        assertDoesNotThrow(() -> run(session, 5, inputs));
        assertTrue(world.getPlayer().getZ() > 16.5);
        assertEquals(1, world.players().size(), "an unknown id must not create a player");
        Map<Integer, PlayerFrameInput> nullInput = new HashMap<>();
        nullInput.put(0, null);
        assertThrows(NullPointerException.class, () -> session.advanceAll(TICK, nullInput));
    }

    @Test void callerMapOrderDoesNotChangeTheResult() {
        String[] results = new String[2];
        for (int variant = 0; variant < 2; variant++) {
            World world = world();
            GameSession session = new GameSession(world, new GameSettings());
            int b = session.join(1);
            teleport(world.player(b).orElseThrow(), 20.5, 20.5);
            session.enemies().spawnAt(18.5, 24.5).orElseThrow();
            for (int tick = 0; tick < 100; tick++) {
                Map<Integer, PlayerFrameInput> inputs = variant == 0 ? new HashMap<>()
                        : new LinkedHashMap<>();
                if (variant == 0) {
                    inputs.put(0, scripted(tick));
                    inputs.put(b, scripted(tick + 7));
                } else {
                    inputs.put(b, scripted(tick + 7));
                    inputs.put(0, scripted(tick));
                }
                session.advanceAll(TICK, inputs);
            }
            results[variant] = snapshot(session, world);
        }
        assertEquals(results[0], results[1]);
    }

    @Test void aDeadPlayerIsSkippedAndTheOthersKeepRunning() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        session.life(0).die(PlayerLife.DeathCause.ENEMY);
        run(session, 10, Map.of(0, FORWARD, b, FORWARD));
        assertEquals(16.5, world.getPlayer().getZ(), "a dead player ignores its input");
        assertTrue(world.player(b).orElseThrow().getZ() > 16.5, "the living player keeps moving");
        assertEquals(GameSession.State.RUNNING, session.state());
        assertTrue(session.life(0).isDead());
        assertFalse(session.life(b).isDead());
        assertEquals(2, world.players().size(), "death must not remove a player");
    }

    @Test void sessionIsDeadOnlyWhenEveryPlayerIsDeadAndRespawnByIdResumesIt() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        session.life(0).die(PlayerLife.DeathCause.VOID);
        assertEquals(GameSession.State.RUNNING, session.state());
        session.life(b).die(PlayerLife.DeathCause.ENEMY);
        assertEquals(GameSession.State.DEAD, session.state());
        String frozen = snapshot(session, world);
        run(session, 10, Map.of(0, FORWARD, b, FORWARD));
        assertEquals(frozen, snapshot(session, world), "a dead session does not tick");
        session.respawn(b);
        assertEquals(GameSession.State.RUNNING, session.state());
        assertFalse(session.life(b).isDead());
        assertTrue(session.life(0).isDead(), "respawn(id) revives only that player");
        assertThrows(IllegalArgumentException.class, () -> session.respawn(99));
        assertThrows(IllegalArgumentException.class, () -> session.life(99));
        assertThrows(IllegalArgumentException.class, () -> session.controls(99));
        assertThrows(IllegalArgumentException.class, () -> session.pistol(99));
    }

    @Test void zombieTargetsTheNearestLivingPlayerAndTiesGoToTheEarliestJoined() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        int b = session.join(1);
        teleport(world.getPlayer(), 4.5, 4.5);
        teleport(world.player(b).orElseThrow(), 28.5, 4.5);
        session.enemies().spawnAt(22.5, 4.5).orElseThrow();
        run(session, 80, Map.of());
        assertTrue(session.life(b).health() < PlayerLife.MAX_HEALTH, "the nearer player was attacked");
        assertEquals(PlayerLife.MAX_HEALTH, session.life(0).health());

        World tieWorld = world();
        GameSession tie = new GameSession(tieWorld, new GameSettings());
        int second = tie.join(1);
        teleport(tieWorld.getPlayer(), 10.5, 10.5);
        teleport(tieWorld.player(second).orElseThrow(), 14.5, 10.5);
        Zombie zombie = tie.enemies().spawnAt(12.5, 10.5).orElseThrow();
        run(tie, 80, Map.of());
        assertTrue(zombie.getX() < 12.5 + 1e-9, "an equidistant zombie goes to the earliest player");
        assertTrue(tie.life(0).health() < PlayerLife.MAX_HEALTH);
        assertEquals(PlayerLife.MAX_HEALTH, tie.life(second).health());
    }

    @Test void zombiesIgnoreADeadPlayerAndChaseTheLivingOne() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        int b = session.join(1);
        teleport(world.getPlayer(), 12.5, 4.5);
        teleport(world.player(b).orElseThrow(), 28.5, 4.5);
        session.enemies().spawnAt(14.5, 4.5).orElseThrow();
        session.life(0).die(PlayerLife.DeathCause.ENEMY);
        run(session, 120, Map.of());
        assertTrue(session.life(b).health() < PlayerLife.MAX_HEALTH, "the living player was chased");
        assertTrue(session.life(0).isDead());
        assertEquals(0, session.life(0).health());
    }

    @Test void aPlayersPrimaryActionNeverFiresAnotherPlayersPistol() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        assertNotSame(session.pistol(0), session.pistol(b));
        session.pistol(0).update(PistolService.FIRST_WAVE, .01);
        var pickup = session.pistol(0).pickupPosition().orElseThrow();
        teleport(world.getPlayer(), pickup.x() + .5, pickup.z() + .5);
        world.getPlayer().setY(pickup.y());
        session.pistol(0).update(PistolService.FIRST_WAVE, .01);
        assertTrue(session.pistol(0).equipped());
        assertFalse(session.pistol(b).owned());
        PlayerFrameInput click = new PlayerFrameInput(new MovementInput(false, false, false, false),
                false, 0, 0, true, false, false, null);
        session.advanceAll(TICK, Map.of(b, click));
        assertEquals(0, session.pistol(0).cooldownSeconds(), "b's click must not fire a's gun");
        session.advanceAll(TICK, Map.of(0, click));
        assertTrue(session.pistol(0).cooldownSeconds() > 0, "a's own click fires a's gun");
        assertEquals(0, session.pistol(b).cooldownSeconds());
    }

    @Test void leaveRemovesAPlayerButTheLastStaysVacantAndTheNextJoinReclaimsItsId() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        session.leave(b);
        assertEquals(1, world.players().size());
        assertTrue(world.player(b).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> session.life(b));
        assertDoesNotThrow(() -> session.leave(99), "an unknown id is a no-op");
        session.life(0).die(PlayerLife.DeathCause.ENEMY);
        session.leave(0);
        assertEquals(1, world.players().size(), "the last body is kept");
        assertEquals(GameSession.State.PAUSED, session.state());
        assertEquals(0, session.join(2), "the vacant seat is reclaimed with its own id");
        assertEquals(GameSession.State.RUNNING, session.state());
        assertEquals(2, world.getPlayer().getSkinId());
        assertFalse(session.life(0).isDead(), "a dead body is respawned on reclaim");
        assertEquals(2, session.join(0), "ids already used by a leaver are never reused");
    }

    @Test void serverBootstrapLeaveOfPrimaryMakesTheSessionPausedAndTheFirstJoinGetsIdZero() {
        World world = world();
        GameSession session = quiet(world);
        session.leave(world.primaryPlayerId());
        assertEquals(GameSession.State.PAUSED, session.state());
        run(session, 10, Map.of(0, FORWARD));
        assertEquals(16.5, world.getPlayer().getZ(), "a vacant session does not tick");
        assertEquals(0, session.join(1));
        assertEquals(GameSession.State.RUNNING, session.state());
        assertEquals(1, session.join(2));
    }

    @Test void oldAccessorsLifeControlsPistolRespawnTargetThePrimary() {
        World world = world();
        GameSession session = quiet(world);
        int b = session.join(1);
        assertSame(session.life(0), session.life());
        assertSame(session.pistol(0), session.pistol());
        assertEquals(session.controls(0), session.controls());
        assertNotSame(session.life(b), session.life());
        session.life().die(PlayerLife.DeathCause.ENEMY);
        session.life(b).die(PlayerLife.DeathCause.ENEMY);
        session.respawn();
        assertFalse(session.life(0).isDead());
        assertTrue(session.life(b).isDead(), "respawn() revives the primary only");
    }

    @Test void savedWorldKeepsOnlyThePrimaryWithoutSkin() throws Exception {
        World world = world();
        GameSession session = quiet(world);
        session.join(2);
        world.getPlayer().setSkinId(1);
        String json = WorldJsonCodec.write(world);
        assertFalse(json.toLowerCase().contains("skin"), "JSON v1 has no skin field");
        World loaded = WorldJsonCodec.read(json, new BlockFactory());
        assertEquals(1, loaded.players().size());
        assertEquals(0, loaded.getPlayer().getSkinId());
        assertEquals(world.getPlayer().getX(), loaded.getPlayer().getX());
        assertEquals(world.getPlayer().getZ(), loaded.getPlayer().getZ());
    }
}
