package presentation.game;

import application.EnemyUpdateService;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import domain.enemy.Zombie;
import domain.enemy.ZombieState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Dibuja cada zombi como una caja verde del tamaño de su cuerpo. Al morir, la caja se hincha y
 * cambia a naranja durante el tiempo de despawn: una «explosión» puramente visual que no toca
 * bloques ni causa daño. Un único Model compartido; las instancias se descartan al desaparecer.
 */
public final class ZombieRenderer implements Disposable {
    private static final Color BODY = new Color(0.22f, 0.58f, 0.24f, 1f);
    private static final Color BURST = new Color(1f, 0.55f, 0.1f, 1f);
    private static final float BURST_GROWTH = 1.6f;

    private final Model model;
    private final Map<Zombie, ModelInstance> instances = new HashMap<>();

    public ZombieRenderer() {
        model = new ModelBuilder().createBox((float) Zombie.WIDTH, (float) Zombie.HEIGHT, (float) Zombie.WIDTH,
                new Material(ColorAttribute.createDiffuse(BODY)), Usage.Position | Usage.Normal);
    }

    public void render(ModelBatch batch, Environment environment, List<Zombie> zombies) {
        instances.keySet().retainAll(new HashSet<>(zombies));
        for (Zombie zombie : zombies) {
            ModelInstance instance = instances.computeIfAbsent(zombie, ignored -> new ModelInstance(model));
            float scale = 1f;
            Color color = BODY;
            if (zombie.getState() == ZombieState.DEAD) {
                float progress = (float) Math.min(1.0, zombie.getDeadSeconds() / EnemyUpdateService.DESPAWN_SECONDS);
                scale = 1f + BURST_GROWTH * progress;
                color = BURST;
            }
            instance.transform.setToTranslation((float) zombie.getX(),
                    (float) zombie.getY() + (float) Zombie.HEIGHT / 2f * scale, (float) zombie.getZ());
            instance.transform.scale(scale, scale, scale);
            instance.materials.get(0).set(ColorAttribute.createDiffuse(color));
            batch.render(instance, environment);
        }
    }

    int instanceCount() {
        return instances.size();
    }

    @Override
    public void dispose() {
        instances.clear();
        model.dispose();
    }
}
