package application;

import domain.enemy.Difficulty;
import domain.player.PlayerLife;
import domain.world.World;
import java.util.Objects;

/** Runtime de una partida. El menú puede cambiar preferencias sin alterar parámetros en uso. */
public final class GameSession {
    public enum State { RUNNING, PAUSED, DEAD }
    private final World world;
    private final Difficulty difficulty;
    private final PlayerLife life;
    private final EnemyUpdateService enemies;
    private final HordeManager hordes;
    private final PistolService pistol;
    private boolean paused;

    public GameSession(World world, GameSettings settings) {
        this.world = Objects.requireNonNull(world);
        difficulty = Objects.requireNonNull(settings).difficulty();
        life = new PlayerLife(world.getPlayer());
        enemies = new EnemyUpdateService(world, difficulty.zombieParameters());
        enemies.setEnabled(settings.enemiesEnabled());
        hordes = new HordeManager(difficulty.waveRules(), world, enemies);
        pistol = new PistolService(world);
    }
    public State state() { return paused ? State.PAUSED : life.isDead() ? State.DEAD : State.RUNNING; }
    public void setPaused(boolean value) { paused = value; }
    public boolean isPaused() { return paused; }
    public PlayerLife life() { return life; }
    public EnemyUpdateService enemies() { return enemies; }
    public HordeManager hordes() { return hordes; }
    public PistolService pistol() { return pistol; }
    public Difficulty difficulty() { return difficulty; }
    public void applySettings(GameSettings settings) { enemies.setEnabled(settings.enemiesEnabled()); }

    /** Entrada, física, stamina e IA avanzan únicamente en RUNNING, con el mismo delta limitado. */
    public void advance(double rawDelta, Runnable playerUpdate) {
        if (state() != State.RUNNING || !Double.isFinite(rawDelta) || rawDelta <= 0) return;
        double delta = Math.min(rawDelta, 0.05);
        Objects.requireNonNull(playerUpdate).run();
        life.update();
        life.advance(delta);
        if (!life.isDead()) {
            hordes.update(delta, world.getPlayer().getX(), world.getPlayer().getZ());
            enemies.update(life, delta);
            if (!life.isDead()) pistol.update(hordes.wave(), delta);
        }
    }
    public void respawn() { life.respawn(); }
    public String waveSummary() {
        if (!enemies.isEnabled()) return "desactivadas";
        long seconds = (long) Math.ceil(hordes.secondsToNextWave());
        return switch (hordes.phase()) {
            case WAITING -> "oleada " + (hordes.wave() + 1) + " en " + seconds + " s";
            case SPAWNING -> "oleada " + hordes.wave() + " | por aparecer: " + hordes.pendingSpawns()
                    + " | en juego: " + hordes.remainingInWave();
            case ACTIVE -> "oleada " + hordes.wave() + " | quedan: " + hordes.remainingInWave();
            case PAUSE -> "oleada " + hordes.wave() + " superada | siguiente en " + seconds + " s";
        };
    }
}
