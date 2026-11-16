package presentation.game;

import application.PlayerFrameInput;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.ScreenUtils;
import domain.player.Player;
import domain.world.Chunk;
import network.*;
import java.util.*;

/** Network presentation: submits intentions and draws server snapshots, never advances a session. */
public final class RemoteVoxelGame extends ApplicationAdapter {
    private final NetworkClient client;
    private final GameInput controls = new GameInput();
    private final NetworkInputBuffer pending = new NetworkInputBuffer();
    private RemoteWorldReplica replica;
    private PerspectiveCamera camera;
    private ModelBatch batch;
    private Environment light;
    private BlockTextureAtlas atlas;
    private ChunkMeshBuilder builder;
    private RemotePlayerRenderer players;
    private SpriteBatch overlay;
    private BitmapFont font;
    private Model zombieModel;
    private Model pickupModel;
    private ModelInstance pickup;
    private final List<ModelInstance> zombies = new ArrayList<>();
    private final Map<Chunk,ModelInstance> meshes = new LinkedHashMap<>();
    private final Set<Chunk> dirty = new LinkedHashSet<>();
    private double elapsed;
    private boolean respawn;
    private String failure;
    private int frames;
    private boolean captured;
    public RemoteVoxelGame(NetworkClient client) { this.client=Objects.requireNonNull(client); }
    @Override public void create() {
        camera = new PerspectiveCamera(70,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()); camera.near=.1f; camera.far=300;
        batch=new ModelBatch(); light=new Environment(); light.set(new ColorAttribute(ColorAttribute.AmbientLight,1,1,1,1));
        atlas=new BlockTextureAtlas(); players=new RemotePlayerRenderer(); overlay=new SpriteBatch(); font=new BitmapFont();
        zombieModel=new ModelBuilder().createBox(.5f,1.7f,.35f,new Material(ColorAttribute.createDiffuse(new Color(.22f,.58f,.24f,1))),
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);
        pickupModel=new ModelBuilder().createBox(.35f,.2f,.2f,new Material(ColorAttribute.createDiffuse(Color.YELLOW)),
                VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);pickup=new ModelInstance(pickupModel);
        Gdx.input.setCursorCatched(true);
    }
    @Override public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched()); pending.clear();
        }
        if (replica==null && client.welcome()!=null && client.welcome().world()!=null) {
            replica=new RemoteWorldReplica(client.welcome().world()); builder=new ChunkMeshBuilder(replica.world(),atlas,true);
            replica.world().addObserver(change -> {
                var p=change.block().getPosition();
                for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                    replica.world().findChunk(Math.floorDiv(p.x(),16)+dx,Math.floorDiv(p.z(),16)+dz).ifPresent(dirty::add);
            });
        }
        try {
            if(replica!=null) for(int i=0;i<2;i++) {
                var terrain=client.pollTerrain(); if(terrain==null) break; replica.apply(terrain);
            }
        } catch(RuntimeException e) { failure="Invalid world: "+e.getMessage(); client.close(); }
        var state=client.latestState();
        var local=state==null || client.welcome()==null ? null : state.players().stream()
                .filter(p->p.playerId()==client.welcome().playerId()).findFirst().orElse(null);
        if (replica!=null && replica.ready() && local!=null && !local.dead() && Gdx.input.isCursorCatched()) pending.add(controls.read());
        else pending.clear();
        respawn |= local!=null && local.dead() && Gdx.input.isKeyJustPressed(Input.Keys.R);
        elapsed+=Math.min(Gdx.graphics.getDeltaTime(),.1f);
        if(client.status()==NetworkClient.Status.CONNECTED && elapsed>=1d/client.welcome().tickHz()) {
            client.send(pending.take(),respawn); respawn=false; elapsed=0;
        }
        ScreenUtils.clear(.45f,.68f,.92f,1,true);
        if(replica!=null && replica.ready() && local!=null) {
            Player player=replica.local(local); updateCamera(player); refreshMeshes(player);
            batch.begin(camera);
            for(var entry:meshes.entrySet()) {
                Chunk c=entry.getKey();
                if(camera.frustum.boundsInFrustum(new Vector3((c.getChunkX()+.5f)*16,32,(c.getChunkZ()+.5f)*16),new Vector3(16,64,16)))
                    batch.render(entry.getValue(),light);
            }
            players.render(batch,light,state.players(),client.welcome().playerId());
            while(zombies.size()<state.zombies().size()) zombies.add(new ModelInstance(zombieModel));
            while(zombies.size()>state.zombies().size()) zombies.remove(zombies.size()-1);
            for(int i=0;i<state.zombies().size();i++) {
                var z=state.zombies().get(i); if(z.health()<=0)continue;
                zombies.get(i).transform.setToTranslation((float)z.x(),(float)z.y()+.85f,(float)z.z()); batch.render(zombies.get(i),light);
            }
            if(local.pickup()!=null) {
                var pos=local.pickup();pickup.transform.setToTranslation((float)pos.x()+.5f,(float)pos.y()+.3f,(float)pos.z()+.5f);
                batch.render(pickup,light);
            }
            batch.end();
        }
        overlay.begin();
        String status=failure!=null?failure:client.status()+ (client.error()==null?"":" - "+client.error());
        font.draw(overlay,"Minecraft2 | "+status,15,Gdx.graphics.getHeight()-18);
        if(replica!=null && !replica.ready()) font.draw(overlay,"Recibiendo mundo: "+replica.receivedChunks()+" chunks",15,Gdx.graphics.getHeight()-40);
        if(local!=null) {
            font.draw(overlay,"Vida: "+(int)local.health()+" | jugadores: "+state.players().size()+" | skin: "+local.skinId()+" | tick: "+state.tick(),15,Gdx.graphics.getHeight()-60);
            font.draw(overlay,"Arma: "+(local.pistolEquipped()?"pistola equipada":local.pistolOwned()?"pistola guardada (Q)":"sin pistola; recogida amarilla"),15,Gdx.graphics.getHeight()-80);
            if(local.dead())font.draw(overlay,"Has muerto. R: reaparecer",15,Gdx.graphics.getHeight()-105);
        }
        font.draw(overlay,"WASD mover | raton mirar | espacio saltar | 1-7 bloques | clic actuar | Q arma | ESC cursor",15,25);
        font.draw(overlay,"+",Gdx.graphics.getWidth()/2f,Gdx.graphics.getHeight()/2f);
        overlay.end();
        frames++;
        String capture=System.getProperty("mc2.screenshot");
        if(!captured && capture!=null && replica!=null && replica.ready() && frames>=Integer.getInteger("mc2.screenshot.frame",180)
                && state.players().size()>=Integer.getInteger("mc2.screenshot.players",1) && meshes.size()>=4) {
            Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            var data=pixels.getPixels();int row=pixels.getWidth()*4;byte[] flipped=new byte[row*pixels.getHeight()];
            for(int y=0;y<pixels.getHeight();y++) { data.position((pixels.getHeight()-1-y)*row);data.get(flipped,y*row,row); }
            data.clear();data.put(flipped);data.clear();
            PixmapIO.writePNG(Gdx.files.absolute(capture),pixels);pixels.dispose();captured=true;
            System.out.println("[remote-render] screenshot "+capture+" players="+state.players().size());
            if(Boolean.parseBoolean(System.getProperty("mc2.screenshot.exit","true")))Gdx.app.exit();
        }
    }
    private void updateCamera(Player p) {
        double yaw=Math.toRadians(p.getYaw()),pitch=Math.toRadians(p.getPitch());
        camera.position.set((float)p.getX(),(float)p.getY()+1.6f,(float)p.getZ());
        camera.direction.set((float)(-Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(Math.cos(yaw)*Math.cos(pitch)));
        camera.up.set(0,1,0);camera.update();
    }
    private void refreshMeshes(Player player) {
        int x=Math.floorDiv((int)Math.floor(player.getX()),16),z=Math.floorDiv((int)Math.floor(player.getZ()),16);
        var it=meshes.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();Chunk c=entry.getKey();
            if(Math.abs(c.getChunkX()-x)>3 || Math.abs(c.getChunkZ()-z)>3 || dirty.contains(c)) {
                entry.getValue().model.dispose();it.remove();
            }
        }
        dirty.clear();int budget=2;
        for(int ring=0;ring<=3 && budget>0;ring++)for(int dx=-ring;dx<=ring && budget>0;dx++)for(int dz=-ring;dz<=ring && budget>0;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
            Chunk c=replica.world().findChunk(x+dx,z+dz).orElse(null);
            if(c!=null && !meshes.containsKey(c)) { meshes.put(c,new ModelInstance(builder.build(c).model())); budget--; }
        }
    }
    @Override public void resize(int width,int height) {
        if(width<=0 || height<=0)return;
        if(camera!=null) { camera.viewportWidth=width;camera.viewportHeight=height;camera.update(); }
        if(overlay!=null)overlay.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,width,height));
    }
    @Override public void dispose() {
        client.close();meshes.values().forEach(m->m.model.dispose());meshes.clear();
        if(batch!=null)batch.dispose();if(atlas!=null)atlas.dispose();if(players!=null)players.dispose();
        if(zombieModel!=null)zombieModel.dispose();if(pickupModel!=null)pickupModel.dispose();if(overlay!=null)overlay.dispose();if(font!=null)font.dispose();
    }
}
