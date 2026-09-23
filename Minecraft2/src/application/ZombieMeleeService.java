package application;

import domain.enemy.NavigationGrid;
import domain.enemy.Zombie;
import domain.player.Player;
import domain.world.World;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Golpe cuerpo a cuerpo del jugador: elige el zombi vivo más cercano bajo la mira, dentro de
 * {@link #REACH} y sin un bloque sólido entre el ojo y el impacto. Si no hay objetivo válido
 * devuelve vacío para que el clic izquierdo siga eliminando bloques.
 */
public final class ZombieMeleeService {
    public static final double REACH = 4.0;
    public static final int DAMAGE = 1;
    private static final double OCCLUSION_STEP = 0.05;

    private final NavigationGrid grid;

    public ZombieMeleeService(World world) {
        this.grid = new NavigationGrid(Objects.requireNonNull(world, "world no puede ser null"));
    }

    /** Aplica el golpe si hay un zombi alcanzable; true cuando el clic se consumió como ataque. */
    public boolean strike(Player player, Collection<Zombie> zombies) {
        Optional<Zombie> target = findTarget(player, zombies);
        target.ifPresent(zombie -> zombie.takeDamage(DAMAGE));
        return target.isPresent();
    }

    public Optional<Zombie> findTarget(Player player, Collection<Zombie> zombies) {
        double eyeX = player.getX();
        double eyeY = player.getY() + Player.EYE_HEIGHT;
        double eyeZ = player.getZ();
        double yaw = Math.toRadians(player.getYaw());
        double pitch = Math.toRadians(player.getPitch());
        double dirX = -Math.sin(yaw) * Math.cos(pitch);
        double dirY = Math.sin(pitch);
        double dirZ = Math.cos(yaw) * Math.cos(pitch);

        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            if (!zombie.isAlive()) {
                continue;
            }
            double hit = rayHitsBox(zombie, eyeX, eyeY, eyeZ, dirX, dirY, dirZ);
            if (hit >= 0 && hit <= REACH && hit < nearestDistance) {
                nearest = zombie;
                nearestDistance = hit;
            }
        }
        if (nearest == null || isOccluded(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, nearestDistance)) {
            return Optional.empty();
        }
        return Optional.of(nearest);
    }

    /** Intersección rayo-caja por planos (slab test); devuelve la distancia de entrada o -1 si no toca. */
    private static double rayHitsBox(Zombie zombie, double ox, double oy, double oz,
                                     double dx, double dy, double dz) {
        double half = Zombie.WIDTH / 2.0;
        double tMin = 0;
        double tMax = Double.MAX_VALUE;
        double[] origin = {ox, oy, oz};
        double[] direction = {dx, dy, dz};
        double[] min = {zombie.getX() - half, zombie.getY(), zombie.getZ() - half};
        double[] max = {zombie.getX() + half, zombie.getY() + Zombie.HEIGHT, zombie.getZ() + half};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < 1e-9) {
                if (origin[axis] < min[axis] || origin[axis] > max[axis]) {
                    return -1;
                }
                continue;
            }
            double t1 = (min[axis] - origin[axis]) / direction[axis];
            double t2 = (max[axis] - origin[axis]) / direction[axis];
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            if (tMin > tMax) {
                return -1;
            }
        }
        return tMin;
    }

    private boolean isOccluded(double ox, double oy, double oz, double dx, double dy, double dz, double distance) {
        for (double t = OCCLUSION_STEP; t < distance; t += OCCLUSION_STEP) {
            int x = (int) Math.floor(ox + dx * t);
            int y = (int) Math.floor(oy + dy * t);
            int z = (int) Math.floor(oz + dz * t);
            if (grid.isSolid(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
