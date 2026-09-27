package application;

import domain.Position;
import domain.block.BlockType;
import domain.enemy.AStarPathfinder;
import domain.enemy.NavigationGrid;
import domain.enemy.NavigationNode;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;

/** Arma de sesión: una aparición desde la oleada 3, recogida cercana y disparos contra enemigos. */
public final class PistolService {
    public static final int FIRST_WAVE = 3;
    public static final int DAMAGE = 3;
    public static final double REACH = 36;
    public static final double SHOT_COOLDOWN = .28;
    private static final double PICKUP_RADIUS = 1.25;
    private final World world;
    private final NavigationGrid grid;
    private final ZombieTargeting targeting;
    private final SplittableRandom random;
    private Position pickup;
    private boolean appeared;
    private boolean owned;
    private boolean equipped;
    private double cooldown;
    private double shotFlash;
    private double retrySpawn;

    public PistolService(World world) { this(world, world.getSeed() ^ 0x504953544f4cL); }
    public PistolService(World world, long seed) {
        this.world = Objects.requireNonNull(world);
        grid = new NavigationGrid(world);
        targeting = new ZombieTargeting(world);
        random = new SplittableRandom(seed);
    }

    /** Solo GameSession.RUNNING llama a este método; pausa y muerte congelan arma y recogida. */
    public void update(int wave, double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) throw new IllegalArgumentException("Delta inválido");
        cooldown = Math.max(0, cooldown - deltaSeconds);
        shotFlash = Math.max(0, shotFlash - deltaSeconds);
        retrySpawn = Math.max(0, retrySpawn - deltaSeconds);
        if (!appeared && wave >= FIRST_WAVE && retrySpawn == 0) {
            pickup = chooseSpawn().orElse(null);
            appeared = pickup != null;
            if (!appeared) retrySpawn = 5;
        }
        if (pickup != null) {
            Player player = world.getPlayer();
            if (Math.hypot(player.getX() - (pickup.x() + .5), player.getZ() - (pickup.z() + .5)) <= PICKUP_RADIUS
                    && Math.abs(player.getY() - pickup.y()) <= 1.2) {
                owned = true;
                equipped = true;
                pickup = null;
            }
        }
    }

    private Optional<Position> chooseSpawn() {
        Player player = world.getPlayer();
        Optional<NavigationNode> start = grid.nodeUnder(player.getX(), player.getY(), player.getZ());
        if (start.isEmpty() || world.getChunks().isEmpty()) return Optional.empty();
        AStarPathfinder paths = new AStarPathfinder(2_000);
        int searches = 0;
        Position safeFallback = null;
        for (int attempt = 0; attempt < 120 && searches < 8; attempt++) {
            int x, z;
            if (attempt < 24) {
                Chunk chunk = world.getChunks().get(random.nextInt(world.getChunks().size()));
                x = chunk.getChunkX() * Chunk.WIDTH + random.nextInt(Chunk.WIDTH);
                z = chunk.getChunkZ() * Chunk.DEPTH + random.nextInt(Chunk.DEPTH);
            } else {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 5 + random.nextDouble() * 20;
                x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
                z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            }
            double distance = Math.hypot(x + .5 - player.getX(), z + .5 - player.getZ());
            if (distance < 3 || distance > 32) continue;
            Optional<NavigationNode> node = groundAt(x, z);
            if (node.isEmpty()) continue;
            if (safeFallback == null) safeFallback = new Position(x, node.get().feetY(), z);
            searches++;
            if (paths.findPath(grid, start.get(), node.get()).isPresent()) {
                return Optional.of(new Position(x, node.get().feetY(), z));
            }
        }
        // Un mundo pequeño o modificado puede ofrecer muy pocas columnas: probar cerca del jugador.
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = start.get().x() + dx, z = start.get().z() + dz;
                    Optional<NavigationNode> node = groundAt(x, z);
                    if (node.isEmpty()) continue;
                    if (safeFallback == null) safeFallback = new Position(x, node.get().feetY(), z);
                    if (++searches > 16) return Optional.ofNullable(safeFallback);
                    if (paths.findPath(grid, start.get(), node.get()).isPresent()) {
                        return Optional.of(new Position(x, node.get().feetY(), z));
                    }
                }
            }
        }
        // A player trapped in a shaft can build an exit; do not suppress the wave-3 reward.
        return Optional.ofNullable(safeFallback);
    }

    private Optional<NavigationNode> groundAt(int x, int z) {
        if (!grid.chunkExists(x, z)) return Optional.empty();
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            if (!grid.isSolid(x, y, z)) continue;
            Position ground = new Position(x, y, z);
            BlockType material = world.findChunk(ground).orElseThrow().getBlock(ground).orElseThrow().getType();
            if (material == BlockType.WOOD || material == BlockType.LEAVES || y > Chunk.HEIGHT - 3) return Optional.empty();
            NavigationNode candidate = new NavigationNode(x, y, z);
            return grid.isWalkable(candidate) ? Optional.of(candidate) : Optional.empty();
        }
        return Optional.empty();
    }

    /** Un tiro se consume aunque falle; nunca cambia bloques. Munición ilimitada. */
    public boolean shoot(Player player, Collection<Zombie> zombies) {
        if (!equipped || cooldown > 0) return false;
        cooldown = SHOT_COOLDOWN;
        shotFlash = .07;
        targeting.findTarget(player, zombies, REACH).ifPresent(zombie -> zombie.takeDamage(DAMAGE));
        return true;
    }

    public void toggleEquipped() { if (owned) equipped = !equipped; }
    public void unequip() { equipped = false; }
    public boolean appeared() { return appeared; }
    public boolean owned() { return owned; }
    public boolean equipped() { return equipped; }
    public Optional<Position> pickupPosition() { return Optional.ofNullable(pickup); }
    public double shotFlashSeconds() { return shotFlash; }
    public double cooldownSeconds() { return cooldown; }
}
