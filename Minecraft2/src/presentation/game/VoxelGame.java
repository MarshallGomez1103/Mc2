package presentation.game;

import application.EnemyUpdateService;
import application.PlayerInteractionService;
import application.ZombieMeleeService;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.Position;
import domain.enemy.ZombieParameters;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.BlockChange;
import domain.world.Chunk;
import domain.world.World;
import patterns.observer.Observer;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
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
    private static final int MESHES_PER_FRAME = 2;
    private static final Color SKY = new Color(0.45f, 0.68f, 0.92f, 1f);
    /** Mismo tope que GameInput: un tirón de la ventana no debe teletransportar a los zombis. */
    private static final float MAX_ENEMY_DELTA_SECONDS = 0.05f;
    /** Distancia delante del jugador a la que la tecla Z genera un zombi de prueba. */
    private static final double DEBUG_SPAWN_DISTANCE = 6.0;

    private final World world;
    private final PlayerInteractionService interactionService;
    private final boolean texturesEnabled;

    /** Captura automática para la evidencia de CT-10; desactivada si no se pide. */
    private final String screenshotPath;
    private final int screenshotFrame;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private BlockTextureAtlas textureAtlas;
    private ChunkMeshBuilder meshBuilder;
    private GameInput input;
    private RenderDistance renderDistance;
    private PlayerLife playerLife;
    private EnemyUpdateService enemyService;
    private ZombieRenderer zombieRenderer;
    private final Vector3 chunkCenter = new Vector3();
    private final Vector3 chunkDimensions = new Vector3(Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH);

    private final Map<Chunk, ChunkMeshBuilder.ChunkMesh> meshes = new LinkedHashMap<>();
    /** Instancias dibujables, cacheadas: crearlas por fotograma sería trabajo tirado. */
    private final Map<Chunk, ModelInstance> instances = new LinkedHashMap<>();
    private final Set<Chunk> dirtyChunks = new LinkedHashSet<>();

    private SpriteBatch hudBatch;
    private BitmapFont font;
    private BitmapFont deathFont;
    private final GlyphLayout deathText = new GlyphLayout();
    private ShapeRenderer shapeRenderer;
    private int frame;

    public VoxelGame(World world, PlayerInteractionService interactionService) {
        this(world, interactionService, true);
    }

    public VoxelGame(World world, PlayerInteractionService interactionService, boolean texturesEnabled) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
        this.texturesEnabled = texturesEnabled;
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
        meshBuilder = new ChunkMeshBuilder(world, textureAtlas, texturesEnabled);
        enemyService = new EnemyUpdateService(world, ZombieParameters.defaults());
        zombieRenderer = new ZombieRenderer();
        input = new GameInput(interactionService, new ZombieMeleeService(world), enemyService);
        renderDistance = RenderDistance.forWorld(world);
        playerLife = new PlayerLife(world.getPlayer());

        hudBatch = new SpriteBatch();
        font = new BitmapFont();
        deathFont = new BitmapFont();
        deathFont.getData().setScale(4f);
        deathFont.setColor(Color.RED);
        shapeRenderer = new ShapeRenderer();

        world.addObserver(this);
        Gdx.input.setCursorCatched(true);
        spawnEvidenceZombies(Integer.getInteger("mc2.zombies", 0));
    }

    /** Solo para capturas de evidencia: coloca zombis delante del jugador sin pulsar Z. */
    private void spawnEvidenceZombies(int count) {
        Player player = world.getPlayer();
        double[] forward = player.forwardVector();
        double[] right = player.rightVector();
        for (int i = 0; i < count; i++) {
            double side = (i - (count - 1) / 2.0) * 2.0;
            enemyService.spawnAt(player.getX() + forward[0] * DEBUG_SPAWN_DISTANCE + right[0] * side,
                    player.getZ() + forward[1] * DEBUG_SPAWN_DISTANCE + right[1] * side);
        }
    }

    /** Llega desde World al colocar o eliminar un bloque: marca el chunk afectado. */
    @Override
    public void update(BlockChange change) {
        Position position = change.block().getPosition();
        int chunkX = Chunk.chunkXFor(position);
        int chunkZ = Chunk.chunkZFor(position);
        markDirty(chunkX, chunkZ);
        // Una cara del chunk vecino puede quedar expuesta al cambiar un bloque del borde.
        if (Math.floorMod(position.x(), Chunk.WIDTH) == 0) {
            markDirty(chunkX - 1, chunkZ);
        } else if (Math.floorMod(position.x(), Chunk.WIDTH) == Chunk.WIDTH - 1) {
            markDirty(chunkX + 1, chunkZ);
        }
        if (Math.floorMod(position.z(), Chunk.DEPTH) == 0) {
            markDirty(chunkX, chunkZ - 1);
        } else if (Math.floorMod(position.z(), Chunk.DEPTH) == Chunk.DEPTH - 1) {
            markDirty(chunkX, chunkZ + 1);
        }
    }

    private void markDirty(int chunkX, int chunkZ) {
        world.findChunk(chunkX, chunkZ).ifPresent(dirtyChunks::add);
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            renderDistance.decrease();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            renderDistance.increase();
        }
        if (playerLife.isDead()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                playerLife.respawn();
            }
        } else {
            input.update(player, world, Gdx.graphics.getDeltaTime());
            playerLife.update();
            // Provisional hasta que HordeManager (Estudiante 3) genere oleadas: Z crea un zombi delante.
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                double[] forward = player.forwardVector();
                enemyService.spawnAt(player.getX() + forward[0] * DEBUG_SPAWN_DISTANCE,
                        player.getZ() + forward[1] * DEBUG_SPAWN_DISTANCE);
            }
            enemyService.update(playerLife, Math.min(Gdx.graphics.getDeltaTime(), MAX_ENEMY_DELTA_SECONDS));
        }
        updateCamera(player);
        refreshVisibleMeshes(player);

        ScreenUtils.clear(SKY, true);
        modelBatch.begin(camera);
        for (Map.Entry<Chunk, ModelInstance> entry : instances.entrySet()) {
            Chunk chunk = entry.getKey();
            chunkCenter.set((chunk.getChunkX() + 0.5f) * Chunk.WIDTH,
                    Chunk.HEIGHT / 2f, (chunk.getChunkZ() + 0.5f) * Chunk.DEPTH);
            if (camera.frustum.boundsInFrustum(chunkCenter, chunkDimensions)) {
                modelBatch.render(entry.getValue(), environment);
            }
        }
        zombieRenderer.render(modelBatch, environment, enemyService.zombies());
        modelBatch.end();

        drawHud(player);
        if (playerLife.isDead()) {
            drawDeathOverlay();
        }

        frame++;
        if (screenshotPath != null && frame == screenshotFrame) {
            captureScreenshot();
            Gdx.app.exit();
        }
    }

    /** Solo mantiene mallas alrededor del jugador; el World conserva sus bloques para guardar. */
    private void refreshVisibleMeshes(Player player) {
        int playerChunkX = Math.floorDiv((int) Math.floor(player.getX()), Chunk.WIDTH);
        int playerChunkZ = Math.floorDiv((int) Math.floor(player.getZ()), Chunk.DEPTH);

        Iterator<Map.Entry<Chunk, ChunkMeshBuilder.ChunkMesh>> iterator = meshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Chunk, ChunkMeshBuilder.ChunkMesh> entry = iterator.next();
            if (!renderDistance.contains(playerChunkX, playerChunkZ, entry.getKey())) {
                entry.getValue().model().dispose();
                instances.remove(entry.getKey());
                dirtyChunks.remove(entry.getKey());
                iterator.remove();
            }
        }

        int budget = MESHES_PER_FRAME;
        for (Chunk chunk : Set.copyOf(dirtyChunks)) {
            if (budget == 0) {
                break;
            }
            if (renderDistance.contains(playerChunkX, playerChunkZ, chunk) && meshes.containsKey(chunk)) {
                meshes.remove(chunk).model().dispose();
                buildMesh(chunk);
                dirtyChunks.remove(chunk);
                budget--;
            }
        }

        // Centro primero, luego anillos: al aumentar la distancia no se bloquea un frame.
        for (int ring = 0; ring <= renderDistance.radius() && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    Chunk chunk = world.findChunk(playerChunkX + dx, playerChunkZ + dz).orElse(null);
                    if (chunk != null && !meshes.containsKey(chunk)) {
                        buildMesh(chunk);
                        dirtyChunks.remove(chunk);
                        budget--;
                    }
                }
            }
        }
        // Un cambio fuera del radio se reconstruirá al volver a crear su malla.
        dirtyChunks.removeIf(chunk -> !meshes.containsKey(chunk)
                && !renderDistance.contains(playerChunkX, playerChunkZ, chunk));
    }

    private void buildMesh(Chunk chunk) {
        ChunkMeshBuilder.ChunkMesh mesh = meshBuilder.build(chunk);
        meshes.put(chunk, mesh);
        instances.put(chunk, new ModelInstance(mesh.model()));
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
        font.draw(hudBatch, "FPS: " + Gdx.graphics.getFramesPerSecond()
                        + "   Distancia: " + renderDistance.radius() + " chunks (J-/K+)"
                        + "   Mallas: " + meshes.size(), 12f, height - 72f);
        font.draw(hudBatch, "Zombis: " + enemyService.zombies().size() + "   (Z genera uno delante)",
                12f, height - 92f);
        font.draw(hudBatch, "WASD mover · Shift correr · espacio saltar · clic izq. golpear/eliminar · clic der. colocar · ESC salir",
                12f, 22f);
        hudBatch.end();
    }

    private void drawDeathOverlay() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(0, 0, width, height);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        deathText.setText(deathFont, "MORISTE");
        hudBatch.begin();
        deathFont.draw(hudBatch, deathText, (width - deathText.width) / 2f,
                (height + deathText.height) / 2f);
        font.draw(hudBatch, "R para reaparecer  ·  ESC para salir", width / 2f - 125f,
                height / 2f - 48f);
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
        // Volver al menú después de morir no debe dejar una posición bajo el vacío guardable.
        if (playerLife != null && playerLife.isDead()) {
            playerLife.respawn();
        }
        world.removeObserver(this);
        meshes.values().forEach(mesh -> mesh.model().dispose());
        meshes.clear();
        instances.clear();

        disposeQuietly(modelBatch);
        disposeQuietly(hudBatch);
        disposeQuietly(font);
        disposeQuietly(deathFont);
        disposeQuietly(shapeRenderer);
        disposeQuietly(textureAtlas);
        disposeQuietly(zombieRenderer);
    }

    private static void disposeQuietly(com.badlogic.gdx.utils.Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
