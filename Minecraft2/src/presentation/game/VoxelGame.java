package presentation.game;

import application.PlayerInteractionService;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.Position;
import domain.player.Player;
import domain.world.BlockChange;
import domain.world.Chunk;
import domain.world.World;
import patterns.observer.Observer;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Vista 3D del mundo y bucle de juego.
 *
 * <p>Es presentación: dibuja y recoge la entrada, pero no contiene reglas. El movimiento, la
 * física, la colisión y la interacción los resuelven los servicios del dominio y de aplicación
 * a través de {@link GameInput}.
 *
 * <p>Implementa {@link Observer} de {@link BlockChange}: cuando el jugador coloca o elimina un
 * bloque, {@link World} ya emite la notificación y aquí solo se marca ese chunk para reconstruir
 * su malla. Es el uso que justifica el patrón Observer en el proyecto.
 */
public final class VoxelGame extends ApplicationAdapter implements Observer<BlockChange> {
    private static final float FIELD_OF_VIEW = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 300f;
    private static final Color SKY = new Color(0.45f, 0.68f, 0.92f, 1f);

    private final World world;
    private final PlayerInteractionService interactionService;

    /** Captura automática para la evidencia de CT-10; desactivada si no se pide. */
    private final String screenshotPath;
    private final int screenshotFrame;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private BlockTextureAtlas textureAtlas;
    private ChunkMeshBuilder meshBuilder;
    private GameInput input;

    private final Map<Chunk, ChunkMeshBuilder.ChunkMesh> meshes = new LinkedHashMap<>();
    /** Instancias dibujables, cacheadas: crearlas por fotograma sería trabajo tirado. */
    private final Map<Chunk, ModelInstance> instances = new LinkedHashMap<>();
    private final Set<Chunk> dirtyChunks = new LinkedHashSet<>();

    private SpriteBatch hudBatch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private int frame;

