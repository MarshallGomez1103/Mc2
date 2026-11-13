package application;

import domain.enemy.Difficulty;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.World;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime de una partida. El menú puede cambiar preferencias sin alterar parámetros en uso.
 * A server adapter may add players with {@link #join(int)}; single player never does and
 * behaves as before, through the primary-player accessors.
 * Not thread-safe: call join, leave, advanceAll and respawn from the one tick thread, passing a snapshot map.
 */
public final class GameSession {
    public enum State { RUNNING, PAUSED, DEAD }

    /** Everything that belongs to one player: body, life, weapon and controls. */
    private record Slot(Player player, PlayerLife life, PistolService pistol, PlayerControlService control) { }

    private final World world;
    private final Difficulty difficulty;
    private final PlayerInteractionService interactionService;
    private final EnemyUpdateService enemies;
    private final HordeManager hordes;
    /** Join order, same keys as {@code world.players()}. */
    private final Map<Integer, Slot> slots = new LinkedHashMap<>();
    private boolean paused;
    /** True after the last player left: the body is kept but nobody drives it. */
    private boolean vacant;

    public GameSession(World world, GameSettings settings) {
        this(world, settings, new PlayerInteractionService());
    }

    public GameSession(World world, GameSettings settings, PlayerInteractionService interactionService) {
        this.world = Objects.requireNonNull(world);
        this.interactionService = interactionService;
        difficulty = Objects.requireNonNull(settings).difficulty();
        enemies = new EnemyUpdateService(world, difficulty.zombieParameters());
        enemies.setEnabled(settings.enemiesEnabled());
        hordes = new HordeManager(difficulty.waveRules(), world, enemies);
        world.players().forEach((id, player) -> slots.put(id, newSlot(player)));
    }

    private Slot newSlot(Player player) {
        PistolService pistol = new PistolService(world, player);
        return new Slot(player, new PlayerLife(player), pistol,
                new PlayerControlService(world, player, interactionService, enemies, pistol));
    }

    private Slot primary() { return slots.get(world.primaryPlayerId()); }

    private Slot slot(int id) {
        Slot slot = slots.get(id);
        if (slot == null) throw new IllegalArgumentException("Unknown player id: " + id);
        return slot;
    }

    /** PAUSED while paused or vacant; DEAD only when every player is dead. */
    public State state() {
        if (paused || vacant) return State.PAUSED;
        return slots.values().stream().allMatch(slot -> slot.life().isDead()) ? State.DEAD : State.RUNNING;
    }
    public void setPaused(boolean value) { paused = value; }
    public boolean isPaused() { return paused; }
    public PlayerLife life() { return primary().life(); }
    public EnemyUpdateService enemies() { return enemies; }
    public HordeManager hordes() { return hordes; }
    public PistolService pistol() { return primary().pistol(); }
    public Difficulty difficulty() { return difficulty; }
    public void applySettings(GameSettings settings) { enemies.setEnabled(settings.enemiesEnabled()); }

    /** Entrada, física, stamina e IA avanzan únicamente en RUNNING, con el mismo delta limitado. */
    public void advance(double rawDelta, PlayerFrameInput input) {
        if (state() != State.RUNNING || !Double.isFinite(rawDelta) || rawDelta <= 0) return;
        Objects.requireNonNull(input, "input no puede ser null");
        advanceAll(rawDelta, Map.of(world.primaryPlayerId(), input));
    }

    /**
     * One tick for every player, processed in join order and never in the caller's map order.
     * A missing input is NEUTRAL, an unknown id is ignored and a null value is rejected. It is
     * named differently from {@link #advance(double, PlayerFrameInput)} so that a null second
     * argument stays unambiguous for existing callers.
     */
    public void advanceAll(double rawDelta, Map<Integer, PlayerFrameInput> inputs) {
        if (state() != State.RUNNING || !Double.isFinite(rawDelta) || rawDelta <= 0) return;
        Objects.requireNonNull(inputs, "inputs must not be null");
        double delta = Math.min(rawDelta, PlayerControlService.MAX_DELTA_SECONDS);
        // Validate before mutating anything, so a null value cannot leave a half-processed tick.
        slots.keySet().forEach(id -> {
            if (inputs.containsKey(id)) Objects.requireNonNull(inputs.get(id), "input no puede ser null");
        });
        slots.forEach((id, slot) -> {
            if (slot.life().isDead()) return;
            PlayerFrameInput input = inputs.getOrDefault(id, PlayerFrameInput.NEUTRAL);
            slot.control().update(delta, Objects.requireNonNull(input, "input no puede ser null"));
        });
        slots.values().forEach(slot -> {
            slot.life().update();
            slot.life().advance(delta);
        });
        Slot reference = firstLiving();
        if (reference == null) return;
        // ponytail: hordes spawn around the first living player only; a List<Player> overload would cover all.
        hordes.update(delta, reference.player().getX(), reference.player().getZ());
        enemies.update(slots.values().stream().map(Slot::life).toList(), delta);
        slots.values().forEach(slot -> {
            if (!slot.life().isDead()) slot.pistol().update(hordes.wave(), delta);
        });
    }

    private Slot firstLiving() {
        return slots.values().stream().filter(slot -> !slot.life().isDead()).findFirst().orElse(null);
    }

    /**
     * Adds a player with its own life, pistol and controls and returns its id. The new player
     * appears where the first living player stands (the primary if nobody is alive). A bad skin
     * is rejected before anything changes. After the last player left, the vacant body is
     * reclaimed instead and keeps its id.
     */
    public int join(int skinId) {
        if (vacant) {
            // ponytail: the previous driver's body is reclaimed as is (position, health, pistol); rebuild the slot for a fresh start.
            Slot body = primary();
            body.player().setSkinId(skinId);
            vacant = false;
            if (body.life().isDead()) respawn(world.primaryPlayerId());
            return world.primaryPlayerId();
        }
        // ponytail: spawns on the first living player's block (may share it); a free-cell search would fix it.
        Slot near = firstLiving() != null ? firstLiving() : primary();
        Player joined = new Player(near.player().getX(), near.player().getY(), near.player().getZ());
        joined.setSkinId(skinId);
        int id = world.addPlayer(joined);
        slots.put(id, newSlot(joined));
        return id;
    }

    /** Removes a player. The last one stays as a vacant body and the session pauses. */
    public void leave(int id) {
        if (!slots.containsKey(id)) return;
        if (slots.size() == 1) {
            vacant = true;
            return;
        }
        world.removePlayer(id);
        slots.remove(id);
    }

    public PlayerLife life(int id) { return slot(id).life(); }
    public PlayerControlState controls(int id) { return slot(id).control().state(); }
    public PistolService pistol(int id) { return slot(id).pistol(); }
    public PlayerControlState controls() { return controls(world.primaryPlayerId()); }
    public void respawn() { respawn(world.primaryPlayerId()); }
    public void respawn(int id) {
        Slot slot = slot(id);
        slot.life().respawn();
        slot.control().resetAfterRespawn();
    }
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
