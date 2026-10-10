package presentation.game;

import application.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PixmapIO;
import domain.Position;
import domain.block.BlockType;
import domain.enemy.*;
import domain.world.World;
import java.lang.reflect.*;
import java.util.*;

/** Smoke OpenGL con entrada automatizada; no representa una sesión de usabilidad humana. */
public final class PlayerControlsVisualSmoke extends ApplicationAdapter {
    private VoxelGame game;
    private GameSession session;
    private World world;
    private Input realInput;
    private Input scriptedInput;
    private int frame;
    private Set<Integer> held = Set.of(), pressed = Set.of(), buttons = Set.of();
    private int dx, dy;
    private double frozenY, frozenZ, frozenEnergy, cooldown;
    private Zombie target;
    private final String output;
    private boolean completed;
    private static volatile Throwable failure;
    public PlayerControlsVisualSmoke(String output) { this.output = output; }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Indicar el archivo PNG de salida");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MC2 control verification");
        config.setWindowedMode(960, 640); config.setForegroundFPS(60); config.useVsync(false);
        try { new Lwjgl3Application(new PlayerControlsVisualSmoke(args[0]), config); }
        catch (Throwable problem) { failure = problem; }
        if (failure != null) { failure.printStackTrace(); System.exit(1); }
    }
    public void create() {
        try {
            world = TestWorlds.flat(1, 1);
            var p = world.getPlayer(); p.setX(8.5); p.setY(21); p.setZ(5.5);
            var settings = new GameSettings(); settings.toggleEnemies();
            game = new VoxelGame(world, new PlayerInteractionService(), true, settings);
            game.create();
            Field field = VoxelGame.class.getDeclaredField("session"); field.setAccessible(true);
            session = (GameSession) field.get(game);
            realInput = Gdx.input;
            scriptedInput = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (proxy, method, args) -> switch(method.getName()) {
                    case "isKeyPressed" -> held.contains((Integer)args[0]);
                    case "isKeyJustPressed" -> pressed.contains((Integer)args[0]);
                    case "isButtonJustPressed" -> buttons.contains((Integer)args[0]);
                    case "getDeltaX" -> dx;
                    case "getDeltaY" -> dy;
                    default -> method.invoke(realInput, args);
                });
            System.out.println("OpenGL renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER));
        } catch(Throwable problem) { failure = problem; Gdx.app.exit(); }
    }
    private static void check(boolean valid, String name) { if(!valid) throw new AssertionError(name); }
    private boolean blockPresent(int z) { return world.findChunk(0,0).orElseThrow().getBlock(new Position(8,22,z)).isPresent(); }
    public void render() {
        if(failure != null) return;
        try {
            // LWJGL restablece Gdx.input al activar la ventana en cada fotograma.
            Gdx.input = scriptedInput;
            var p = world.getPlayer(); held=Set.of(); pressed=Set.of(); buttons=Set.of(); dx=0;dy=0;
            if(frame>=5 && frame<20) held=Set.of(Input.Keys.W, Input.Keys.SHIFT_LEFT);
            if(frame==20) check(session.controls().staminaCurrent()<100 && p.getZ()>5.5, "sprint / HUD state");
            if(frame==22) pressed=Set.of(Input.Keys.SPACE);
            if(frame==23) { check(p.getY()>21,"jump"); dx=10;dy=-5; }
            if(frame==24) check(p.getYaw()==2 && p.getPitch()==1,"mouse look / camera");
            if(frame==30) { game.setPaused(true); frozenY=p.getY(); frozenZ=p.getZ(); frozenEnergy=session.controls().staminaCurrent(); }
            if(frame>=30 && frame<40) held=Set.of(Input.Keys.W, Input.Keys.SHIFT_LEFT);
            if(frame==40) { check(p.getY()==frozenY && p.getZ()==frozenZ && session.controls().staminaCurrent()==frozenEnergy,"pause freeze"); game.setPaused(false); }
            if(frame==50) { session.life().die(domain.player.PlayerLife.DeathCause.ENEMY); frozenY=p.getY(); frozenZ=p.getZ(); }
            if(frame>=51 && frame<60) held=Set.of(Input.Keys.W);
            if(frame==60) { check(p.getY()==frozenY && p.getZ()==frozenZ,"death freeze"); pressed=Set.of(Input.Keys.R); }
            if(frame==61) { check(session.state()==GameSession.State.RUNNING && session.controls().staminaCurrent()==100 && !session.controls().moving(),"R / respawn"); }
            if(frame==70) {
                p.setX(8.5);p.setY(21);p.setZ(5.5);p.setYaw(0);p.setPitch(0);p.setVelocityY(0);
                target=new Zombie(8.5,21,7.5,3);session.enemies().register(target);
                TestWorlds.place(world,8,22,9,BlockType.STONE);
            }
            if(frame==71) buttons=Set.of(Input.Buttons.LEFT);
            if(frame==72) { check(target.getHealth()==2 && blockPresent(9),"melee priority"); target.takeDamage(100); }
            if(frame==73) buttons=Set.of(Input.Buttons.LEFT);
            if(frame==74) check(!blockPresent(9),"mine block");
            if(frame==75) { TestWorlds.place(world,8,22,9,BlockType.STONE);pressed=Set.of(Input.Keys.NUM_6);buttons=Set.of(Input.Buttons.RIGHT); }
            if(frame==76) check(world.findChunk(0,0).orElseThrow().getBlock(new Position(8,22,8)).orElseThrow().getType()==BlockType.WOOD,"selection / placement");
            if(frame==80) {
                TestWorlds.remove(world,8,22,8);
                session.pistol().update(3,.01);var pickup=session.pistol().pickupPosition().orElseThrow();
                p.setX(pickup.x()+.5);p.setY(pickup.y());p.setZ(pickup.z()+.5);session.pistol().update(3,.01);
                p.setX(8.5);p.setY(21);p.setZ(5.5);
                target=new Zombie(8.5,21,7.5,3);session.enemies().register(target);
            }
            if(frame==81) buttons=Set.of(Input.Buttons.LEFT);
            if(frame==82) { check(!target.isAlive() && blockPresent(9),"pistol priority");cooldown=session.pistol().cooldownSeconds();game.setPaused(true); }
            if(frame==87) { check(session.pistol().cooldownSeconds()==cooldown,"paused pistol");game.setPaused(false); }
            if(frame==89) { pressed=Set.of(Input.Keys.NUM_1);buttons=Set.of(Input.Buttons.LEFT); }
            if(frame==90) check(!session.pistol().equipped() && !blockPresent(9),"selection unequips pistol before action");
            game.render();
            if(frame==95) {
                var pixmap=com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(0,0,960,640);
                try { PixmapIO.writePNG(Gdx.files.absolute(output),pixmap,-1,true); } finally { pixmap.dispose(); }
                verifyCloseAfterDeath();
                completed=true;System.out.println("PASS: sprint, jump, look, pause, death, R, melee, mining, placement, pistol, selection, rendered HUD/camera, close after death");Gdx.app.exit();
            }
            frame++;
        } catch(Throwable problem) { failure=problem;Gdx.app.exit(); }
    }
    private void verifyCloseAfterDeath() {
        var player = world.getPlayer();
        player.setX(8.5); player.setY(21); player.setZ(5.5); player.setVelocityY(0);
        session.advance(.05, new PlayerFrameInput(
                new domain.player.MovementInput(true, false, false, false, true),
                false, 0, 0, false, false, false, null));
        check(session.controls().staminaCurrent() < 100, "close fixture consumes stamina");
        var material = session.controls().selectedType();
        session.life().die(domain.player.PlayerLife.DeathCause.VOID);
        player.setY(-4);
        game.setPaused(true);
        game.dispose();
        game = null;
        check(!session.life().isDead() && player.getY() >= 1, "close restores a valid player");
        check(session.controls().staminaCurrent() == 100 && !session.controls().moving()
                && !session.controls().sprinting(), "close resets controls through application");
        check(session.isPaused() && session.controls().selectedType() == material,
                "close preserves pause and material");
    }
    public void dispose() {
        if(game!=null) game.dispose();
        if(realInput!=null) Gdx.input=realInput;
        if(!completed && failure==null) failure=new AssertionError("Window closed before verification completed");
    }
}