    public VoxelGame(World world, PlayerInteractionService interactionService) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
        this.screenshotPath = System.getProperty("mc2.screenshot");
        this.screenshotFrame = Integer.getInteger("mc2.screenshot.frame", 5);
    }

    @Override
    public void create() {
        camera = new PerspectiveCamera(FIELD_OF_VIEW, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = NEAR_PLANE;
        camera.far = FAR_PLANE;

        environment = new Environment();
        // Solo luz ambiental blanca: el relieve ya viene del sombreado por cara de
        // BlockAppearance, así los colores salen tal como se diseñaron.
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));

        modelBatch = new ModelBatch();
        textureAtlas = new BlockTextureAtlas();
        meshBuilder = new ChunkMeshBuilder(world, textureAtlas);
        input = new GameInput(interactionService);

        hudBatch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();

        buildAllMeshes();
        world.addObserver(this);
        Gdx.input.setCursorCatched(true);
    }

    private void buildAllMeshes() {
        long start = System.nanoTime();
        int totalFaces = 0;
        int totalBlocks = 0;

        for (Chunk chunk : world.getChunks()) {
            ChunkMeshBuilder.ChunkMesh mesh = meshBuilder.build(chunk);
            meshes.put(chunk, mesh);
            instances.put(chunk, new ModelInstance(mesh.model()));
            totalFaces += mesh.faceCount();
            totalBlocks += mesh.blockCount();
        }

        long millis = (System.nanoTime() - start) / 1_000_000;
        int theoretical = totalBlocks * 6;
        System.out.printf(
                "[render] %d chunks, %d bloques, %d caras dibujadas de %d posibles (%.1f%%), en %d ms%n",
                world.getChunks().size(), totalBlocks, totalFaces, theoretical,
                theoretical == 0 ? 0f : (100f * totalFaces / theoretical), millis);
    }

    /** Llega desde World al colocar o eliminar un bloque: marca el chunk afectado. */
    @Override
    public void update(BlockChange change) {
        world.findChunk(change.block().getPosition()).ifPresent(dirtyChunks::add);
    }

    /** Chunks pendientes de reconstruir. Permite comprobar el cableado del Observer sin gráficos. */
    int pendingRebuildCount() {
        return dirtyChunks.size();
    }

    @Override
    public void render() {
        Player player = world.getPlayer();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
            Gdx.app.exit();
            return;
        }

        input.update(player, world, Gdx.graphics.getDeltaTime());
        rebuildDirtyChunks();
        updateCamera(player);

        ScreenUtils.clear(SKY, true);
        modelBatch.begin(camera);
        for (ModelInstance instance : instances.values()) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();

        drawHud(player);

        frame++;
        if (screenshotPath != null && frame == screenshotFrame) {
            captureScreenshot();
            Gdx.app.exit();
        }
    }

    private void rebuildDirtyChunks() {
        if (dirtyChunks.isEmpty()) {
            return;
        }
        for (Chunk chunk : dirtyChunks) {
            ChunkMeshBuilder.ChunkMesh previous = meshes.remove(chunk);
            if (previous != null) {
                previous.model().dispose();
            }
            ChunkMeshBuilder.ChunkMesh rebuilt = meshBuilder.build(chunk);
            meshes.put(chunk, rebuilt);
            instances.put(chunk, new ModelInstance(rebuilt.model()));
        }
        dirtyChunks.clear();
    }

    /**
     * La cámara va en el ojo del jugador y usa exactamente la misma convención de yaw y pitch
     * que {@code Player.forwardVector()} y {@code PlayerInteractionService}. Si se desviara,
     * el jugador caminaría o apuntaría hacia un sitio distinto del que ve.
     */
    private void updateCamera(Player player) {
        double yaw = Math.toRadians(player.getYaw());
        double pitch = Math.toRadians(player.getPitch());

        camera.position.set(
                (float) player.getX(),
                (float) (player.getY() + Player.EYE_HEIGHT),
                (float) player.getZ());
        camera.direction.set(
                (float) (-Math.sin(yaw) * Math.cos(pitch)),
                (float) Math.sin(pitch),
                (float) (Math.cos(yaw) * Math.cos(pitch)));
        camera.up.set(0f, 1f, 0f);
        camera.update();
    }

    private void drawHud(Player player) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // Punto de mira: sin él no se sabe a qué bloque se está apuntando.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.line(width / 2f - 8f, height / 2f, width / 2f + 8f, height / 2f);
        shapeRenderer.line(width / 2f, height / 2f - 8f, width / 2f, height / 2f + 8f);
        shapeRenderer.end();

        Position block = player.toPosition();
        hudBatch.begin();
        font.draw(hudBatch, "Mundo: " + world.getName(), 12f, height - 12f);
        font.draw(hudBatch, String.format("Posicion: %d, %d, %d", block.x(), block.y(), block.z()),
                12f, height - 32f);
        font.draw(hudBatch, "Bloque a colocar: " + input.getSelectedType() + "   (teclas 1-7)",
                12f, height - 52f);
        font.draw(hudBatch, "WASD mover · espacio saltar · clic izq. eliminar · clic der. colocar · ESC salir",
                12f, 22f);
        hudBatch.end();
    }

    private void captureScreenshot() {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        flipVertically(pixmap, width, height);

        PixmapIO.writePNG(Gdx.files.absolute(screenshotPath), pixmap);
        pixmap.dispose();
        System.out.println("[render] captura guardada en " + screenshotPath);
    }

    /**
     * OpenGL entrega el framebuffer con el origen abajo a la izquierda, así que el PNG saldría
     * boca abajo. Se invierte fila por fila antes de guardarlo.
     */
    private static void flipVertically(Pixmap pixmap, int width, int height) {
        ByteBuffer pixels = pixmap.getPixels();
        int bytesPerLine = width * 4;
        byte[] flipped = new byte[bytesPerLine * height];

        for (int line = 0; line < height; line++) {
            pixels.position((height - line - 1) * bytesPerLine);
            pixels.get(flipped, line * bytesPerLine, bytesPerLine);
        }
        pixels.clear();
        pixels.put(flipped);
        pixels.clear();
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null && width > 0 && height > 0) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
    }

    @Override
    public void dispose() {
        world.removeObserver(this);
        meshes.values().forEach(mesh -> mesh.model().dispose());
        meshes.clear();
        instances.clear();

        disposeQuietly(modelBatch);
        disposeQuietly(hudBatch);
        disposeQuietly(font);
        disposeQuietly(shapeRenderer);
        disposeQuietly(textureAtlas);
    }

    private static void disposeQuietly(com.badlogic.gdx.utils.Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
