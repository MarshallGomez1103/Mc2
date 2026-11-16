package presentation.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import network.ClientProtocol.PlayerView;
import java.util.*;

/** Draws immutable network players. Owns GPU resources; call only on the render thread. */
public final class RemotePlayerRenderer implements Disposable {
    private final Model[] skins = new Model[3];
    private final Map<Long, Visual> instances = new HashMap<>();
    public RemotePlayerRenderer() {
        for (int i = 0; i < skins.length; i++) skins[i] = model(i);
    }
    /** RGB variants follow the wire skinId 0..2; no simulation objects are accessed. */
    static int skinColor(int skinId) {
        return switch (skinId) {
            case 0 -> 0x4285F4FF;
            case 1 -> 0xE95A47FF;
            case 2 -> 0x55B875FF;
            default -> throw new IllegalArgumentException("skinId must be 0..2");
        };
    }
    static List<PlayerView> visiblePlayers(List<PlayerView> players, long localPlayerId) {
        return players.stream().filter(p -> p.playerId() != localPlayerId).toList();
    }
    private static Model model(int skin) {
        ModelBuilder builder = new ModelBuilder(); builder.begin();
        part(builder, "head", 0, 1.55f, 0, .40f, .40f, .40f, new Color(0xE8BA91FF));
        part(builder, "body", 0, 1.02f, 0, .48f, .66f, .28f, new Color(skinColor(skin)));
        part(builder, "leftArm", -.34f, 1.02f, 0, .18f, .65f, .22f, new Color(skinColor(skin)));
        part(builder, "rightArm", .34f, 1.02f, 0, .18f, .65f, .22f, new Color(skinColor(skin)));
        part(builder, "leftLeg", -.13f, .35f, 0, .22f, .70f, .25f, new Color(0x30384CFF));
        part(builder, "rightLeg", .13f, .35f, 0, .22f, .70f, .25f, new Color(0x30384CFF));
        return builder.end();
    }
    private static void part(ModelBuilder builder, String id, float x, float y, float z,
                             float width, float height, float depth, Color color) {
        builder.node().id = id;
        builder.part(id, GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(color))).box(x, y, z, width, height, depth);
    }
    public void render(ModelBatch batch, Environment environment, List<PlayerView> players, long localPlayerId) {
        List<PlayerView> visible = visiblePlayers(players, localPlayerId);
        Set<Long> active = new HashSet<>();
        for (PlayerView player : visible) {
            active.add(player.playerId());
            Visual visual = instances.get(player.playerId());
            if (visual == null || visual.skin != player.skinId()) {
                visual = new Visual(player.skinId(), new ModelInstance(skins[player.skinId()]));
                instances.put(player.playerId(), visual);
            }
            visual.instance.transform.setToTranslation((float)player.x(), (float)player.y(), (float)player.z())
                    .rotate(Vector3.Y, player.yaw());
            if (!player.dead()) batch.render(visual.instance, environment);
        }
        instances.keySet().retainAll(active);
    }
    @Override public void dispose() {
        instances.clear(); for (Model skin : skins) skin.dispose();
    }
    private record Visual(int skin, ModelInstance instance) { }
}
