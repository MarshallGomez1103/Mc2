package loadtest;

import application.ChunkGenerationService;
import application.EnemyUpdateService;
import application.HordeManager;
import domain.enemy.AStarPathfinder;
import domain.enemy.Difficulty;
import domain.enemy.Zombie;
import domain.enemy.ZombieState;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.Chunk;
import domain.world.SimpleTerrainGenerator;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Harness de carga headless de la IA de enemigos (C2-10). No es un test JUnit: queda fuera del gate
 * {@code mvn clean test} y se lanza a mano después de compilar.
 *
 * <pre>
 * mvn test-compile
 * java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness BASELINE
 * java -cp "target/classes;target/test-classes" loadtest.EnemyLoadHarness ENDURANCE 30
 * </pre>
 *
 * <p>Simula fotogramas de 1/60 s tan rápido como puede. Mide el tiempo real (wall-clock) de
 * {@code HordeManager.update + EnemyUpdateService.update} por fotograma, las búsquedas A*, los
 * zombis activos y el heap tras {@code System.gc()}. El jugador recorre un círculo para forzar
 * repaths y, si un zombi lo mata, reaparece en el acto para que la carga no se detenga.
 * No mide FPS, GPU ni memoria nativa: aquí no hay ventana.
 */
public final class EnemyLoadHarness {
    private static final double FRAME = 1.0 / 60.0;
    private static final double FRAME_BUDGET_MS = 1000.0 / 60.0;
    private static final long SEED = 20260925L;
    private static final double PLAYER_ORBIT_RADIUS = 6.0;
    private static final double PLAYER_SPEED = 2.0;
    private static final double RING_MIN = 8.0;
    private static final double RING_MAX = 18.0;

