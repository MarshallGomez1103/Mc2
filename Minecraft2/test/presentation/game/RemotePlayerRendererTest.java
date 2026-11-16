package presentation.game;

import network.ClientProtocol.PlayerView;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemotePlayerRendererTest {
    private static PlayerView player(long id, int skin) {
        return new PlayerView(id, "p" + id, skin, 1, 2, 3, 45, 0, 100, false);
    }
    @Test void excludesLocalPlayerAndRetainsIdentityAndSkin() {
        var visible = RemotePlayerRenderer.visiblePlayers(List.of(player(3, 0), player(8, 2)), 3);
        assertEquals(List.of(player(8, 2)), visible);
        assertThrows(UnsupportedOperationException.class, visible::clear);
    }
    @Test void threeSkinsAreDistinctAndInvalidSkinIsRejected() {
        assertNotEquals(RemotePlayerRenderer.skinColor(0), RemotePlayerRenderer.skinColor(1));
        assertNotEquals(RemotePlayerRenderer.skinColor(1), RemotePlayerRenderer.skinColor(2));
        assertNotEquals(RemotePlayerRenderer.skinColor(0), RemotePlayerRenderer.skinColor(2));
        assertThrows(IllegalArgumentException.class, () -> RemotePlayerRenderer.skinColor(3));
    }
    @Test void departedPlayersDisappearOnNextSnapshot() {
        assertTrue(RemotePlayerRenderer.visiblePlayers(List.of(player(3, 0)), 3).isEmpty());
    }
}
