package presentation.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

/** Conserva el tamaño de ventana y el cursor al cambiar de monitor/modo. */
public final class FullscreenController {
    private int width = 1280;
    private int height = 720;

    public boolean toggle() {
        boolean captured = Gdx.input.isCursorCatched();
        try {
            return toggle(Gdx.graphics);
        } finally {
            Gdx.input.setCursorCatched(captured);
        }
    }

    /** API separada del input para verificar cambios fallidos y retorno a ventana. */
    public boolean toggle(Graphics graphics) {
        if (!graphics.supportsDisplayModeChange()) return false;
        if (graphics.isFullscreen()) return graphics.setWindowedMode(width, height);
        int previousWidth = graphics.getWidth();
        int previousHeight = graphics.getHeight();
        boolean changed = graphics.setFullscreenMode(graphics.getDisplayMode(graphics.getMonitor()));
        if (changed) {
            width = previousWidth;
            height = previousHeight;
        }
        return changed;
    }
}
