package presentation.game;

import application.EnemyUpdateService;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import domain.enemy.Zombie;
import domain.enemy.ZombieState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Six articulated parts, unique face UVs, shared resources and movement-driven visual gait. */
public final class ZombieRenderer implements Disposable {
    private static final Color BODY = new Color(.22f,.58f,.24f,1);
    private static final Color BURST = new Color(1,.55f,.1f,1);
    private static final ZombiePose.Part[] PARTS = ZombiePose.Part.values();
    private final com.badlogic.gdx.math.Quaternion partRotation = new com.badlogic.gdx.math.Quaternion();
    private final Model model;
    private final Texture skinTexture;
    private final Map<Zombie, Visual> instances = new HashMap<>();
    private boolean texturesEnabled;

    public ZombieRenderer() { this(true); }
    public ZombieRenderer(boolean texturesEnabled) {
        this.texturesEnabled=texturesEnabled;
        skinTexture=createSkinTexture();
        model=createModel(skinTexture);
    }
    public void setTexturesEnabled(boolean enabled) { texturesEnabled=enabled; }
    public boolean texturesEnabled() { return texturesEnabled; }

    private static Texture createSkinTexture() {
        Pixmap pixmap=new Pixmap(ZombieAtlas.WIDTH,ZombieAtlas.HEIGHT,Pixmap.Format.RGBA8888);
        try {
            int[][] pixels=ZombieAtlas.pixels();
            for(int y=0;y<ZombieAtlas.HEIGHT;y++) for(int x=0;x<ZombieAtlas.WIDTH;x++) pixmap.drawPixel(x,y,pixels[y][x]);
            Texture texture=new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            return texture;
        } finally { pixmap.dispose(); }
    }
    private static Model createModel(Texture texture) {
        ModelBuilder builder=new ModelBuilder(); builder.begin();
        addPart(builder,texture,"head",0,1.575f,0,.45f,.45f,.45f,0,0,0);
        addPart(builder,texture,"torso",0,1.025f,0,.46f,.65f,.28f,1,0,0);
        // Arms pivot at shoulders and extend forward (+Z), legs pivot at hips.
        addPart(builder,texture,"leftArm",-.33f,1.27f,0,.20f,.20f,.65f,3,0,.27f);
        addPart(builder,texture,"rightArm",.33f,1.27f,0,.20f,.20f,.65f,3,0,.27f);
        addPart(builder,texture,"leftLeg",-.12f,.70f,0,.22f,.70f,.25f,2,-.35f,0);
        addPart(builder,texture,"rightLeg",.12f,.70f,0,.22f,.70f,.25f,2,-.35f,0);
        return builder.end();
    }
    private static void addPart(ModelBuilder builder, Texture texture, String id, float px,float py,float pz,
                                float width,float height,float depth,int part,float offsetY,float offsetZ) {
        var node=builder.node();node.id=id;node.translation.set(px,py,pz);
        Material material=new Material(ColorAttribute.createDiffuse(Color.WHITE),TextureAttribute.createDiffuse(texture));
        MeshPartBuilder mesh=builder.part(id,GL20.GL_TRIANGLES,Usage.Position|Usage.Normal|Usage.TextureCoordinates,material);
        float x=width/2,y=height/2,z=depth/2;
        // CCW vertex order seen from outside. Each face has explicit top and bottom UVs.
        face(mesh,part,0,new float[][]{{-x,-y,z},{x,-y,z},{x,y,z},{-x,y,z}},0,0,1,offsetY,offsetZ);
        face(mesh,part,1,new float[][]{{x,-y,-z},{-x,-y,-z},{-x,y,-z},{x,y,-z}},0,0,-1,offsetY,offsetZ);
        face(mesh,part,2,new float[][]{{x,-y,z},{x,-y,-z},{x,y,-z},{x,y,z}},1,0,0,offsetY,offsetZ);
        face(mesh,part,3,new float[][]{{-x,-y,-z},{-x,-y,z},{-x,y,z},{-x,y,-z}},-1,0,0,offsetY,offsetZ);
        face(mesh,part,4,new float[][]{{-x,y,z},{x,y,z},{x,y,-z},{-x,y,-z}},0,1,0,offsetY,offsetZ);
        face(mesh,part,5,new float[][]{{-x,-y,-z},{x,-y,-z},{x,-y,z},{-x,-y,z}},0,-1,0,offsetY,offsetZ);
    }
    private static void face(MeshPartBuilder mesh,int part,int face,float[][] points,float nx,float ny,float nz,float oy,float oz) {
        float[] uv=ZombieAtlas.uv(part,face);
        var a=vertex(points[0],nx,ny,nz,uv[0],uv[3],oy,oz);
        var b=vertex(points[1],nx,ny,nz,uv[2],uv[3],oy,oz);
        var c=vertex(points[2],nx,ny,nz,uv[2],uv[1],oy,oz);
        var d=vertex(points[3],nx,ny,nz,uv[0],uv[1],oy,oz);
        mesh.rect(a,b,c,d);
    }
    private static MeshPartBuilder.VertexInfo vertex(float[] p,float nx,float ny,float nz,float u,float v,float oy,float oz) {
        return new MeshPartBuilder.VertexInfo().setPos(p[0],p[1]+oy,p[2]+oz).setNor(nx,ny,nz).setUV(u,v);
    }
    public void render(ModelBatch batch, Environment environment,List<Zombie> zombies) {
        instances.keySet().retainAll(new HashSet<>(zombies));
        for(Zombie zombie:zombies) {
            Visual visual=instances.computeIfAbsent(zombie,z->new Visual(new ModelInstance(model),z));
            double dx=zombie.getX()-visual.x,dz=zombie.getZ()-visual.z;
            double moved=Math.hypot(dx,dz);
            if(moved>1e-5) { visual.yaw=(float)Math.toDegrees(Math.atan2(dx,dz));visual.phase+=moved*7; }
            visual.x=zombie.getX();visual.z=zombie.getZ();
            float swing=moved>1e-5?(float)Math.sin(visual.phase)*25:0;
            for (ZombiePose.Part part : PARTS) {
                ZombiePose.Pose pose = ZombiePose.part(part, zombie.isCrawler(), zombie.isShattered(),
                        zombie.getDeadSeconds(), swing);
                var node = visual.instance.getNode(part.node);
                node.translation.set(pose.x(), pose.y(), pose.z());
                node.rotation.set(Vector3.X, pose.rotationX()).mul(
                        partRotation.set(Vector3.Z, pose.rotationZ()));
                node.scale.set(pose.scale(), pose.scale(), pose.scale());
            }
            visual.instance.calculateTransforms();
            float scale=1; Color color=texturesEnabled?Color.WHITE:BODY;
            if(zombie.getState()==ZombieState.DEAD && !zombie.isShattered()) {
                scale=1+(float)Math.min(1,zombie.getDeadSeconds()/EnemyUpdateService.DESPAWN_SECONDS)*1.6f;color=BURST;
            }
            if (zombie.hitFlashSeconds() > 0) color=Color.RED;
            visual.instance.transform.setToTranslation((float)zombie.getX(),(float)zombie.getY(),(float)zombie.getZ())
                    .rotate(Vector3.Y,visual.yaw).scale(scale,scale,scale);
            for(Material material:visual.instance.materials) {
                material.set(ColorAttribute.createDiffuse(color));
                if(texturesEnabled && zombie.hitFlashSeconds() <= 0) material.set(TextureAttribute.createDiffuse(skinTexture));
                else material.remove(TextureAttribute.Diffuse);
            }
            batch.render(visual.instance,environment);
        }
    }
    int instanceCount() { return instances.size(); }
    @Override public void dispose() { instances.clear();model.dispose();skinTexture.dispose(); }
    private static final class Visual {
        final ModelInstance instance;double x,z,phase;float yaw;
        Visual(ModelInstance instance,Zombie zombie) { this.instance=instance;x=zombie.getX();z=zombie.getZ(); }
    }
}
