package application;

import domain.enemy.WaveRules;
import domain.enemy.Zombie;
import domain.world.Chunk;
import domain.world.World;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Decide cuándo empieza cada oleada, cuántos zombis trae, cada cuánto aparece uno y cuándo
 * termina. No mueve zombis, no ejecuta la FSM ni A* y no dibuja: crea zombis a través de
 * {@link EnemyUpdateService#spawnAt(double, double)} y observa {@link EnemyUpdateService#zombies()}
 * para saber cuándo desaparecieron.
 *
 * <p>El tiempo llega explícito en {@link #update(double, double, double)}, así que el resultado
 * depende solo de la seed, de las reglas, de los deltas recibidos y de la posición del jugador:
 * las pruebas lo controlan sin relojes reales.
 *
 * <p>Ciclo: {@code WAITING} (espera inicial) → {@code SPAWNING} (aparece un zombi cada intervalo)
 * → {@code ACTIVE} (hasta que todos los zombis de la oleada murieron y desaparecieron) →
 * {@code PAUSE} (descanso) → {@code SPAWNING} de la oleada siguiente.
 */
public final class HordeManager {
    /** Intentos de encontrar una columna caminable antes de aplazar una aparición. */
    static final int SPAWN_ATTEMPTS = 8;
    /** Presupuesto por actualización; nunca descarta pendientes de oleadas grandes. */
    static final int MAX_SPAWNS_PER_UPDATE = 64;
    private static final double MIN_RETRY_INTERVAL_SECONDS = .05;
    private static final double EDGE_MARGIN = 0.5;
    private static final double PLAYER_SPAWN_CLEARANCE = 2.5;

    public enum Phase { WAITING, SPAWNING, ACTIVE, PAUSE }

    private final WaveRules rules;
    private final long seed;
    private final EnemyUpdateService enemies;
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;
    private final Set<Zombie> waveZombies = Collections.newSetFromMap(new IdentityHashMap<>());

    private Phase phase = Phase.WAITING;
    private int wave;
    private int pendingSpawns;
    private long spawnIndex;
    private int failedSpawns;
    private double countdown;

    public HordeManager(WaveRules rules, World world, EnemyUpdateService enemies) {
        this(rules, Objects.requireNonNull(world, "world no puede ser null").getSeed(), world, enemies);
    }

    public HordeManager(WaveRules rules, long seed, World world, EnemyUpdateService enemies) {
        this.rules = Objects.requireNonNull(rules, "rules no puede ser null");
        this.enemies = Objects.requireNonNull(enemies, "enemies no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
        if (world.getChunks().isEmpty()) {
            throw new IllegalArgumentException("El mundo no tiene chunks donde generar oleadas");
        }
        this.seed = seed;
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (Chunk chunk : world.getChunks()) {
            minChunkX = Math.min(minChunkX, chunk.getChunkX());
            maxChunkX = Math.max(maxChunkX, chunk.getChunkX());
            minChunkZ = Math.min(minChunkZ, chunk.getChunkZ());
            maxChunkZ = Math.max(maxChunkZ, chunk.getChunkZ());
        }
        this.minX = (double) minChunkX * Chunk.WIDTH + EDGE_MARGIN;
        this.maxX = (double) (maxChunkX + 1) * Chunk.WIDTH - EDGE_MARGIN;
        this.minZ = (double) minChunkZ * Chunk.DEPTH + EDGE_MARGIN;
        this.maxZ = (double) (maxChunkZ + 1) * Chunk.DEPTH - EDGE_MARGIN;
        this.countdown = rules.firstWaveDelaySeconds();
    }

    /**
     * Avanza el tiempo de las oleadas. Con enemigos desactivados no ocurre nada: ni cuenta atrás
     * ni apariciones.
     */
    public void update(double deltaSeconds, double playerX, double playerZ) {
        if (!enemies.isEnabled() || deltaSeconds <= 0) {
            return;
        }
        if (phase == Phase.WAITING || phase == Phase.PAUSE) {
            countdown -= deltaSeconds;
            if (countdown > 0) {
                return;
            }
            startNextWave();
        } else if (phase == Phase.SPAWNING) {
            countdown -= deltaSeconds;
        }
        if (phase == Phase.SPAWNING) {
            int budget = MAX_SPAWNS_PER_UPDATE;
            while (pendingSpawns > 0 && countdown <= 0 && budget-- > 0) {
                if (!spawnOne(playerX, playerZ)) {
                    // La congestión no reduce la cantidad: probar otros sitios en el siguiente intervalo.
                    countdown = Math.max(rules.spawnIntervalSeconds(), MIN_RETRY_INTERVAL_SECONDS);
                    break;
                }
                pendingSpawns--;
                countdown += rules.spawnIntervalSeconds();
            }
            if (pendingSpawns > 0) {
                return;
            }
            phase = Phase.ACTIVE;
        }
        if (phase == Phase.ACTIVE) {
            waveZombies.retainAll(sessionZombies());
            if (waveZombies.isEmpty()) {
                phase = Phase.PAUSE;
                countdown = rules.pauseSeconds();
            }
        }
    }

    /** Los zombis que siguen registrados; un zombi desaparece de aquí tras morir y hacer despawn. */
    private Set<Zombie> sessionZombies() {
        Set<Zombie> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        alive.addAll(enemies.zombies());
        return alive;
    }

    private void startNextWave() {
        if (wave < Integer.MAX_VALUE) wave++;
        pendingSpawns = rules.countFor(wave);
        spawnIndex = 0;
        phase = Phase.SPAWNING;
        // El primer zombi de la oleada aparece en el mismo fotograma en que empieza.
        countdown = 0;
    }

    /** Busca una columna caminable en el anillo alrededor del jugador, siempre con la misma secuencia. */
    private boolean spawnOne(double playerX, double playerZ) {
        SplittableRandom random = new SplittableRandom(spawnSeed(wave, spawnIndex++));
        double span = rules.maxSpawnDistance() - rules.minSpawnDistance();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = rules.minSpawnDistance() + random.nextDouble() * span;
            // Project along the ray to the boundary, avoiding clamped corner piles.
            double dx = Math.cos(angle) * distance, dz = Math.sin(angle) * distance;
            double originX = clamp(playerX, minX, maxX), originZ = clamp(playerZ, minZ, maxZ);
            double fraction = 1.0;
            if (dx > 0) fraction = Math.min(fraction, (maxX - originX) / dx);
            if (dx < 0) fraction = Math.min(fraction, (minX - originX) / dx);
            if (dz > 0) fraction = Math.min(fraction, (maxZ - originZ) / dz);
            if (dz < 0) fraction = Math.min(fraction, (minZ - originZ) / dz);
            double x = clamp(originX + dx * fraction, minX, maxX);
            double z = clamp(originZ + dz * fraction, minZ, maxZ);
            if (Math.hypot(x - playerX, z - playerZ) < PLAYER_SPAWN_CLEARANCE) {
                continue;
            }
            if (enemies.spawnAt(x, z).map(waveZombies::add).isPresent()) {
                return true;
            }
        }
        if (failedSpawns < Integer.MAX_VALUE) failedSpawns++;
        return false;
    }

    private long spawnSeed(int waveNumber, long index) {
        return seed ^ (waveNumber * 0x9E3779B97F4A7C15L) ^ (index * 0xC2B2AE3D27D4EB4FL);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public Phase phase() {
        return phase;
    }

    /** Número de la oleada actual o de la última terminada; 0 antes de la primera. */
    public int wave() {
        return wave;
    }

    /** Zombis que la oleada actual todavía debe generar. */
    public int pendingSpawns() {
        return pendingSpawns;
    }

    /** Zombis de la oleada actual que siguen en la sesión, vivos o en su despawn. */
    public int remainingInWave() {
        return waveZombies.size();
    }

    /** Lotes de intentos fallidos: la aparición sigue pendiente y se reintenta, nunca se pierde. */
    public int failedSpawns() {
        return failedSpawns;
    }

    /** Segundos hasta la próxima oleada en WAITING o PAUSE; 0 mientras una oleada está en curso. */
    public double secondsToNextWave() {
        return phase == Phase.WAITING || phase == Phase.PAUSE ? Math.max(0, countdown) : 0;
    }
}
