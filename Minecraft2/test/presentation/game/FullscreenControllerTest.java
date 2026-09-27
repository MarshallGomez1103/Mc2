package presentation.game;

import com.badlogic.gdx.Graphics;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class FullscreenControllerTest {
    @Test void roundTripPreservesWindowSizeAfterFullscreenResize() {
        State state = new State();
        FullscreenController controller = new FullscreenController();
        assertTrue(controller.toggle(state.graphics()));
        state.width = 1920; state.height = 1080;
        assertTrue(controller.toggle(state.graphics()));
        assertEquals(960, state.restoredWidth);
        assertEquals(540, state.restoredHeight);
    }
    @Test void unsupportedModeMakesNoChanges() {
        State state = new State(); state.supported = false;
        assertFalse(new FullscreenController().toggle(state.graphics()));
        assertEquals(0, state.switches);
    }
    @Test void failedFullscreenDoesNotChangeModeAndRetryRetainsNewWindowSize() {
        State state = new State(); state.success = false;
        FullscreenController controller = new FullscreenController();
        assertFalse(controller.toggle(state.graphics()));
        assertFalse(state.fullscreen);
        state.success = true; state.width = 800; state.height = 600;
        assertTrue(controller.toggle(state.graphics()));
        assertTrue(controller.toggle(state.graphics()));
        assertEquals(800, state.restoredWidth); assertEquals(600, state.restoredHeight);
    }
    private static class State {
        boolean supported = true, success = true, fullscreen;
        int width = 960, height = 540, restoredWidth, restoredHeight, switches;
        Graphics graphics() {
            return (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(),
                    new Class<?>[]{Graphics.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "supportsDisplayModeChange" -> supported;
                        case "isFullscreen" -> fullscreen;
                        case "getWidth" -> width;
                        case "getHeight" -> height;
                        case "getMonitor", "getDisplayMode" -> null;
                        case "setFullscreenMode" -> { switches++; if (success) fullscreen = true; yield success; }
                        case "setWindowedMode" -> {
                            switches++; restoredWidth = (int) args[0]; restoredHeight = (int) args[1];
                            if (success) fullscreen = false; yield success;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
