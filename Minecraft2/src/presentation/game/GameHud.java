package presentation.game;

import application.GameSession;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import domain.Position;
import domain.world.World;

/** HUD y overlays de la partida; proyección en píxeles actualizada al cambiar tamaño. */
public final class GameHud implements Disposable {
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont title = new BitmapFont();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final GlyphLayout layout = new GlyphLayout();
    private final DeathMessages deathMessages = new DeathMessages();
    public GameHud() {
        title.getData().setScale(3f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        Matrix4 projection = new Matrix4().setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(projection);
        shapes.setProjectionMatrix(projection);
    }
    public void draw(World world, GameInput input, GameSession session, RenderDistance distance, int meshes, boolean managedByShell) {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        boolean running = session.state() == GameSession.State.RUNNING;
        String deathMessage = deathMessages.observe(session.life().deathCause());
        if (running && session.life().hitFlashSeconds() > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            float alpha = .28f * (float) (session.life().hitFlashSeconds()
                    / domain.player.PlayerLife.HIT_FLASH_SECONDS);
            shapes.setColor(1, .03f, .02f, alpha);
            shapes.rect(0, 0, w, h);
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(w / 2 - 100, h - 24, 200, 12);
        shapes.setColor(input.stamina().exhausted() ? Color.ORANGE : Color.GREEN);
        shapes.rect(w / 2 - 100, h - 24, 200 * (float) input.stamina().fraction(), 12);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(w / 2 - 100, h - 43, 200, 8);
        shapes.setColor(.88f, .16f, .18f, 1);
        shapes.rect(w / 2 - 100, h - 43, 200 * (float) session.life().healthFraction(), 8);
        shapes.end();
        if (running) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.WHITE);
            shapes.line(w / 2 - 8, h / 2, w / 2 + 8, h / 2);
            shapes.line(w / 2, h / 2 - 8, w / 2, h / 2 + 8);
            shapes.end();
        }
        Position p = world.getPlayer().toPosition();
        batch.begin();
        font.draw(batch, "Mundo: " + world.getName(), 12, h - 12);
        font.draw(batch, "Posicion: " + p.x() + ", " + p.y() + ", " + p.z(), 12, h - 32);
        font.draw(batch, "Bloque: " + input.getSelectedType() + " (1-7)", 12, h - 52);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond() + " | Radio: " + distance.radius()
                + " (J/K) | Mallas: " + meshes, 12, h - 72);
        font.draw(batch, "Zombis: " + session.enemies().zombies().size() + " | " + session.difficulty(), 12, h - 92);
        font.draw(batch, "Oleadas: " + session.waveSummary(), 12, h - 112);
        if (session.pistol().owned()) {
            font.draw(batch, session.pistol().equipped()
                    ? "Pistola | Clic disparar | Q guardar | 1-7 bloques"
                    : "Q equipar pistola", 12, 43);
        } else if (session.pistol().appeared()) {
            font.draw(batch, "Hay una pistola en el mundo. Búscala.", 12, 43);
        }
        font.draw(batch, "WASD | Shift correr | Espacio saltar | Clic golpear/bloques | F pantalla completa | ESC pausa", 12, 22);
        batch.end();
        if (session.state() == GameSession.State.DEAD) overlay(deathMessage, "R para reaparecer | ESC menu", Color.RED);
        else if (session.isPaused() && !managedByShell) overlay("PAUSA", "ESC para continuar", Color.WHITE);
    }
    private void overlay(String heading, String text, Color color) {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, .65f); shapes.rect(0, 0, w, h); shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        title.setColor(color);
        layout.setText(title, heading, color, Math.max(100, w - 48), com.badlogic.gdx.utils.Align.center, true);
        batch.begin(); title.draw(batch, layout, 24, h * .68f);
        font.draw(batch, text, w / 2 - 100, h * .68f - 45); batch.end();
    }
    public void dispose() { batch.dispose(); font.dispose(); title.dispose(); shapes.dispose(); }
}
