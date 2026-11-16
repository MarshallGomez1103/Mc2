package network;

import application.ChunkGenerationService;
import application.GameSession;
import application.GameSettings;
import application.PlayerControlService;
import application.PlayerFrameInput;
import application.PlayerInteractionService;
import application.WorldApplicationService;
import application.WorldSize;
import domain.enemy.Difficulty;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import domain.world.World;
import patterns.factory.BlockFactory;
import persistence.JsonWorldStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Authoritative multiplayer runtime. Owned exclusively by the server tick thread after construction. */
final class ServerSimulation {
    private final World world;
    private final GameSession session;
    private final WorldApplicationService storage;
    private final Map<Integer, PlayerControlService> loadControls = new LinkedHashMap<>();
    private final Map<Integer, String> names = new LinkedHashMap<>();
    private final Map<domain.Position, Map<String,Object>> blockChanges = new LinkedHashMap<>();
    private final int target;
    private final SplittableRandom random = new SplittableRandom(20260925L);

    ServerSimulation(ServerConfig config) throws IOException {
        target = config.loadZombies();
        BlockFactory blocks = new BlockFactory();
        if (target == 0) {
            storage = new WorldApplicationService(new JsonWorldStorage(config.worldDirectory(), blocks), blocks);
            if (storage.listWorlds().contains("server")) storage.loadWorld("server");
            else storage.createWorld("server", WorldSize.SMALL);
            world = storage.currentWorld().orElseThrow();
        } else {
            // Fixed-population benchmark: same terrain seed, size and difficulty as the C2 recovery.
            // It is deliberately transient and never overwrites a saved game.
            storage = null;
            world = new World("load", 20260925L, Instant.parse("2026-09-25T00:00:00Z"));
            SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(world.getSeed());
            new ChunkGenerationService(terrain, blocks).generateChunks(10, 10).forEach(world::addChunk);
            world.getPlayer().setX(80.5); world.getPlayer().setZ(80.5);
            world.getPlayer().setY(terrain.surfaceHeightAt(80, 80) + 1d);
        }
        GameSettings settings = new GameSettings();
        if (target > 0) settings.setDifficulty(Difficulty.VERY_HARD);
        session = new GameSession(world, settings);
        session.leave(world.primaryPlayerId());
        world.addObserver(change -> {
            var pos = change.block().getPosition();
            blockChanges.put(pos, Map.of("x", pos.x(), "y", pos.y(), "z", pos.z(), "type",
                    change.type() == domain.world.BlockChange.Type.REMOVED ? "AIR" : change.block().getType().name()));
        });
    }

    int join(String name, int skin) {
        int id = session.join(skin);
        names.put(id, name);
        loadControls.put(id, new PlayerControlService(world, world.players().get(id), new PlayerInteractionService(),
                session.enemies(), session.pistol(id)));
        return id;
    }
    void leave(int id) { session.leave(id); names.remove(id); loadControls.remove(id); }
    boolean canApply(int id) { return names.containsKey(id) && (target > 0 || !session.life(id).isDead()); }
    void respawn(int id) { if (session.life(id).isDead()) session.respawn(id); }
    void advance(double delta, Map<Integer, PlayerFrameInput> inputs) {
        if (target == 0) { session.advanceAll(delta, inputs); return; }
        // Benchmark keeps the fixed population and real multiplayer controls/AI, without wave growth.
        for (int id : names.keySet()) if (session.life(id).isDead()) session.respawn(id);
        int spawned = refill(4);
        loadControls.forEach((id, control) -> control.update(delta, inputs.getOrDefault(id, PlayerFrameInput.NEUTRAL)));
        for (int id : names.keySet()) { session.life(id).update(); session.life(id).advance(delta); }
        if (names.isEmpty()) session.enemies().update(session.life(), delta);
        else session.enemies().update(names.keySet().stream().map(session::life).toList(), delta);
        for (int id : names.keySet()) if (session.life(id).isDead()) session.respawn(id);
        refill(4 - spawned);
    }
    Map<String,Object> worldInfo() {
        return Map.of("name", world.getName(), "seed", world.getSeed(), "chunkCount", world.getChunks().size());
    }
    int chunkCount() { return world.getChunks().size(); }
    Map<String,Object> chunk(int index) { return TerrainCodec.encode(world.getChunks().get(index)); }
    List<Map<String,Object>> drainBlockChanges() {
        var result = List.copyOf(blockChanges.values()); blockChanges.clear(); return result;
    }
    private int refill(int budget) {
        int active = activeZombies();
        int spawned = 0;
        Player player = world.getPlayer();
        // Bound startup/refill work and stagger first path searches across ticks.
        for (int attempts = 0; active < target && spawned < budget && attempts < 32; attempts++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 8 + random.nextDouble() * 10;
            if (session.enemies().spawnAt(player.getX() + Math.cos(angle) * radius,
                    player.getZ() + Math.sin(angle) * radius).isPresent()) { active++; spawned++; }
        }
        return spawned;
    }
    int activeZombies() {
        return (int) session.enemies().zombies().stream().filter(Zombie::isAlive).count();
    }
    boolean running() { return session.state() == GameSession.State.RUNNING; }
    void save() throws IOException {
        if (storage != null) {
            if (session.life().isDead()) session.respawn();
            storage.saveCurrentWorld();
        }
    }

    Map<String, Object> state(long tick) {
        List<Map<String,Object>> players = names.entrySet().stream().map(entry -> {
            int id = entry.getKey(); Player p = world.players().get(id);
            Map<String,Object> player = new LinkedHashMap<>();
            player.put("playerId", id); player.put("name", entry.getValue()); player.put("skinId", p.getSkinId());
            player.put("x", p.getX()); player.put("y", p.getY()); player.put("z", p.getZ());
            player.put("yaw", p.getYaw()); player.put("pitch", p.getPitch());
            player.put("health", session.life(id).health()); player.put("dead", session.life(id).isDead());
            player.put("pistolOwned", session.pistol(id).owned()); player.put("pistolEquipped", session.pistol(id).equipped());
            player.put("pickup", session.pistol(id).pickupPosition().map(pos -> Map.of("x",pos.x(),"y",pos.y(),"z",pos.z())).orElse(null));
            return player;
        }).toList();
        List<Map<String, Object>> zombies = session.enemies().zombies().stream().map(z -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("x", z.getX()); item.put("y", z.getY()); item.put("z", z.getZ());
            item.put("health", z.getHealth()); item.put("state", z.getState().name());
            return item;
        }).toList();
        return Map.of("type", "state", "tick", tick, "players", players, "zombies", zombies);
    }
}
