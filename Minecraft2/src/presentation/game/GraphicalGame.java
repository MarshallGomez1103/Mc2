package presentation.game;

import application.PlayerInteractionService;
import application.WorldApplicationService;
import application.WorldSize;
import bootstrap.GpuPreference;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import domain.world.World;

import java.nio.file.Path;
import java.util.List;

/** Shell de pantallas y ciclo de vida; CRUD y simulación se delegan a sus servicios. */
public final class GraphicalGame extends ApplicationAdapter {
    private final WorldApplicationService worlds;
    private final GameWindow window;
    private final PlayerInteractionService interaction;
    private final Path storageDirectory;
    private final FullscreenController fullscreen = new FullscreenController();
    private Stage stage;
    private Skin skin;
    private Table panel;
    private Label message;
    private VoxelGame game;
    private MenuWorldBackdrop menuBackdrop;
    private GraphicsDiagnostics diagnostics;
    private String view = "MAIN";
    private int frame;
    private boolean deferResume;

    public GraphicalGame(WorldApplicationService worlds, GameWindow window,
                         PlayerInteractionService interaction, Path storageDirectory) {
        this.worlds = worlds;
        this.window = window;
        this.interaction = interaction;
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();
    }

    @Override public void create() {
        diagnostics = GraphicsDiagnostics.capture();
        System.out.println(diagnostics.summary());
        System.out.println("Guardados: " + storageDirectory);
        skin = MenuSkin.create();
        stage = new Stage(new ScreenViewport());
        menuBackdrop = new MenuWorldBackdrop();
        showMain();
        String autoWorld = System.getProperty("mc2.play.world");
        if (autoWorld != null) operation(() -> { worlds.loadWorld(autoWorld); startWorld(); });
    }

    @Override public void render() {
        if (deferResume) {
            deferResume = false;
            game.setPaused(false);
            Gdx.input.setInputProcessor(null);
            stage.clear();
            view = "PLAY";
        }
        if (!(stage.getKeyboardFocus() instanceof TextField)
                && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (!fullscreen.toggle()) error("No se pudo cambiar el modo de pantalla.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (game != null && !game.isPaused()) showPause();
            else if (game != null && view.equals("PAUSE")) resumeGame();
            else if (game != null) showPause();
            else showMain();
        }
        if (game != null) game.render();
        else menuBackdrop.render(Gdx.graphics.getDeltaTime());
        if (!view.equals("PLAY")) {
            Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);
            stage.act(Math.min(Gdx.graphics.getDeltaTime(), .1f));
            stage.draw();
        }
        captureIfRequested();
    }

    private void page(String title) {
        deferResume = false;
        view = title;
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(stage);
        stage.clear();
        Table root = new Table();
        root.setFillParent(true);
        panel = new Table(skin);
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(26);
        panel.defaults().width(contentWidth()).pad(5).height(38);
        ScrollPane pageScroll = new ScrollPane(panel, skin);
        pageScroll.setScrollingDisabled(true, false);
        pageScroll.setFadeScrollBars(false);
        root.add(pageScroll).grow().pad(12);
        stage.addActor(root);
        Label header = new Label(title, skin);
        header.setFontScale(1.5f);
        panel.add(header).height(48).row();
        message = new Label("", skin);
        message.setWrap(true);
        panel.add(message).height(48).row();
    }

