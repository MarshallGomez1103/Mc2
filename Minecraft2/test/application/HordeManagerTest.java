package application;

import domain.enemy.TestWorlds;
import domain.enemy.WaveRules;
import domain.enemy.Zombie;
import domain.enemy.ZombieParameters;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.Chunk;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static domain.enemy.TestWorlds.GROUND_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Oleadas con tiempo explícito sobre un mundo plano de 64×64. Los pasos de 0.25 s son exactos en
 * binario, así que los límites de tiempo se comprueban sin tolerancias.
 */
class HordeManagerTest {
    private static final double STEP = 0.25;
    private static final double CENTER = 32.5;
    /** Espera 2 s, oleadas de 2, 3, 4 (tope 4), un zombi cada 0.5 s, pausa 3 s, anillo 8–12. */
    private static final WaveRules RULES = new WaveRules(2.0, 2, 1, 4, 0.5, 3.0, 8.0, 12.0);

    private final World world = TestWorlds.flat(4, 4);
    private final Player player = world.getPlayer();
    private final PlayerLife life = new PlayerLife(player);
    private final EnemyUpdateService enemies = new EnemyUpdateService(world, ZombieParameters.defaults());
    private final HordeManager horde = new HordeManager(RULES, 99L, world, enemies);

    HordeManagerTest() {
        player.setX(CENTER);
        player.setY(GROUND_Y + 1);
        player.setZ(CENTER);
    }

    private static void advance(HordeManager manager, double seconds) {
        for (double elapsed = 0; elapsed < seconds; elapsed += STEP) {
            manager.update(STEP, CENTER, CENTER);
        }
    }

    private void advance(double seconds) {
        advance(horde, seconds);
    }

    /** Mata a todos los zombis y deja pasar su despawn, como haría el juego. */
    private void killEveryoneAndDespawn() {
        enemies.zombies().forEach(zombie -> zombie.takeDamage(zombie.getHealth()));
        for (int frame = 0; frame < 60; frame++) {
            enemies.update(life, 1.0 / 60.0);
        }
    }

    @Test
    void nothingHappensBeforeTheFirstWaveDelay() {
        advance(1.75);

        assertEquals(HordeManager.Phase.WAITING, horde.phase());
        assertEquals(0, horde.wave());
        assertEquals(0, enemies.zombies().size());
        assertEquals(0.25, horde.secondsToNextWave());
    }

    @Test
    void firstWaveSpawnsBaseCountOneIntervalApart() {
        advance(2.0);
        assertEquals(1, horde.wave());
        assertEquals(HordeManager.Phase.SPAWNING, horde.phase());
        assertEquals(1, enemies.zombies().size(), "el primero aparece al empezar la oleada");
        assertEquals(1, horde.pendingSpawns());

        advance(0.25);
        assertEquals(1, enemies.zombies().size(), "todavía no pasó el intervalo de 0.5 s");

        advance(0.25);
        assertEquals(2, enemies.zombies().size());
        assertEquals(0, horde.pendingSpawns());
        assertEquals(HordeManager.Phase.ACTIVE, horde.phase());
        assertEquals(0.0, horde.secondsToNextWave(), "con una oleada en curso no hay cuenta atrás");
    }

    @Test
    void waveLastsUntilEveryZombieDiedAndDespawned() {
        advance(2.5);
        List<Zombie> wave = new ArrayList<>(enemies.zombies());

        wave.get(0).takeDamage(wave.get(0).getHealth());
        killDespawnFrames(60);
        advance(STEP);
        assertEquals(HordeManager.Phase.ACTIVE, horde.phase(), "queda un zombi vivo");
        assertEquals(1, horde.remainingInWave());

        wave.get(1).takeDamage(wave.get(1).getHealth());
        killDespawnFrames(30);
        advance(STEP);
        assertEquals(HordeManager.Phase.ACTIVE, horde.phase(), "el último zombi aún está en su despawn");

        killDespawnFrames(30);
        advance(STEP);
        assertEquals(HordeManager.Phase.PAUSE, horde.phase());
        assertEquals(RULES.pauseSeconds(), horde.secondsToNextWave());
    }

    private void killDespawnFrames(int frames) {
        for (int frame = 0; frame < frames; frame++) {
            enemies.update(life, 1.0 / 60.0);
        }
    }

    @Test
    void nextWavesBringExtraZombiesUpToTheCap() {
        int[] expected = {2, 3, 4, 4};
        for (int wave = 1; wave <= expected.length; wave++) {
            advance(wave == 1 ? RULES.firstWaveDelaySeconds() : RULES.pauseSeconds());
            advance(RULES.spawnIntervalSeconds() * (expected[wave - 1] - 1));

            assertEquals(wave, horde.wave());
            assertEquals(expected[wave - 1], enemies.zombies().size(), "oleada " + wave);
            assertEquals(HordeManager.Phase.ACTIVE, horde.phase());

            killEveryoneAndDespawn();
            advance(STEP);
            assertEquals(HordeManager.Phase.PAUSE, horde.phase());
        }
    }

