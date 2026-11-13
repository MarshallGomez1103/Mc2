package application;

import domain.block.BlockType;
import domain.enemy.TestWorlds;
import domain.player.MovementInput;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.World;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A server adapter drives GameSession with plain data only: a fixed delta per tick, a scripted
 * PlayerFrameInput and public accessors to publish state. No LibGDX type is involved anywhere in
 * this class. Kept out of the failsafe list on purpose so it runs in the unit dataset.
 */
class GameSessionHeadlessTest {
    private static final double TICK = 1.0 / 20;
    private static final int TICKS = 1600;

    private static World world() {
        World world = TestWorlds.flat(4, 4);
        world.getPlayer().setX(32.5);
        world.getPlayer().setY(21);
        world.getPlayer().setZ(32.5);
        return world;
    }

    /** Deterministic input script that moves, looks around, jumps, shoots and swaps material. */
    private static PlayerFrameInput scripted(int tick) {
        boolean forward = tick % 40 < 30;
        boolean right = tick % 90 > 60;
        return new PlayerFrameInput(new MovementInput(forward, false, false, right, tick % 200 < 60),
                tick % 37 == 0, (tick % 11) - 5, (tick % 7) - 3, tick % 13 == 0, tick % 29 == 0,
                tick % 150 == 0, tick % 300 == 0 ? BlockType.WOOD : null);
    }

    /** Everything a server publishes each tick, as comparable text. */
    private static String snapshot(GameSession session, World world) {
        Player player = world.getPlayer();
        StringBuilder out = new StringBuilder();
        out.append(session.state()).append('|')
                .append(player.getX()).append(',').append(player.getY()).append(',')
                .append(player.getZ()).append('|')
                .append(session.life().health()).append('|')
                .append(session.controls()).append('|')
                .append(session.hordes().phase()).append(session.hordes().wave()).append('/')
                .append(session.hordes().remainingInWave()).append('/')
                .append(session.hordes().secondsToNextWave()).append("|pistol=")
                .append(session.pistol().pickupPosition()).append(session.pistol().owned())
                .append(session.pistol().equipped()).append("|zombies=")
                .append(session.enemies().zombies().size());
        session.enemies().zombies().forEach(z -> out.append(';').append(z.getX()).append(',')
                .append(z.getZ()).append(',').append(z.getState()).append(',').append(z.getHealth()));
        return out.toString();
    }

    private static List<String> run(int ticks, boolean script) {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        List<String> trace = new ArrayList<>();
        for (int tick = 0; tick < ticks; tick++) {
            session.advance(TICK, script ? scripted(tick) : PlayerFrameInput.NEUTRAL);
            trace.add(snapshot(session, world));
        }
        return trace;
    }

    @Test void sameWorldAndSameScriptedInputsGiveTheSameStateOnEveryTick() {
        List<String> first = run(TICKS, true);
        List<String> second = run(TICKS, true);
        assertEquals(first, second);
        // The run is not trivial: a wave arrived and the player actually moved.
        assertTrue(first.stream().anyMatch(s -> !s.contains("zombies=0")), "no zombie appeared");
        assertNotEquals(first.get(0), first.get(TICKS - 1));
    }

    @Test void pistolPickupSpawnsAtTheSamePlaceForTheSameWorld() {
        World first = world();
        World second = world();
        GameSession a = new GameSession(first, new GameSettings());
        GameSession b = new GameSession(second, new GameSettings());
        for (int tick = 0; tick < 5; tick++) {
            a.pistol().update(PistolService.FIRST_WAVE, TICK);
            b.pistol().update(PistolService.FIRST_WAVE, TICK);
        }
        assertTrue(a.pistol().appeared(), "the pistol never appeared");
        assertEquals(a.pistol().pickupPosition(), b.pistol().pickupPosition());
    }

    @Test void differentInputsGiveADifferentState() {
        assertNotEquals(run(200, true).get(199), run(200, false).get(199));
    }

    @Test void aFixedTwentyHertzTickIsNotTruncatedByTheSimulationCap() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        PlayerFrameInput sprint = new PlayerFrameInput(new MovementInput(true, false, false, false, true),
                false, 0, 0, false, false, false, null);
        double countdown = session.hordes().secondsToNextWave();
        session.advance(TICK, sprint);
        assertEquals(32.5 + Player.MOVE_SPEED * 1.65 * TICK, world.getPlayer().getZ(), 1e-9);
        assertEquals(100 - 10 * TICK, session.controls().staminaCurrent(), 1e-9);
        assertEquals(countdown - TICK, session.hordes().secondsToNextWave(), 1e-9);
    }

    @Test void aSlowLoopIsCappedToOneTwentiethOfASecond() {
        World capped = world();
        World fixed = world();
        GameSession slow = new GameSession(capped, new GameSettings());
        GameSession tick = new GameSession(fixed, new GameSettings());
        slow.advance(2.0, scripted(1));
        tick.advance(TICK, scripted(1));
        assertEquals(snapshot(tick, fixed), snapshot(slow, capped));
    }

    @Test void publishedStateReadsThroughPublicAccessors() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        session.enemies().spawnAt(40.5, 40.5).orElseThrow();
        session.advance(TICK, PlayerFrameInput.NEUTRAL);
        assertEquals(GameSession.State.RUNNING, session.state());
        assertEquals(PlayerLife.MAX_HEALTH, session.life().health());
        assertEquals(32.5, world.getPlayer().getX(), 1e-9);
        assertEquals(1, session.enemies().zombies().size());
        assertEquals(0, session.hordes().wave());
        session.life().die(PlayerLife.DeathCause.ENEMY);
        assertEquals(GameSession.State.DEAD, session.state());
    }

    @Test void pausedAndDeadSessionsIgnoreEveryTickEvenWithoutInput() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        String before = snapshot(session, world);
        session.setPaused(true);
        for (int tick = 0; tick < 100; tick++) {
            assertDoesNotThrow(() -> session.advance(TICK, null));
        }
        assertEquals(GameSession.State.PAUSED, session.state());
        assertEquals(before.replace("RUNNING", "PAUSED"), snapshot(session, world));
        session.setPaused(false);
        session.life().die(PlayerLife.DeathCause.VOID);
        String dead = snapshot(session, world);
        for (int tick = 0; tick < 100; tick++) {
            session.advance(TICK, scripted(tick));
        }
        assertEquals(dead, snapshot(session, world));
    }

    @Test void invalidDeltasLeaveTheStateUntouched() {
        World world = world();
        GameSession session = new GameSession(world, new GameSettings());
        String before = snapshot(session, world);
        for (double delta : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -TICK, 0}) {
            session.advance(delta, scripted(1));
        }
        assertEquals(before, snapshot(session, world));
    }
}
