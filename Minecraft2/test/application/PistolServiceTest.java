package application;

import domain.Position;
import domain.enemy.AStarPathfinder;
import domain.enemy.NavigationGrid;
import domain.enemy.TestWorlds;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PistolServiceTest {
    private static World world() {
        World world = TestWorlds.flat(4, 4);
        world.getPlayer().setX(16.5);
        world.getPlayer().setY(21);
        world.getPlayer().setZ(16.5);
        return world;
    }
    private static PistolService owned(World world) {
        PistolService pistol = new PistolService(world);
        pistol.update(3, .01);
        Position cell = pistol.pickupPosition().orElseThrow();
        world.getPlayer().setX(cell.x() + .5);
        world.getPlayer().setY(cell.y());
        world.getPlayer().setZ(cell.z() + .5);
        pistol.update(3, .01);
        return pistol;
    }
    @Test void onlyAppearsFromThirdWaveAndPersistsInLaterWaves() {
        World world = world();
        PistolService pistol = new PistolService(world);
        pistol.update(1, 10);
        pistol.update(2, 10);
        assertFalse(pistol.appeared());
        assertTrue(pistol.pickupPosition().isEmpty());
        pistol.update(3, .01);
        Position first = pistol.pickupPosition().orElseThrow();
        pistol.update(8, 30);
        assertEquals(first, pistol.pickupPosition().orElseThrow());
        assertTrue(pistol.appeared());
        assertFalse(pistol.owned());
    }
    @Test void skippedWaveThresholdStillSpawnsOnSafeReachableActualGround() {
        World world = world();
        PistolService pistol = new PistolService(world);
        pistol.update(7, .01);
        Position cell = pistol.pickupPosition().orElseThrow();
        NavigationGrid grid = new NavigationGrid(world);
        assertTrue(world.findChunk(cell).isPresent());
        assertTrue(grid.isSolid(cell.x(), cell.y() - 1, cell.z()));
        assertTrue(grid.isBodyClear(cell.x(), cell.y(), cell.z()));
        var start = grid.nodeUnder(16.5, 21, 16.5).orElseThrow();
        var end = grid.nodeUnder(cell.x() + .5, cell.y(), cell.z() + .5).orElseThrow();
        assertTrue(new AStarPathfinder(2_000).findPath(grid, start, end).isPresent());
    }
    @Test void seedControlsRandomLocationWithoutEditingWorldBlocks() {
        World world = world();
        int count = world.getChunks().stream().mapToInt(c -> c.getBlocks().size()).sum();
        Set<Position> positions = new HashSet<>();
        for (int seed = 0; seed < 12; seed++) {
            PistolService pistol = new PistolService(world, seed);
            pistol.update(3, .01);
            positions.add(pistol.pickupPosition().orElseThrow());
        }
        assertTrue(positions.size() > 4);
        assertEquals(count, world.getChunks().stream().mapToInt(c -> c.getBlocks().size()).sum());
        PistolService first = new PistolService(world, 19), second = new PistolService(world, 19);
        first.update(3, .01); second.update(3, .01);
        assertEquals(first.pickupPosition(), second.pickupPosition());
    }
    @Test void proximityAutoEquipsAndNeverCreatesASecondPickup() {
        World world = world();
        PistolService pistol = owned(world);
        assertTrue(pistol.owned());
        assertTrue(pistol.equipped());
        assertTrue(pistol.pickupPosition().isEmpty());
        pistol.toggleEquipped();
        assertFalse(pistol.equipped());
        pistol.toggleEquipped();
        assertTrue(pistol.equipped());
        pistol.unequip();
        pistol.update(30, 60);
        assertTrue(pistol.owned());
        assertFalse(pistol.equipped());
        assertTrue(pistol.pickupPosition().isEmpty());
    }
    @Test void unownedGunCannotEquipOrFire() {
        World world = world();
        PistolService pistol = new PistolService(world);
        pistol.toggleEquipped();
        assertFalse(pistol.equipped());
        assertFalse(pistol.shoot(world.getPlayer(), List.of()));
        assertEquals(0, pistol.cooldownSeconds());
    }
    @Test void shotKillsNormalZombieAndCooldownPreventsInstantSecondShot() {
        World world = world();
        PistolService pistol = owned(world);
        Player player = world.getPlayer();
        player.setX(5.5); player.setY(21); player.setZ(5.5);
        Zombie near = new Zombie(5.5, 21, 9.5, 3), far = new Zombie(5.5, 21, 13.5, 3);
        assertTrue(pistol.shoot(player, List.of(far, near)));
        assertFalse(near.isAlive());
        assertEquals(3, far.getHealth());
        assertFalse(pistol.shoot(player, List.of(far)));
        assertTrue(pistol.shotFlashSeconds() > 0);
        pistol.update(3, PistolService.SHOT_COOLDOWN);
        assertEquals(0, pistol.shotFlashSeconds());
        assertTrue(pistol.shoot(player, List.of(far)));
        assertFalse(far.isAlive());
    }
    @Test void wallsAndRangeProtectEnemiesAndMissNeverChangesBlocks() {
        World world = world();
        PistolService pistol = owned(world);
        Player player = world.getPlayer();
        player.setX(5.5); player.setY(21); player.setZ(5.5);
        Zombie zombie = new Zombie(5.5, 21, 10.5, 3);
        TestWorlds.wall(world, 5, 8);
        int blocks = world.getChunks().stream().mapToInt(c -> c.getBlocks().size()).sum();
        assertTrue(pistol.shoot(player, List.of(zombie)));
        assertEquals(3, zombie.getHealth());
        assertEquals(blocks, world.getChunks().stream().mapToInt(c -> c.getBlocks().size()).sum());
        pistol.update(3, PistolService.SHOT_COOLDOWN);
        Zombie tooFar = new Zombie(6.5, 21, 43.5, 3);
        player.setX(6.5);
        assertTrue(pistol.shoot(player, List.of(tooFar)));
        assertEquals(3, tooFar.getHealth());
    }
    @Test void pickupRequiresMatchingFloorHeight() {
        World world = world();
        PistolService pistol = new PistolService(world);
        pistol.update(3, .01);
        Position cell = pistol.pickupPosition().orElseThrow();
        Player player = world.getPlayer();
        player.setX(cell.x() + .5); player.setZ(cell.z() + .5); player.setY(cell.y() + 4);
        pistol.update(3, .01);
        assertFalse(pistol.owned());
        player.setY(cell.y());
        pistol.update(3, .01);
        assertTrue(pistol.owned());
    }
    @Test void undergroundPlayerStillUnlocksPickupInThirdWave() {
        var world = world(); var player = world.getPlayer();
        domain.enemy.TestWorlds.remove(world, 16, 20, 16);
        domain.enemy.TestWorlds.place(world, 16, 10, 16, domain.block.BlockType.STONE);
        player.setX(16.5); player.setY(11); player.setZ(16.5);
        var pistol = new PistolService(world);
        pistol.update(3, .01);
        assertTrue(pistol.appeared());
        var pickup = pistol.pickupPosition().orElseThrow();
        assertTrue(new NavigationGrid(world).isWalkable(new domain.enemy.NavigationNode(pickup.x(), pickup.y()-1, pickup.z())));
        assertFalse(pistol.owned());
    }
}