    @Test
    void pauseDoesNotStartTheNextWaveEarly() {
        advance(2.5);
        killEveryoneAndDespawn();
        advance(STEP);
        advance(RULES.pauseSeconds() - STEP);

        assertEquals(HordeManager.Phase.PAUSE, horde.phase());
        assertEquals(1, horde.wave());
        assertEquals(0, enemies.zombies().size());

        advance(STEP);
        assertEquals(2, horde.wave());
        assertEquals(1, enemies.zombies().size());
    }

    @Test
    void sameSeedSpawnsInTheSamePlacesAndOtherSeedDoesNot() {
        List<double[]> first = spawnPositions(123L);
        List<double[]> repeated = spawnPositions(123L);
        List<double[]> other = spawnPositions(124L);

        assertEquals(first.size(), repeated.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i)[0], repeated.get(i)[0]);
            assertEquals(first.get(i)[1], repeated.get(i)[1]);
        }
        boolean differs = false;
        for (int i = 0; i < first.size(); i++) {
            differs |= first.get(i)[0] != other.get(i)[0] || first.get(i)[1] != other.get(i)[1];
        }
        assertTrue(differs, "otra seed debe producir otras posiciones");
    }

    private static List<double[]> spawnPositions(long seed) {
        World fresh = TestWorlds.flat(4, 4);
        EnemyUpdateService service = new EnemyUpdateService(fresh, ZombieParameters.defaults());
        HordeManager manager = new HordeManager(RULES, seed, fresh, service);
        advance(manager, 2.5);
        List<double[]> positions = new ArrayList<>();
        service.zombies().forEach(zombie -> positions.add(new double[]{zombie.getX(), zombie.getZ()}));
        return positions;
    }

    @Test
    void spawnsLandOnTheRingAroundThePlayer() {
        for (int wave = 1; wave <= 3; wave++) {
            advance(wave == 1 ? RULES.firstWaveDelaySeconds() : RULES.pauseSeconds());
            advance(2.0);
            for (Zombie zombie : enemies.zombies()) {
                double distance = Math.hypot(zombie.getX() - CENTER, zombie.getZ() - CENTER);
                assertTrue(distance >= RULES.minSpawnDistance() && distance <= RULES.maxSpawnDistance(),
                        "distancia fuera del anillo: " + distance);
                assertEquals(GROUND_Y + 1, zombie.getY(), "los pies sobre el suelo");
            }
            killEveryoneAndDespawn();
            advance(STEP);
        }
    }

    @Test
    void spawnsNearTheWorldEdgeStayInsideTheWorld() {
        World small = TestWorlds.flat(1, 1);
        EnemyUpdateService service = new EnemyUpdateService(small, ZombieParameters.defaults());
        HordeManager manager = new HordeManager(new WaveRules(0.0, 4, 0, 4, 0.0, 1.0, 8.0, 12.0), 5L, small, service);

        manager.update(STEP, 1.0, 1.0);

        assertEquals(4, service.zombies().size());
        assertEquals(0, manager.failedSpawns());
        for (Zombie zombie : service.zombies()) {
            assertTrue(zombie.getX() > 0 && zombie.getX() < Chunk.WIDTH, "x=" + zombie.getX());
            assertTrue(zombie.getZ() > 0 && zombie.getZ() < Chunk.DEPTH, "z=" + zombie.getZ());
        }
    }

    @Test
    void enemiesOffFreezesTheHorde() {
        enemies.setEnabled(false);

        advance(30.0);

        assertEquals(0, enemies.zombies().size());
        assertEquals(0, horde.wave());
        assertEquals(RULES.firstWaveDelaySeconds(), horde.secondsToNextWave(), "ni siquiera corre la espera");

        enemies.setEnabled(true);
        advance(2.0);
        assertEquals(1, horde.wave());
    }

    @Test
    void worldWithoutWalkableGroundDoesNotBlockTheWaves() {
        World empty = new World("vacio", 1L, Instant.parse("2026-09-25T00:00:00Z"));
        empty.addChunk(new Chunk(0, 0));
        EnemyUpdateService service = new EnemyUpdateService(empty, ZombieParameters.defaults());
        HordeManager manager = new HordeManager(RULES, empty, service);

        advance(manager, 2.5);
        assertEquals(0, service.zombies().size());
        assertEquals(2, manager.failedSpawns());
        assertEquals(HordeManager.Phase.PAUSE, manager.phase(), "una oleada sin zombis termina");

        advance(manager, RULES.pauseSeconds() + 1.0);
        assertEquals(2, manager.wave());
    }

    @Test
    void usesTheWorldSeedByDefault() {
        World fresh = TestWorlds.flat(4, 4);
        EnemyUpdateService service = new EnemyUpdateService(fresh, ZombieParameters.defaults());
        HordeManager byWorld = new HordeManager(RULES, fresh, service);
        List<double[]> explicit = spawnPositions(fresh.getSeed());

        advance(byWorld, 2.5);

        List<Zombie> spawned = service.zombies();
        assertEquals(explicit.size(), spawned.size());
        for (int i = 0; i < spawned.size(); i++) {
            assertEquals(explicit.get(i)[0], spawned.get(i).getX());
            assertEquals(explicit.get(i)[1], spawned.get(i).getZ());
        }
    }

    @Test
    void zeroDeltaNeverAdvancesTheHorde() {
        HordeManager instant = new HordeManager(new WaveRules(0.0, 2, 0, 2, 0.0, 0.0, 8.0, 12.0), world, enemies);

        instant.update(0.0, CENTER, CENTER);

        assertEquals(0, instant.wave());
        assertEquals(0, enemies.zombies().size());
    }

    @Test
    void spawnsSurroundThePlayerInEveryDirection() {
        HordeManager crowd = new HordeManager(new WaveRules(0.0, 40, 0, 40, 0.0, 1.0, 8.0, 12.0), 3L, world, enemies);

        crowd.update(STEP, CENTER, CENTER);

        boolean[] quadrants = new boolean[4];
        boolean innerHalf = false;
        boolean outerHalf = false;
        for (Zombie zombie : enemies.zombies()) {
            int quadrant = (zombie.getX() < CENTER ? 0 : 1) + (zombie.getZ() < CENTER ? 0 : 2);
            quadrants[quadrant] = true;
            double distance = Math.hypot(zombie.getX() - CENTER, zombie.getZ() - CENTER);
            innerHalf |= distance < 10.0;
            outerHalf |= distance > 10.0;
        }
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            assertTrue(quadrants[quadrant], "ningún zombi en el cuadrante " + quadrant);
        }
        assertTrue(innerHalf && outerHalf, "las distancias ocupan todo el anillo 8–12, no solo un borde");
    }

    @Test
    void zombiesOfOneWaveAndOfConsecutiveWavesDoNotShareSpots() {
        advance(2.5);
        List<Zombie> firstWave = new ArrayList<>(enemies.zombies());
        assertTrue(firstWave.get(0).getX() != firstWave.get(1).getX()
                || firstWave.get(0).getZ() != firstWave.get(1).getZ(), "cada zombi de la oleada tiene su sitio");

        killEveryoneAndDespawn();
        advance(STEP);
        advance(RULES.pauseSeconds());
        Zombie secondWaveFirst = enemies.zombies().get(0);
        assertTrue(secondWaveFirst.getX() != firstWave.get(0).getX()
                || secondWaveFirst.getZ() != firstWave.get(0).getZ(), "la oleada 2 no repite la 1");
    }

    @Test
    void spawnsBeyondTheWorldAreClampedToItsMarginsEvenInNegativeChunks() {
        World negative = new World("negativo", 11L, Instant.parse("2026-09-25T00:00:00Z"));
        Chunk chunk = new Chunk(-1, -1);
        negative.addChunk(chunk);
        for (int x = -Chunk.WIDTH; x < 0; x++) {
            for (int z = -Chunk.DEPTH; z < 0; z++) {
                TestWorlds.place(negative, x, GROUND_Y, z, domain.block.BlockType.STONE);
            }
        }
        EnemyUpdateService service = new EnemyUpdateService(negative, ZombieParameters.defaults());
        HordeManager far = new HordeManager(new WaveRules(0.0, 24, 0, 24, 0.0, 1.0, 100.0, 120.0), negative, service);

        far.update(STEP, -8.0, -8.0);

        assertEquals(0, far.failedSpawns(), "el recorte deja siempre una columna del mundo");
        assertEquals(24, service.zombies().size());
        boolean westEdge = false;
        boolean eastEdge = false;
        boolean northEdge = false;
        boolean southEdge = false;
        for (Zombie zombie : service.zombies()) {
            assertTrue(zombie.getX() >= -15.5 && zombie.getX() <= -0.5, "x=" + zombie.getX());
            assertTrue(zombie.getZ() >= -15.5 && zombie.getZ() <= -0.5, "z=" + zombie.getZ());
            westEdge |= zombie.getX() == -15.5;
            eastEdge |= zombie.getX() == -0.5;
            northEdge |= zombie.getZ() == -15.5;
            southEdge |= zombie.getZ() == -0.5;
        }
        assertTrue(westEdge && eastEdge && northEdge && southEdge, "se usan los cuatro márgenes del mundo");
    }

    @Test
    void rejectsWorldsWithoutChunks() {
        World none = new World("nada", 1L, Instant.parse("2026-09-25T00:00:00Z"));
        assertThrows(IllegalArgumentException.class,
                () -> new HordeManager(RULES, none, new EnemyUpdateService(none, ZombieParameters.defaults())));
    }
}
