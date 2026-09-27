package presentation.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.enemy.Zombie;
import java.util.List;

/** Real OpenGL visual capture harness; does not claim a human playtest. */
public final class ZombieVisualSmoke extends ApplicationAdapter {
    private static final String[] VIEWS={"front","back","side","top","walking","textures-off"};
    private final String output;
    private ZombieRenderer renderer;
    private ModelBatch batch;
    private PerspectiveCamera camera;
    private final Zombie zombie=new Zombie(0,0,0,3);
    private Environment environment;
    private int frame;
    public ZombieVisualSmoke(String output) { this.output=output; }
    public static void main(String[] args) {
        var config=new Lwjgl3ApplicationConfiguration();config.setTitle("Zombie UV visual validation");
        config.setWindowedMode(600,600);config.setForegroundFPS(60);config.useVsync(true);
        new Lwjgl3Application(new ZombieVisualSmoke(args[0]),config);
    }
    @Override public void create() {
        renderer=new ZombieRenderer(true);batch=new ModelBatch();camera=new PerspectiveCamera(48,600,600);
        camera.near=.1f;camera.far=30;
        environment=new Environment();environment.set(new ColorAttribute(ColorAttribute.AmbientLight,.95f,.95f,.95f,1));
        System.out.println("Zombie visual GL: "+Gdx.gl.glGetString(GL20.GL_RENDERER));
    }
    @Override public void render() {
        int view=frame/30;
        if(view>=VIEWS.length) {Gdx.app.exit();return;}
        Gdx.gl.glClearColor(.22f,.25f,.30f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        camera.up.set(0,1,0);
        if(view==1) camera.position.set(0,1.3f,-3.2f);
        else if(view==2) camera.position.set(3.2f,1.3f,0);
        else if(view==3) {camera.position.set(0,4,.5f);camera.up.set(0,0,-1);}
        else camera.position.set(0,1.3f,3.2f);
        camera.lookAt(0,.9f,0);camera.update();
        if(view==4) zombie.setPosition(0,0,zombie.getZ()+.025);
        else zombie.setPosition(0,0,0);
        renderer.setTexturesEnabled(view!=5);
        batch.begin(camera);renderer.render(batch,environment,List.of(zombie));batch.end();
        if(frame%30==29) {
            var pixmap=ScreenUtils.getFrameBufferPixmap(0,0,600,600);
            try {PixmapIO.writePNG(Gdx.files.absolute(output+"/zombie-"+VIEWS[view]+".png"),pixmap,-1,true);}
            finally {pixmap.dispose();}
        }
        frame++;
    }
    @Override public void dispose() {renderer.dispose();batch.dispose();}
}
