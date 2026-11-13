package domain.world;

import domain.player.Player;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldMultiPlayerTest {
    private static World world() {
        return new World("multi", 7, Instant.parse("2026-09-21T12:00:00Z"));
    }

    private static Player at(double x) {
        return new Player(x, 21, 0);
    }

    @Test void newWorldHasPrimaryWithIdZeroReturnedByGetPlayer() {
        World world = world();
        assertEquals(0, world.primaryPlayerId());
        assertSame(world.getPlayer(), world.player(0).orElseThrow());
        assertEquals(1, world.players().size());
    }

    @Test void addPlayerAssignsIncreasingIdsInJoinOrder() {
        World world = world();
        Player a = at(1), b = at(2);
        assertEquals(1, world.addPlayer(a));
        assertEquals(2, world.addPlayer(b));
        assertEquals(List.of(0, 1, 2), List.copyOf(world.players().keySet()));
        assertSame(a, world.players().get(1));
        assertSame(b, world.players().get(2));
    }

    @Test void playerByIdFindsAddedPlayerAndUnknownIdIsEmpty() {
        World world = world();
        Player a = at(1);
        int id = world.addPlayer(a);
        assertSame(a, world.player(id).orElseThrow());
        assertTrue(world.player(99).isEmpty());
        assertTrue(world.player(-1).isEmpty());
    }

    @Test void removedIdsAreNotReused() {
        World world = world();
        int first = world.addPlayer(at(1));
        assertTrue(world.removePlayer(first));
        assertTrue(world.player(first).isEmpty());
        assertEquals(first + 1, world.addPlayer(at(2)));
        assertFalse(world.removePlayer(99), "unknown id must report false and change nothing");
        assertEquals(2, world.players().size());
    }

    @Test void removingThePrimaryPromotesTheNextPlayerToGetPlayer() {
        World world = world();
        Player second = at(1);
        int id = world.addPlayer(second);
        assertTrue(world.removePlayer(world.primaryPlayerId()));
        assertSame(second, world.getPlayer());
        assertEquals(id, world.primaryPlayerId());
    }

    @Test void removingTheLastPlayerIsRejected() {
        World world = world();
        Player primary = world.getPlayer();
        assertThrows(IllegalStateException.class, () -> world.removePlayer(0));
        assertSame(primary, world.getPlayer());
        assertEquals(1, world.players().size());
    }

    @Test void addingNullOrTheSameInstanceTwiceIsRejected() {
        World world = world();
        Player a = at(1);
        assertThrows(NullPointerException.class, () -> world.addPlayer(null));
        world.addPlayer(a);
        assertThrows(IllegalArgumentException.class, () -> world.addPlayer(a));
        assertThrows(IllegalArgumentException.class, () -> world.addPlayer(world.getPlayer()));
        assertEquals(2, world.players().size());
    }

    @Test void playersViewIsUnmodifiableAndInJoinOrder() {
        World world = world();
        Player a = at(1), b = at(2);
        world.addPlayer(a);
        world.addPlayer(b);
        assertThrows(UnsupportedOperationException.class, () -> world.players().put(9, at(3)));
        assertThrows(UnsupportedOperationException.class, () -> world.players().remove(0));
        assertEquals(List.of(world.getPlayer(), a, b), List.copyOf(world.players().values()));
    }

    @Test void skinIdDefaultsToZero() {
        assertEquals(0, at(1).getSkinId());
        assertEquals(0, world().getPlayer().getSkinId());
    }

    @Test void skinIdAcceptsZeroToTwoAndRejectsMinusOneAndThree() {
        Player player = at(1);
        for (int skin = 0; skin <= Player.MAX_SKIN_ID; skin++) {
            player.setSkinId(skin);
            assertEquals(skin, player.getSkinId());
        }
        player.setSkinId(1);
        assertThrows(IllegalArgumentException.class, () -> player.setSkinId(-1));
        assertThrows(IllegalArgumentException.class, () -> player.setSkinId(Player.MAX_SKIN_ID + 1));
        assertEquals(1, player.getSkinId(), "a rejected skin must not change the player");
    }
}
