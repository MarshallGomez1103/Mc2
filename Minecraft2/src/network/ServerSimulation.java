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

/** Single-player runtime for phase B. Owned exclusively by the server tick thread after construction. */
final class ServerSimulation {
    private final World world;
    private final GameSession session;
    private final WorldApplicationService storage;
    private final PlayerControlService loadControls;
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
        loadControls = new PlayerControlService(world, new PlayerInteractionService(), session.enemies(), session.pistol());
    }

    void advance(double delta, PlayerFrameInput input, boolean connected) {
        if (target == 0) {
            if (connected) session.advance(delta, input);
            return;
        }
        // Fixed-population mode advances the real controls, physics and AI, but not wave growth.
        // Respawn only in this benchmark mode so a dead target cannot silently freeze the measured AI.
        if (session.life().isDead()) session.respawn();
        int spawned = refill(4);
        loadControls.update(delta, input);
        session.life().update();
        session.life().advance(delta);
        session.enemies().update(session.life(), delta);
        if (session.life().isDead()) session.respawn();
        refill(4 - spawned);
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
    void save() throws IOException { if (storage != null) storage.saveCurrentWorld(); }

    Map<String, Object> state(long tick, long ack, String name, int skin) {
        Player p = world.getPlayer();
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("playerId", 0); player.put("name", name); player.put("skinId", skin);
        player.put("x", p.getX()); player.put("y", p.getY()); player.put("z", p.getZ());
        player.put("yaw", p.getYaw()); player.put("pitch", p.getPitch());
        player.put("health", session.life().health());
        player.put("dead", session.life().isDead());
        List<Map<String, Object>> zombies = session.enemies().zombies().stream().map(z -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("x", z.getX()); item.put("y", z.getY()); item.put("z", z.getZ());
            item.put("health", z.getHealth()); item.put("state", z.getState().name());
            return item;
        }).toList();
        return Map.of("type", "state", "tick", tick, "ack", ack,
                "players", List.of(player), "zombies", zombies);
    }
}
