package presentation.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/** Información del contexto efectivo; consultar después de crear OpenGL. */
public record GraphicsDiagnostics(String vendor, String renderer, String version, String backend) {
    public static GraphicsDiagnostics capture() {
        return new GraphicsDiagnostics(Gdx.gl.glGetString(GL20.GL_VENDOR),
                Gdx.gl.glGetString(GL20.GL_RENDERER), Gdx.gl.glGetString(GL20.GL_VERSION),
                Gdx.graphics.getType().name());
    }

    /** Friendly category; raw device details remain exclusively in terminal diagnostics. */
    public String deviceLabel() {
        String name = (vendor + " " + renderer).toLowerCase(java.util.Locale.ROOT);
        if (name.contains("intel")) return "Integrada";
        if (name.contains("nvidia")) return "Dedicada";
        // Unknown hardware remains explicitly unclassified instead of guessing from a model name.
        return "Automática";
    }

    public String summary() {
        return "GL_VENDOR: " + vendor + "\nGL_RENDERER: " + renderer
                + "\nGL_VERSION: " + version + "\nBackend: " + backend;
    }
}
