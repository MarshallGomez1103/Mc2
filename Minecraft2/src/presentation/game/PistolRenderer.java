package presentation.game;

import application.PistolService;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** Pistola de cinco piezas, compartida por la recogida del suelo y la vista del jugador. */
public final class PistolRenderer implements Disposable {
    private final Model pistolModel;
    private final Model flashModel;
    private final ModelInstance pickup;
    private final ModelInstance held;
    private final ModelInstance flash;
    private final Vector3 right = new Vector3();
    private final Vector3 position = new Vector3();
    private final Quaternion orientation = new Quaternion();

    public PistolRenderer() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        box(builder, "grip", new Color(.20f, .13f, .07f, 1), 0, .14f, -.12f, .16f, .28f, .18f);
        box(builder, "body", new Color(.21f, .24f, .27f, 1), 0, .34f, .03f, .20f, .16f, .58f);
        box(builder, "barrel", new Color(.06f, .07f, .08f, 1), 0, .34f, .38f, .14f, .10f, .20f);
        box(builder, "trigger", new Color(.10f, .11f, .12f, 1), 0, .21f, .02f, .06f, .08f, .06f);
        box(builder, "sight", Color.LIGHT_GRAY, 0, .45f, .27f, .05f, .07f, .05f);
        pistolModel = builder.end();
        flashModel = new ModelBuilder().createBox(.14f, .14f, .18f,
                new Material(ColorAttribute.createDiffuse(Color.YELLOW),
                        ColorAttribute.createEmissive(1f, .75f, .15f, 1f)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        pickup = new ModelInstance(pistolModel);
        held = new ModelInstance(pistolModel);
        flash = new ModelInstance(flashModel);
    }

    private static void box(ModelBuilder builder, String name, Color color,
                            float x, float y, float z, float width, float height, float depth) {
        builder.part(name, GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                new Material(ColorAttribute.createDiffuse(color))).box(x, y, z, width, height, depth);
    }

    public void renderPickup(ModelBatch batch, Environment environment, PistolService pistol) {
        pistol.pickupPosition().ifPresent(cell -> {
            pickup.transform.setToTranslation(cell.x() + .5f, cell.y() + .06f, cell.z() + .5f)
                    .rotate(Vector3.Y, 35).scale(1.25f, 1.25f, 1.25f);
            batch.render(pickup, environment);
        });
    }

    /** Se dibuja después del terreno para que el arma cercana no desaparezca dentro de paredes. */
    public void renderHeld(ModelBatch batch, Environment environment, PerspectiveCamera camera, PistolService pistol) {
        if (!pistol.equipped()) return;
        right.set(camera.direction).crs(camera.up).nor();
        position.set(camera.position).mulAdd(camera.direction, .68f)
                .mulAdd(right, .27f).mulAdd(camera.up, -.40f);
        float yaw = (float) Math.toDegrees(Math.atan2(camera.direction.x, camera.direction.z));
        float pitch = -(float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, camera.direction.y))));
        orientation.setEulerAngles(yaw, pitch, 0);
        held.transform.set(position, orientation).scale(.65f, .65f, .65f);
        batch.render(held, environment);
        if (pistol.shotFlashSeconds() > 0) {
            flash.transform.set(held.transform).translate(0, .34f, .58f);
            batch.render(flash, environment);
        }
    }

    @Override public void dispose() { pistolModel.dispose(); flashModel.dispose(); }
}