    private void button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        panel.add(button).row();
    }

    private void text(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        panel.add(label).height(text.split("\\n", -1).length > 2 ? 82 : 50).row();
    }

    private void showMain() {
        page("Minecraft 2");
        view = "MAIN";
        button("Jugar / Mundos", this::showWorlds);
        button("Crear mundo", this::showCreate);
        button("Opciones", this::showOptions);
        button("Salir", () -> Gdx.app.exit());
        text("F: pantalla completa | WASD: mover | Shift: correr | ESC: pausa");
        text("Gráfica: " + diagnostics.deviceLabel());
    }

    private void showWorlds() {
        page("Mundos guardados");
        operation(() -> {
            List<String> ids = worlds.listWorlds();
            if (ids.isEmpty()) text("No hay mundos guardados en esta carpeta.");
            else {
                com.badlogic.gdx.scenes.scene2d.ui.List<String> list =
                        new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
                list.setItems(ids.toArray(String[]::new));
                ScrollPane scroll = new ScrollPane(list, skin);
                scroll.setFadeScrollBars(false);
                panel.add(scroll).height(180).row();
                button("Cargar y jugar", () -> operation(() -> {
                    worlds.loadWorld(list.getSelected());
                    startWorld();
                }));
                button("Eliminar seleccionado", () -> confirmDelete(list.getSelected()));
            }
        });
        text("Carpeta: " + storageDirectory);
        button("Crear mundo", this::showCreate);
        button("Volver", this::showMain);
    }

    private void confirmDelete(String id) {
        page("Confirmar eliminación");
        text("Eliminar definitivamente el mundo «" + id + "» y su archivo JSON.");
        button("Eliminar", () -> operation(() -> { worlds.deleteWorld(id); showWorlds(); }));
        button("Cancelar", this::showWorlds);
    }

    private void showCreate() {
        page("Crear mundo");
        TextField name = new TextField("", skin);
        name.setMessageText("Nombre del mundo");
        name.setMaxLength(80);
        panel.add(name).row();
        SelectBox<String> size = new SelectBox<>(skin);
        size.setItems("Pequeño (4 chunks)", "Mediano (100 chunks)", "Grande (256 chunks)");
        panel.add(size).row();
        text("Grande requiere más memoria; se recomienda iniciar Java con -Xmx2g.");
        button("Crear y jugar", () -> operation(() -> {
            worlds.createWorld(name.getText(), WorldSize.values()[size.getSelectedIndex()]);
            startWorld();
        }));
        button("Cancelar", this::showWorlds);
        stage.setKeyboardFocus(name);
    }

    private void startWorld() {
        World world = worlds.currentWorld().orElseThrow(() -> new IllegalStateException("Sin mundo cargado"));
        if (game != null) { game.dispose(); game = null; }
        VoxelGame candidate = new VoxelGame(world, interaction, window.texturesEnabled(),
                window.settings(), true);
        try {
            candidate.create();
            candidate.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            candidate.setPaused(false);
        } catch (RuntimeException failure) {
            try { candidate.dispose(); }
            catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            showMain();
            throw failure;
        }
        game = candidate;
        Gdx.input.setInputProcessor(null);
        stage.clear();
        view = "PLAY";
    }

    private void showPause() {
        if (game == null) { showMain(); return; }
        game.setPaused(true);
        page("Partida en pausa");
        view = "PAUSE";
        text("Mundo: " + worlds.currentWorldName().orElse("?")
                + " | Dificultad activa: " + game.runtimeDifficulty());
        button("Continuar", this::resumeGame);
        button("Guardar", () -> operation(() -> {
            worlds.saveCurrentWorld();
            message.setColor(Color.GREEN);
            message.setText("Guardado en " + storageDirectory);
        }));
        button("Opciones", this::showOptions);
        button("Salir al menú", () -> confirmExit(false));
        button("Cerrar juego", () -> confirmExit(true));
    }

    private void resumeGame() {
        // El clic de Continuar se consume en UI; la simulación vuelve en el siguiente frame.
        deferResume = true;
    }

    private void showOptions() {
        if (game != null) game.setPaused(true);
        page("Opciones");
        text("Enemigos y su textura se aplican ahora. Dificultad y bloques: próxima partida.");
        button("Enemigos: " + onOff(window.settings().enemiesEnabled()), () -> {
            window.settings().toggleEnemies(); applySettings(); showOptions();
        });
        button("Texturas de zombis: " + onOff(window.settings().enemyTexturesEnabled()), () -> {
            window.settings().toggleEnemyTextures(); applySettings(); showOptions();
        });
        button("Texturas de bloques: " + onOff(window.texturesEnabled()) + " (próxima partida)", () -> {
            window.toggleTextures(); showOptions();
        });
        button("Dificultad: " + window.settings().difficulty() + " (próxima partida)", () -> {
            window.settings().cycleDifficulty(); showOptions();
        });
        button("Pantalla completa: F", () -> {
            if (!fullscreen.toggle()) error("Este sistema no permite cambiar el modo de pantalla.");
        });
        button("Gráficos / GPU", this::showGraphics);
        button("Volver", game == null ? this::showMain : this::showPause);
    }

    private void applySettings() { if (game != null) game.applySettings(); }
    private static String onOff(boolean enabled) { return enabled ? "ON" : "OFF"; }

    private void showGraphics() {
        page("Tarjeta gráfica");
        text("En uso: " + diagnostics.deviceLabel());
        text("Elegida: " + GpuPreference.label(GpuPreference.readSaved(storageDirectory)));
        text("Elige una y vuelve a abrir el juego para aplicarla.");
        if (GpuPreference.integratedAvailable()) {
            button("Integrada · menor consumo", () -> chooseGraphics(GpuPreference.Mode.INTEGRATED));
        }
        if (GpuPreference.dedicatedAvailable()) {
            button("Dedicada · mayor rendimiento", () -> chooseGraphics(GpuPreference.Mode.DEDICATED));
        }
        button("Automática", () -> chooseGraphics(GpuPreference.Mode.AUTO));
        button("Volver", this::showOptions);
    }

    private void chooseGraphics(GpuPreference.Mode mode) {
        operation(() -> { GpuPreference.save(storageDirectory, mode); showGraphics(); });
    }

    public void requestClose() {
        if (game == null) Gdx.app.exit();
        else confirmExit(true);
    }

    public void focusLost() { if (game != null && !game.isPaused()) showPause(); }

    private void confirmExit(boolean closeApplication) {
        if (game == null) { if (closeApplication) Gdx.app.exit(); else showMain(); return; }
        game.setPaused(true);
        page("Salir de la partida");
        text("Los cambios desde el último guardado solo se conservan si guarda.");
        button("Guardar y salir", () -> operation(() -> {
            worlds.saveCurrentWorld(); leaveGame(closeApplication);
        }));
        button("Salir sin guardar", () -> leaveGame(closeApplication));
        button("Cancelar", this::showPause);
    }

    private void leaveGame(boolean closeApplication) {
        game.dispose();
        game = null;
        deferResume = false;
        if (closeApplication) Gdx.app.exit(); else showMain();
    }

    private void error(String text) {
        System.err.println(text);
        if (message != null) { message.setColor(Color.SALMON); message.setText(text); }
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private void operation(CheckedAction action) {
        try { action.run(); }
        catch (Exception failure) {
            error(failure.getClass().getSimpleName() + ": " + failure.getMessage()
                    + " | Guardados: " + storageDirectory);
        }
    }

    private void captureIfRequested() {
        frame++;
        String path = System.getProperty("mc2.screenshot");
        if (game == null && path != null && frame == Integer.getInteger("mc2.screenshot.frame", 5)) {
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(),
                    Gdx.graphics.getBackBufferHeight());
            try { PixmapIO.writePNG(Gdx.files.absolute(path), pixels, -1, true); }
            finally { pixels.dispose(); }
            Gdx.app.exit();
        }
    }

    private float contentWidth() {
        return Math.max(160, Math.min(520, Gdx.graphics.getWidth() - 80f));
    }

    @Override public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
        if (panel != null) {
            for (Cell<?> cell : panel.getCells()) cell.width(contentWidth());
            panel.invalidateHierarchy();
        }
        if (game != null) game.resize(width, height);
        if (menuBackdrop != null) menuBackdrop.resize(width, height);
    }
    @Override public void pause() { focusLost(); }
    @Override public void dispose() {
        if (game != null) game.dispose();
        if (menuBackdrop != null) menuBackdrop.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
