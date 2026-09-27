package presentation.game;

import application.ChunkGenerationService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Paisaje temporal del menú: no crea partidas, guardados, jugadores activos ni enemigos. */
final class MenuWorldBackdrop implements Disposable {
    private static final int CHUNKS_PER_SIDE = 4;
    private static final float CENTER = CHUNKS_PER_SIDE * Chunk.WIDTH / 2f;
    private static final float ORBIT_RADIUS = CENTER - 8f;
    private final List<ModelInstance> scenery = new ArrayList<>();
    private final Matrix4 overlayProjection = new Matrix4();
    private PerspectiveCamera camera;
    private ModelBatch batch;
    private BlockTextureAtlas atlas;
    private ShapeRenderer shade;
    private Environment environment;
    private float targetHeight;
    private float cameraHeight;
    private float angle;

    MenuWorldBackdrop() {
        try {
            long seed = ThreadLocalRandom.current().nextLong();
            SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(seed);
            World preview = new World("menu_preview", seed, Instant.now());
            new ChunkGenerationService(terrain, new BlockFactory())
                    .generateChunks(CHUNKS_PER_SIDE, CHUNKS_PER_SIDE).forEach(preview::addChunk);
            targetHeight = terrain.surfaceHeightAt((int) CENTER, (int) CENTER) + 3f;
            int highest = 0;
            for (int x = 0; x < CHUNKS_PER_SIDE * Chunk.WIDTH; x++) {
                for (int z = 0; z < CHUNKS_PER_SIDE * Chunk.DEPTH; z++) {
                    highest = Math.max(highest, terrain.surfaceHeightAt(x, z));
                }
            }
            cameraHeight = highest + 8f;
            angle = ThreadLocalRandom.current().nextFloat() * (float) (2 * Math.PI);
            camera = new PerspectiveCamera(65f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.near = .1f;
            camera.far = 140f;
            environment = new Environment();
            environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
            batch = new ModelBatch();
            atlas = new BlockTextureAtlas();
            shade = new ShapeRenderer();
            ChunkMeshBuilder meshes = new ChunkMeshBuilder(preview, atlas, true);
            for (Chunk chunk : preview.getChunks()) {
                scenery.add(new ModelInstance(meshes.build(chunk).model()));
            }
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } catch (RuntimeException failure) {
            try { dispose(); }
            catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }

    void render(float deltaSeconds) {
        angle += Math.max(0f, Math.min(deltaSeconds, .1f)) * .025f;
        camera.position.set(CENTER + (float) Math.cos(angle) * ORBIT_RADIUS,
                cameraHeight, CENTER + (float) Math.sin(angle) * ORBIT_RADIUS);
        camera.up.set(0, 1, 0);
        camera.lookAt(CENTER, targetHeight, CENTER);
        camera.update();
        ScreenUtils.clear(.45f, .68f, .92f, 1f, true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        batch.begin(camera);
        for (ModelInstance instance : scenery) batch.render(instance, environment);
        batch.end();
        // Un velo ligero conserva el paisaje y facilita leer las letras sin paneles opacos.
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shade.setProjectionMatrix(overlayProjection);
        shade.begin(ShapeRenderer.ShapeType.Filled);
        shade.setColor(0f, 0f, 0f, .32f);
        shade.rect(0, 0, camera.viewportWidth, camera.viewportHeight);
        shade.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        overlayProjection.setToOrtho2D(0, 0, width, height);
    }

    @Override public void dispose() {
        for (ModelInstance instance : scenery) instance.model.dispose();
        scenery.clear();
        if (batch != null) { batch.dispose(); batch = null; }
        if (atlas != null) { atlas.dispose(); atlas = null; }
        if (shade != null) { shade.dispose(); shade = null; }
    }
}
