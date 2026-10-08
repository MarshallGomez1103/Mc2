package presentation.game;

import application.GameSession;
import application.GameSettings;
import domain.enemy.Difficulty;
import application.PlayerInteractionService;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.Position;
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
 * a través de {@link GameSession}; {@link GameInput} solo produce los datos de entrada.
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
    /** Tope visual del efecto de cámara; GameSession limita el paso de simulación. */
    private static final float MAX_SIMULATION_DELTA_SECONDS = 0.05f;
    private final World world;
    private final PlayerInteractionService interactionService;
    private final boolean texturesEnabled;
    private final GameSettings gameSettings;

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
    private GameSession session;
    private SprintCameraEffect sprintEffect;
    private GameHud hud;
    private int skipInputFrames;
    private final boolean managedByShell;
    private final FullscreenController fullscreen = new FullscreenController();
    private ZombieRenderer zombieRenderer;
    private PistolRenderer pistolRenderer;
    private final Vector3 chunkCenter = new Vector3();
    private final Vector3 chunkDimensions = new Vector3(Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH);

    private final Map<Chunk, ChunkMeshBuilder.ChunkMesh> meshes = new LinkedHashMap<>();
    /** Instancias dibujables, cacheadas: crearlas por fotograma sería trabajo tirado. */
    private final Map<Chunk, ModelInstance> instances = new LinkedHashMap<>();
    private final Set<Chunk> dirtyChunks = new LinkedHashSet<>();

    private int frame;

    public VoxelGame(World world, PlayerInteractionService interactionService) {
        this(world, interactionService, true, new GameSettings());
    }

    public VoxelGame(World world, PlayerInteractionService interactionService, boolean texturesEnabled) {
        this(world, interactionService, texturesEnabled, new GameSettings());
    }

    public VoxelGame(World world, PlayerInteractionService interactionService,
                     boolean texturesEnabled, GameSettings gameSettings) {
        this(world, interactionService, texturesEnabled, gameSettings, false);
    }

    public VoxelGame(World world, PlayerInteractionService interactionService,
                     boolean texturesEnabled, GameSettings gameSettings, boolean managedByShell) {
        this.managedByShell = managedByShell;
        this.world = Objects.requireNonNull(world, "world no puede ser null");
        this.interactionService = Objects.requireNonNull(interactionService,
                "interactionService no puede ser null");
        this.texturesEnabled = texturesEnabled;
        this.gameSettings = Objects.requireNonNull(gameSettings, "gameSettings no puede ser null");
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
        session = new GameSession(world, gameSettings, interactionService);
        zombieRenderer = new ZombieRenderer(gameSettings.enemyTexturesEnabled());
        pistolRenderer = new PistolRenderer();
        input = new GameInput();
        sprintEffect = new SprintCameraEffect();
        renderDistance = RenderDistance.forWorld(world);
        playerLife = session.life();
        hud = new GameHud();

        world.addObserver(this);
        Gdx.input.setCursorCatched(true);
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

        if (!managedByShell) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) setPaused(!isPaused());
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) fullscreen.toggle();
        }
        if (session.state() == GameSession.State.RUNNING) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) renderDistance.decrease();
            if (Gdx.input.isKeyJustPressed(Input.Keys.K)) renderDistance.increase();
            if (skipInputFrames > 0) {
                skipInputFrames--;
            } else {
                float delta = Math.min(Gdx.graphics.getDeltaTime(), MAX_SIMULATION_DELTA_SECONDS);
                session.advance(delta, input.read());
                sprintEffect.update(delta, session.controls().sprinting(), session.controls().moving(), player.isOnGround());
            }
        } else if (session.state() == GameSession.State.DEAD
                && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            respawn();
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
        zombieRenderer.render(modelBatch, environment, session.enemies().zombies(), session.enemies().debris());
        pistolRenderer.renderPickup(modelBatch, environment, session.pistol());
        modelBatch.end();

        if (session.pistol().equipped() && !session.life().isDead()) {
            Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
            modelBatch.begin(camera);
            pistolRenderer.renderHeld(modelBatch, environment, camera, session.pistol());
            modelBatch.end();
        }
        hud.draw(world, session, renderDistance, meshes.size(), managedByShell);

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
                (float) (player.getY() + Player.EYE_HEIGHT + sprintEffect.verticalOffset()),
                (float) player.getZ());
        camera.direction.set(
                (float) (-Math.sin(yaw) * Math.cos(pitch)),
                (float) Math.sin(pitch),
                (float) (Math.cos(yaw) * Math.cos(pitch)));
        camera.fieldOfView = FIELD_OF_VIEW + (float) sprintEffect.fovOffset();
        camera.up.set(0f, 1f, 0f);
        camera.update();
    }

    public void setPaused(boolean value) {
        session.setPaused(value);
        Gdx.input.setCursorCatched(!value);
        if (!value) skipInputFrames = 1;
    }
    public boolean isPaused() { return session.isPaused(); }
    public GameSession.State sessionState() { return session.state(); }
    public Difficulty runtimeDifficulty() { return session.difficulty(); }
    public boolean texturesEnabled() { return texturesEnabled; }
    public String statusText() { return session.waveSummary(); }
    public void applySettings() {
        session.applySettings(gameSettings);
        zombieRenderer.setTexturesEnabled(gameSettings.enemyTexturesEnabled());
    }
    public void respawn() {
        session.respawn();
        sprintEffect.reset();
        skipInputFrames = 1;
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
            if (hud != null) hud.resize(width, height);
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
        disposeQuietly(hud);
        disposeQuietly(textureAtlas);
        disposeQuietly(zombieRenderer);
        disposeQuietly(pistolRenderer);
    }

    private static void disposeQuietly(com.badlogic.gdx.utils.Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
