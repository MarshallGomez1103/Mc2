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
 * java -Xms1g -Xmx1g -Dmc2.load.out=DIR -cp ... loadtest.EnemyLoadHarness REC_STRESS [minutos reales máx.]
 * </pre>
 *
 * <p>REC_BASELINE y REC_STRESS siguen el protocolo de la recuperación (docs/recuperacion-c2/carga.md):
 * mismo mundo 10×10, seed y dificultad VERY_HARD; 3 repeticiones de 30 s simulados por nivel. Los
 * scripts de {@code perf/} los lanzan y guardan el manifiesto. Un tick fallido no entra en la
 * latencia: se cuenta en {@code failed_ticks} y en {@code error_rate = failed_ticks / frames}.
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
    private static final int RECOVERY_CHUNKS = 10;
    private static final double RECOVERY_SECONDS = 30.0;
    private static final int RECOVERY_REPETITIONS = 3;
    private static final double RECOVERY_WALL_MINUTES = 20.0;

    private EnemyLoadHarness() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Uso: EnemyLoadHarness BASELINE|PEAK|STRESS|ENDURANCE|FUNCTIONAL20|REC_BASELINE|REC_STRESS"
                    + " [minutos: duración de ENDURANCE o límite real de REC_*]");
            System.exit(2);
        }
        String scenario = args[0].toUpperCase(Locale.ROOT);
        double minutesArg = args.length > 1 ? Double.parseDouble(args[1]) : Double.NaN;
        if (scenario.startsWith("REC_") && args.length > 1
                && (!Double.isFinite(minutesArg) || minutesArg <= 0)) {
            throw new IllegalArgumentException("El límite real debe ser finito y positivo");
        }
        String outDir = System.getProperty("mc2.load.out");
        Path csv;
        if (outDir != null && !outDir.isBlank()) {
            Files.createDirectories(Path.of(outDir));
            csv = Path.of(outDir, "results.csv");
        } else {
            Path results = Path.of("target", "load-results");
            Files.createDirectories(results);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            csv = results.resolve(scenario.toLowerCase(Locale.ROOT) + "-" + stamp + ".csv");
        }
        printEnvironment(scenario);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv, StandardCharsets.UTF_8))) {
            switch (scenario) {
                case "FUNCTIONAL20" -> constantLoad(out, 2, Difficulty.VERY_HARD, new int[]{20}, 60, 1, false);
                case "BASELINE" -> constantLoad(out, 2, Difficulty.NORMAL, new int[]{3}, 60, 5, false);
                case "PEAK" -> constantLoad(out, 10, Difficulty.VERY_HARD, new int[]{25}, 60, 3, false);
                case "STRESS" -> constantLoad(out, 10, Difficulty.VERY_HARD,
                        new int[]{10, 20, 40, 80, 160, 320, 640}, 30, 1, true);
                case "ENDURANCE" -> endurance(out, Double.isNaN(minutesArg) ? 30.0 : minutesArg);
                case "REC_BASELINE" -> recoveryLoad(out, new int[]{3}, false,
                        Double.isNaN(minutesArg) ? RECOVERY_WALL_MINUTES : minutesArg);
                case "REC_STRESS" -> recoveryLoad(out, new int[]{20, 40, 80, 160, 320}, true,
                        Double.isNaN(minutesArg) ? RECOVERY_WALL_MINUTES : minutesArg);
                default -> {
                    System.err.println("Escenario desconocido: " + scenario);
                    System.exit(2);
                }
            }
        }
        // Ruta relativa o solo el nombre: la consola se publica y no debe llevar rutas del equipo.
        System.out.println("CSV: " + (csv.isAbsolute() ? csv.getFileName() : csv));
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
     * Protocolo de la recuperación: mundo 10×10, VERY_HARD, 3 × 30 s simulados por nivel tras 10 s de
     * calentamiento. Con {@code stopOnBudget} no sube de nivel si la mediana del p95 supera el
     * presupuesto o hubo ticks fallidos. Se detiene también al agotar {@code wallMinutes} de reloj.
     */
    private static void recoveryLoad(PrintWriter out, int[] populations, boolean stopOnBudget, double wallMinutes) {
        Difficulty difficulty = Difficulty.VERY_HARD;
        System.out.printf(Locale.ROOT, "Protocolo REC: %dx%d chunks · %s · %d rep × %.0f s simulados · "
                        + "calentamiento 10 s · límite real %.2f min · presupuesto p95 %.2f ms%n",
                RECOVERY_CHUNKS, RECOVERY_CHUNKS, difficulty, RECOVERY_REPETITIONS, RECOVERY_SECONDS,
                wallMinutes, FRAME_BUDGET_MS);
        out.println("chunks,difficulty,zombies,repetition,sim_seconds,frames,wall_seconds,update_mean_ms,"
                + "update_p95_ms,update_max_ms,ticks_per_wall_s,update_capacity_ticks_per_s,"
                + "zombie_updates_per_wall_s,astar_calls,astar_calls_per_sim_s,astar_expansions,avg_active,"
                + "heap_mb,player_deaths,failed_ticks,error_rate,min_active,population_shortfall_ticks");
        long deadline = System.nanoTime() + (long) (wallMinutes * 60e9);
        for (int population : populations) {
            new Session(RECOVERY_CHUNKS, difficulty, false).runConstant(population, 10);
            double[] p95s = new double[RECOVERY_REPETITIONS];
            int failedInLevel = 0;
            int shortfallsInLevel = 0;
            for (int repetition = 1; repetition <= RECOVERY_REPETITIONS; repetition++) {
                if (System.nanoTime() > deadline) {
                    System.out.printf(Locale.ROOT, "Límite real de %.2f min alcanzado antes de %d zombis · rep %d.%n",
                            wallMinutes, population, repetition);
                    return;
                }
                Session session = new Session(RECOVERY_CHUNKS, difficulty, false);
                Stats stats = session.runConstant(population, RECOVERY_SECONDS);
                long heap = session.heapAfterGc();
                double wall = stats.wallNanos / 1e9;
                double p95 = stats.percentileMs(95);
                p95s[repetition - 1] = p95;
                failedInLevel += stats.errors;
                shortfallsInLevel += stats.populationShortfallTicks;
                out.printf(Locale.ROOT, "%d,%s,%d,%d,%.0f,%d,%.3f,%.4f,%.4f,%.4f,%.1f,%.1f,%.0f,%d,%.2f,%d,%.1f,%d,%d,%d,%.6f,%d,%d%n",
                        RECOVERY_CHUNKS * RECOVERY_CHUNKS, difficulty, population, repetition, RECOVERY_SECONDS,
                        stats.attempted, wall, stats.meanMs(), p95, stats.maxMs(),
                        stats.attempted / wall, stats.capacityTicksPerSecond(), stats.activeSum / wall,
                        stats.searches, stats.searches / RECOVERY_SECONDS, stats.expansions, stats.averageActive(),
                        heap, stats.playerDeaths, stats.errors, stats.errorRate(), stats.minActive, stats.populationShortfallTicks);
                out.flush();
                System.out.printf(Locale.ROOT,
                        "%3d zombis · rep %d · %.2f s reales · media %.3f ms · p95 %.3f ms · máx %.3f ms · "
                                + "%.0f ticks/s · A* %d · heap %d MB · fallidos %d (%.4f%%)%n",
                        population, repetition, wall, stats.meanMs(), p95, stats.maxMs(), stats.attempted / wall,
                        stats.searches, heap, stats.errors, stats.errorRate() * 100);
            }
            Arrays.sort(p95s);
            double medianP95 = p95s[p95s.length / 2];
            System.out.printf(Locale.ROOT, "Nivel %d zombis: mediana p95 %.3f ms · fallidos %d · déficit de población %d%n",
                    population, medianP95, failedInLevel, shortfallsInLevel);
            if (stopOnBudget && (medianP95 > FRAME_BUDGET_MS || failedInLevel > 0 || shortfallsInLevel > 0)) {
                System.out.printf(Locale.ROOT, "Parada: nivel %d supera el presupuesto, tuvo fallos o no sostuvo la población.%n", population);
                return;
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
    static final class Session {
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
            long start = System.nanoTime();
            for (int frame = 0; frame < frames; frame++) {
                refill(population);
                if (activeCount() < population) stats.populationShortfallTicks++;
                frame(stats);
            }
            stats.wallNanos = System.nanoTime() - start;
            return stats;
        }

        /** Repone zombis desaparecidos para mantener constante la población objetivo. */
        void refill(int population) {
            int attempts = 0;
            int active = activeCount();
            while (active < population && attempts++ < population * 4) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
                Player player = world.getPlayer();
                if (enemies.spawnAt(player.getX() + Math.cos(angle) * distance,
                        player.getZ() + Math.sin(angle) * distance).isPresent()) {
                    spawnedTotal++;
                    active++;
                }
            }
        }

        int activeCount() {
            return (int) enemies.zombies().stream().filter(Zombie::isAlive).count();
        }

        void frame(Stats stats) {
            movePlayer();
            int activeBeforeUpdate = activeCount();
            int searchesBefore = pathfinder.getSearchCount();
            long expansionsBefore = pathfinder.getTotalExpansions();
            long start = System.nanoTime();
            boolean failed = false;
            try {
                if (horde != null) {
                    horde.update(FRAME, world.getPlayer().getX(), world.getPlayer().getZ());
                }
                enemies.update(life, FRAME);
            } catch (RuntimeException failure) {
                failed = true;
                stats.errors++;
                if (stats.errors == 1) {
                    failure.printStackTrace();
                }
            }
            long elapsed = System.nanoTime() - start;
            // Un tick fallido no terminó su trabajo: no entra en la latencia, solo en el conteo de errores.
            stats.record(failed ? -1 : elapsed, activeBeforeUpdate);
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

    /**
     * Tiempos por fotograma y contadores de un tramo de simulación. {@code frames} son las muestras de
     * latencia (ticks correctos) y {@code attempted} todos los ticks, incluidos los fallidos.
     */
    static final class Stats {
        long[] nanos = new long[4096];
        int frames;
        int attempted;
        long activeSum;
        long updateNanos;
        long wallNanos;
        long searches;
        long expansions;
        int playerDeaths;
        int errors;
        int minActive = Integer.MAX_VALUE;
        int populationShortfallTicks;

        /** {@code elapsed < 0} marca un tick fallido: cuenta como intentado pero no como muestra. */
        void record(long elapsed, int active) {
            attempted++;
            activeSum += active;
            minActive = Math.min(minActive, active);
            if (elapsed < 0) {
                return;
            }
            if (frames == nanos.length) {
                nanos = Arrays.copyOf(nanos, frames * 2);
            }
            nanos[frames++] = elapsed;
            updateNanos += elapsed;
        }

        double capacityTicksPerSecond() {
            return updateNanos == 0 ? 0 : frames / (updateNanos / 1e9);
        }

        double errorRate() {
            return attempted == 0 ? 0 : errors / (double) attempted;
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
            return attempted == 0 ? 0 : activeSum / (double) attempted;
        }
    }
}