    private EnemyLoadHarness() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Uso: EnemyLoadHarness BASELINE|PEAK|STRESS|ENDURANCE [minutos para ENDURANCE]");
            System.exit(2);
        }
        String scenario = args[0].toUpperCase(Locale.ROOT);
        Path results = Path.of("target", "load-results");
        Files.createDirectories(results);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path csv = results.resolve(scenario.toLowerCase(Locale.ROOT) + "-" + stamp + ".csv");

        printEnvironment(scenario);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv, StandardCharsets.UTF_8))) {
            switch (scenario) {
                case "BASELINE" -> constantLoad(out, 2, Difficulty.NORMAL, new int[]{3}, 60, 5, false);
                case "PEAK" -> constantLoad(out, 10, Difficulty.VERY_HARD, new int[]{25}, 60, 3, false);
                case "STRESS" -> constantLoad(out, 10, Difficulty.VERY_HARD,
                        new int[]{10, 20, 40, 80, 160, 320, 640}, 30, 1, true);
                case "ENDURANCE" -> endurance(out, args.length > 1 ? Double.parseDouble(args[1]) : 30.0);
                default -> {
                    System.err.println("Escenario desconocido: " + scenario);
                    System.exit(2);
                }
            }
        }
        System.out.println("CSV: " + csv.toAbsolutePath());
    }

    private static void printEnvironment(String scenario) {
        Runtime runtime = Runtime.getRuntime();
        System.out.printf(Locale.ROOT, "Escenario %s · seed %d%n", scenario, SEED);
        System.out.printf(Locale.ROOT, "JVM %s %s · %s %s · %d CPUs · heap máx. %d MB%n",
                System.getProperty("java.vendor"), System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                runtime.availableProcessors(), runtime.maxMemory() / (1024 * 1024));
    }

    // ------------------------------------------------------------------ escenarios

    /**
     * Mantiene una población fija de zombis durante {@code seconds} simulados. Con {@code stopOnBudget}
     * sube escalón a escalón y se detiene cuando el p95 supera el presupuesto de un fotograma.
     */
    private static void constantLoad(PrintWriter out, int chunksPerSide, Difficulty difficulty, int[] populations,
                                     double seconds, int repetitions, boolean stopOnBudget) {
        out.println("chunks,difficulty,zombies,repetition,sim_seconds,frames,update_mean_ms,update_p95_ms,"
                + "update_max_ms,astar_calls,astar_calls_per_sim_s,astar_expansions,avg_active,heap_mb,"
                + "player_deaths,errors");
        for (int population : populations) {
            // Calentamiento del JIT con la misma configuración, sin registrar.
            new Session(chunksPerSide, difficulty, false).runConstant(population, 10);
            for (int repetition = 1; repetition <= repetitions; repetition++) {
                Session session = new Session(chunksPerSide, difficulty, false);
                Stats stats = session.runConstant(population, seconds);
                long heap = session.heapAfterGc();
                out.printf(Locale.ROOT, "%d,%s,%d,%d,%.0f,%d,%.4f,%.4f,%.4f,%d,%.2f,%d,%.1f,%d,%d,%d%n",
                        chunksPerSide * chunksPerSide, difficulty, population, repetition, seconds,
                        stats.frames, stats.meanMs(), stats.percentileMs(95), stats.maxMs(), stats.searches,
                        stats.searches / seconds, stats.expansions, stats.averageActive(), heap,
                        stats.playerDeaths, stats.errors);
                out.flush();
                System.out.printf(Locale.ROOT,
                        "%3d zombis · rep %d · update media %.3f ms · p95 %.3f ms · máx %.3f ms · A* %d (%.1f/s) · "
                                + "heap %d MB · muertes %d · errores %d%n",
                        population, repetition, stats.meanMs(), stats.percentileMs(95), stats.maxMs(),
                        stats.searches, stats.searches / seconds, heap, stats.playerDeaths, stats.errors);
                if (stopOnBudget && stats.percentileMs(95) > FRAME_BUDGET_MS) {
                    System.out.printf(Locale.ROOT, "Degradación: p95 %.2f ms > %.2f ms con %d zombis.%n",
                            stats.percentileMs(95), FRAME_BUDGET_MS, population);
                    return;
                }
            }
        }
    }

    /**
     * Oleadas reales de HordeManager durante {@code minutes} de reloj. Cada zombi muere a los 15 s
     * simulados para recorrer sin pausa el ciclo spawn → update → muerte → despawn → oleada.
     */
    private static void endurance(PrintWriter out, double minutes) {
        out.println("minute,sim_seconds,wave,active_zombies,spawned_total,update_mean_ms,update_p95_ms,"
                + "update_max_ms,astar_calls,heap_mb,player_deaths,errors");
        Session session = new Session(10, Difficulty.VERY_HARD, true);
        long end = System.nanoTime() + (long) (minutes * 60e9);
        int minute = 0;
        while (System.nanoTime() < end) {
            long minuteEnd = Math.min(end, System.nanoTime() + 60_000_000_000L);
            Stats stats = new Stats();
            while (System.nanoTime() < minuteEnd) {
                session.frame(stats);
            }
            minute++;
            double mean = stats.meanMs();
            double p95 = stats.percentileMs(95);
            double max = stats.maxMs();
            // Los tiempos de un minuto ocupan decenas de MB: se sueltan antes de medir el heap.
            stats.nanos = null;
            long heap = session.heapAfterGc();
            out.printf(Locale.ROOT, "%d,%.0f,%d,%d,%d,%.4f,%.4f,%.4f,%d,%d,%d,%d%n",
                    minute, session.simSeconds, session.horde.wave(), session.enemies.zombies().size(),
                    session.spawnedTotal, mean, p95, max, stats.searches,
                    heap, stats.playerDeaths, stats.errors);
            out.flush();
            System.out.printf(Locale.ROOT,
                    "min %d · %.0f s simulados · oleada %d · activos %d · generados %d · p95 %.3f ms · heap %d MB · "
                            + "errores %d%n",
                    minute, session.simSeconds, session.horde.wave(), session.enemies.zombies().size(),
                    session.spawnedTotal, p95, heap, stats.errors);
        }
    }

    // ------------------------------------------------------------------ sesión simulada

    /** Mundo generado con la seed fija, servicio de IA, horda opcional y un jugador en órbita. */
    private static final class Session {
        final World world = new World("carga", SEED, Instant.parse("2026-09-25T00:00:00Z"));
        final SimpleTerrainGenerator terrain = new SimpleTerrainGenerator(SEED);
        final AStarPathfinder pathfinder = new AStarPathfinder();
        final EnemyUpdateService enemies;
        final HordeManager horde;
        final PlayerLife life;
        final Map<Zombie, Double> ages = new IdentityHashMap<>();
        final SplittableRandom random = new SplittableRandom(SEED);
        final double centerX;
        final double centerZ;
        double simSeconds;
        long spawnedTotal;

        Session(int chunksPerSide, Difficulty difficulty, boolean withHorde) {
            new ChunkGenerationService(terrain, new BlockFactory())
                    .generateChunks(chunksPerSide, chunksPerSide).forEach(world::addChunk);
            centerX = chunksPerSide * Chunk.WIDTH / 2.0;
            centerZ = chunksPerSide * Chunk.DEPTH / 2.0;
            enemies = new EnemyUpdateService(world, difficulty.zombieParameters(), pathfinder);
            horde = withHorde ? new HordeManager(difficulty.waveRules(), world, enemies) : null;
            life = new PlayerLife(world.getPlayer());
            movePlayer();
        }

        Stats runConstant(int population, double seconds) {
            Stats stats = new Stats();
            int frames = (int) Math.round(seconds / FRAME);
            for (int frame = 0; frame < frames; frame++) {
                refill(population);
                frame(stats);
            }
            return stats;
        }

        /** Repone zombis desaparecidos para mantener constante la población objetivo. */
        void refill(int population) {
            int attempts = 0;
            while (enemies.zombies().size() < population && attempts++ < population * 4) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
                Player player = world.getPlayer();
                enemies.spawnAt(player.getX() + Math.cos(angle) * distance, player.getZ() + Math.sin(angle) * distance)
                        .ifPresent(zombie -> spawnedTotal++);
            }
        }

        void frame(Stats stats) {
            movePlayer();
            int searchesBefore = pathfinder.getSearchCount();
            long expansionsBefore = pathfinder.getTotalExpansions();
            long start = System.nanoTime();
            try {
                if (horde != null) {
                    horde.update(FRAME, world.getPlayer().getX(), world.getPlayer().getZ());
                }
                enemies.update(life, FRAME);
            } catch (RuntimeException failure) {
                stats.errors++;
                if (stats.errors == 1) {
                    failure.printStackTrace();
                }
            }
            stats.record(System.nanoTime() - start, enemies.zombies().size());
            stats.searches += pathfinder.getSearchCount() - searchesBefore;
            stats.expansions += pathfinder.getTotalExpansions() - expansionsBefore;
            if (horde != null) {
                ageAndKill();
            }
            if (life.isDead()) {
                stats.playerDeaths++;
                life.respawn();
            }
            simSeconds += FRAME;
        }

        /** En resistencia cada zombi muere a los 15 s para que el ciclo completo no se detenga. */
        void ageAndKill() {
            ages.keySet().removeIf(zombie -> !enemies.zombies().contains(zombie));
            for (Zombie zombie : enemies.zombies()) {
                if (!ages.containsKey(zombie)) {
                    spawnedTotal++;
                }
                double age = ages.merge(zombie, FRAME, Double::sum);
                if (age >= 15.0 && zombie.getState() != ZombieState.DEAD) {
                    zombie.takeDamage(zombie.getHealth());
                }
            }
        }

        /** El jugador recorre un círculo alrededor del centro del mundo, siempre sobre la superficie. */
        void movePlayer() {
            double angle = simSeconds * PLAYER_SPEED / PLAYER_ORBIT_RADIUS;
            double x = centerX + Math.cos(angle) * PLAYER_ORBIT_RADIUS;
            double z = centerZ + Math.sin(angle) * PLAYER_ORBIT_RADIUS;
            Player player = world.getPlayer();
            player.setX(x);
            player.setZ(z);
            player.setY(terrain.surfaceHeightAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0);
        }

        long heapAfterGc() {
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        }
    }

    /** Tiempos por fotograma y contadores de un tramo de simulación. */
    private static final class Stats {
        long[] nanos = new long[4096];
        int frames;
        long activeSum;
        long searches;
        long expansions;
        int playerDeaths;
        int errors;

        void record(long elapsed, int active) {
            if (frames == nanos.length) {
                nanos = Arrays.copyOf(nanos, frames * 2);
            }
            nanos[frames++] = elapsed;
            activeSum += active;
        }

        double meanMs() {
            long total = 0;
            for (int i = 0; i < frames; i++) {
                total += nanos[i];
            }
            return frames == 0 ? 0 : total / (double) frames / 1e6;
        }

        double percentileMs(int percentile) {
            if (frames == 0) {
                return 0;
            }
            long[] sorted = Arrays.copyOf(nanos, frames);
            Arrays.sort(sorted);
            int index = (int) Math.ceil(percentile / 100.0 * frames) - 1;
            return sorted[Math.max(0, index)] / 1e6;
        }

        double maxMs() {
            long max = 0;
            for (int i = 0; i < frames; i++) {
                max = Math.max(max, nanos[i]);
            }
            return max / 1e6;
        }

        double averageActive() {
            return frames == 0 ? 0 : activeSum / (double) frames;
        }
    }
}
