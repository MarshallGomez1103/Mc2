package application;

import domain.enemy.NavigationGrid;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/** Una sola selección por rayo para puños y pistola, con altura real y paredes del mundo actual. */
public final class ZombieTargeting {
    private static final double EPSILON = 1e-9;
    private final NavigationGrid grid;

    public ZombieTargeting(World world) {
        grid = new NavigationGrid(Objects.requireNonNull(world));
    }

    public Optional<Zombie> findTarget(Player player, Collection<Zombie> zombies, double reach) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(zombies);
        if (!Double.isFinite(reach) || reach <= 0) throw new IllegalArgumentException("Alcance inválido");
        double ox = player.getX(), oy = player.getY() + Player.EYE_HEIGHT, oz = player.getZ();
        double yaw = Math.toRadians(player.getYaw()), pitch = Math.toRadians(player.getPitch());
        double dx = -Math.sin(yaw) * Math.cos(pitch), dy = Math.sin(pitch), dz = Math.cos(yaw) * Math.cos(pitch);
        Zombie nearest = null;
        double nearestDistance = reach + EPSILON;
        for (Zombie zombie : zombies) {
            if (!zombie.isAlive()) continue;
            double distance = intersection(zombie, ox, oy, oz, dx, dy, dz);
            if (distance >= 0 && distance <= reach && distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }
        if (nearest == null || occluded(ox, oy, oz, dx, dy, dz, nearestDistance)) return Optional.empty();
        return Optional.of(nearest);
    }

    private static double intersection(Zombie zombie, double ox, double oy, double oz,
                                       double dx, double dy, double dz) {
        double half = Zombie.WIDTH / 2;
        double[] origin = {ox, oy, oz}, direction = {dx, dy, dz};
        double[] min = {zombie.getX() - half, zombie.getY(), zombie.getZ() - half};
        double[] max = {zombie.getX() + half, zombie.getY() + zombie.height(), zombie.getZ() + half};
        double near = 0, far = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < EPSILON) {
                if (origin[axis] < min[axis] || origin[axis] > max[axis]) return -1;
            } else {
                double first = (min[axis] - origin[axis]) / direction[axis];
                double second = (max[axis] - origin[axis]) / direction[axis];
                near = Math.max(near, Math.min(first, second));
                far = Math.min(far, Math.max(first, second));
                if (near > far) return -1;
            }
        }
        return near;
    }

    /** Voxel DDA: visita cada celda atravesada sin saltarse bloques ni huecos de chunks. */
    private boolean occluded(double ox, double oy, double oz, double dx, double dy, double dz, double distance) {
        int[] cell = {(int) Math.floor(ox), (int) Math.floor(oy), (int) Math.floor(oz)};
        double[] origin = {ox, oy, oz}, direction = {dx, dy, dz};
        int[] step = new int[3];
        double[] next = new double[3], interval = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            step[axis] = direction[axis] > EPSILON ? 1 : direction[axis] < -EPSILON ? -1 : 0;
            interval[axis] = step[axis] == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / direction[axis]);
            double boundary = cell[axis] + (step[axis] > 0 ? 1 : 0);
            next[axis] = step[axis] == 0 ? Double.POSITIVE_INFINITY : (boundary - origin[axis]) / direction[axis];
        }
        double traveled = 0;
        while (traveled < distance - EPSILON) {
            if (!grid.chunkExists(cell[0], cell[2]) || grid.isSolid(cell[0], cell[1], cell[2])) return true;
            double crossing = Math.min(next[0], Math.min(next[1], next[2]));
            if (crossing >= distance - EPSILON) break;
            int tied = 0;
            for (int axis = 0; axis < 3; axis++) {
                if (next[axis] <= crossing + EPSILON) tied |= 1 << axis;
            }
            // En una arista/esquina también toca celdas laterales: ninguna pared se omite.
            for (int subset = 1; subset < 8; subset++) {
                if ((subset & ~tied) != 0) continue;
                int x = cell[0] + ((subset & 1) != 0 ? step[0] : 0);
                int y = cell[1] + ((subset & 2) != 0 ? step[1] : 0);
                int z = cell[2] + ((subset & 4) != 0 ? step[2] : 0);
                if (!grid.chunkExists(x, z) || grid.isSolid(x, y, z)) return true;
            }
            for (int axis = 0; axis < 3; axis++) {
                if (next[axis] <= crossing + EPSILON) {
                    cell[axis] += step[axis];
                    next[axis] += interval[axis];
                }
            }
            traveled = crossing;
        }
        return false;
    }
}
