package presentation.game;

import application.PlayerInteractionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameWindowTextureModeTest {
    @Test
    void alternaEntreTexturasYColoresPlanos() {
        GameWindow window = new GameWindow(new PlayerInteractionService());

        assertTrue(window.texturesEnabled());
        assertFalse(window.toggleTextures());
        assertTrue(window.toggleTextures());
    }
}
