package application;

import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.enemy.ZombieParameters;
import domain.enemy.ZombieState;
import domain.player.Player;
import domain.player.PlayerLife;
import domain.world.World;
import org.junit.jupiter.api.Test;

import static domain.enemy.TestWorlds.GROUND_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integración Enemy + World real: FSM, A*, repath y ataque sobre bloques que cambian en sesión. */
class EnemyUpdateServiceTest {
    private static final double FRAME = 1.0 / 60.0;
    private static final double FEET = GROUND_Y + 1;

    private final World world = TestWorlds.flat(2, 1);
    private final Player player = world.getPlayer();
    private final PlayerLife life = new PlayerLife(player);
    private final EnemyUpdateService service = new EnemyUpdateService(world, ZombieParameters.defaults());

    private void placePlayer(double x, double z) {
        player.setX(x);
        player.setY(FEET);
        player.setZ(z);
    }

    private void run(double seconds) {
        for (int frame = 0; frame < Math.round(seconds / FRAME); frame++) {
            service.update(life, FRAME);
        }
    }

    @Test
    void spawnPlacesFeetOnGroundAndFailsOutsideChunks() {
        Zombie zombie = service.spawnAt(5.5, 5.5).orElseThrow();
        assertEquals(FEET, zombie.getY());
        assertEquals(ZombieParameters.defaults().maxHealth(), zombie.getHealth());
        assertTrue(service.spawnAt(-3.5, 5.5).isEmpty());
        assertEquals(1, service.zombies().size());
    }

    @Test
    void idleZombieFarFromPlayerNeverSearchesPaths() {
        placePlayer(2.5, 2.5);
        Zombie zombie = service.spawnAt(30.5, 8.5).orElseThrow();
        run(1.0);
        assertEquals(ZombieState.IDLE, zombie.getState());
        assertEquals(0, service.pathfinder().getSearchCount());
        assertEquals(30.5, zombie.getX());
    }

    @Test
    void chaseMovesTowardPlayerWithoutSearchingEveryFrame() {
        placePlayer(12.5, 5.5);
        Zombie zombie = service.spawnAt(3.5, 5.5).orElseThrow();
        run(2.0);
        assertEquals(ZombieState.CHASE, zombie.getState());
        assertTrue(zombie.getX() > 7.0, "avanzó hacia el jugador: " + zombie.getX());
        int searches = service.pathfinder().getSearchCount();
        assertTrue(searches >= 1 && searches <= 3, "120 fotogramas deben producir 1-3 búsquedas, no " + searches);
    }

    @Test
    void playerMovingAwayFromPlannedGoalForcesRepath() {
        placePlayer(10.5, 5.5);
        service.spawnAt(3.5, 5.5).orElseThrow();
        run(FRAME * 3);
        int before = service.pathfinder().getSearchCount();
        assertEquals(1, before);
        placePlayer(10.5, 9.5);
        run(FRAME);
        assertEquals(2, service.pathfinder().getSearchCount());
    }

    @Test
    void wallBuiltDuringChaseInvalidatesRouteAndZombieWalksAround() {
        placePlayer(12.5, 5.5);
        Zombie zombie = service.spawnAt(2.5, 5.5).orElseThrow();
        run(0.5);
        assertTrue(zombie.getX() > 2.5);
        for (int z = 0; z < 14; z++) {
            TestWorlds.wall(world, 7, z);
        }
        run(12.0);
        assertTrue(zombie.getX() > 8.0, "rodeó la pared: x=" + zombie.getX());
        assertEquals(ZombieState.ATTACK, zombie.getState());
        assertTrue(service.pathfinder().getSearchCount() < 30, "sin busy retry: " + service.pathfinder().getSearchCount());
    }

    @Test
    void unreachablePlayerDoesNotBusyRetryPathfinding() {
        placePlayer(10.5, 10.5);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    TestWorlds.wall(world, 10 + dx, 10 + dz);
                }
            }
        }
        service.spawnAt(3.5, 3.5).orElseThrow();
        run(2.0);
        int searches = service.pathfinder().getSearchCount();
        assertTrue(searches <= 3, "reintentos espaciados por noRouteRetrySeconds: " + searches);
    }

    @Test
    void attackKillsPlayerOnlyAfterCooldownAndUsesEnemyCause() {
        placePlayer(5.5, 5.5);
        Zombie zombie = service.spawnAt(6.5, 5.5).orElseThrow();
        run(0.5);
        assertEquals(ZombieState.ATTACK, zombie.getState());
        assertFalse(life.isDead(), "el primer golpe espera el cooldown");
        run(0.6);
        assertTrue(life.isDead());
        assertEquals(PlayerLife.DeathCause.ENEMY, life.deathCause());
    }

    @Test
    void deadPlayerOrDisabledEnemiesFreezeAllZombies() {
        placePlayer(5.5, 5.5);
        Zombie zombie = service.spawnAt(9.5, 5.5).orElseThrow();
        service.setEnabled(false);
        run(1.0);
        assertEquals(ZombieState.IDLE, zombie.getState());
        assertEquals(9.5, zombie.getX());

        service.setEnabled(true);
        life.die(PlayerLife.DeathCause.VOID);
        run(1.0);
        assertEquals(ZombieState.IDLE, zombie.getState());
        assertEquals(9.5, zombie.getX());
    }

    @Test
    void killedZombiesBecomeDeadThenDespawnAndCollectionDoesNotGrow() {
        placePlayer(2.5, 2.5);
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < 20; i++) {
                service.spawnAt(20.5 + (i % 5), 5.5 + (i / 5)).orElseThrow();
            }
            assertEquals(20, service.zombies().size());
            service.zombies().forEach(zombie -> zombie.takeDamage(99));
            run(FRAME);
            assertTrue(service.zombies().stream().allMatch(zombie -> zombie.getState() == ZombieState.DEAD));
            run(EnemyUpdateService.DESPAWN_SECONDS);
            assertEquals(0, service.zombies().size(), "ronda " + round);
        }
    }
}
