package domain.enemy;

import java.util.Optional;

/**
 * Desplaza al zombi hacia el waypoint actual del Path sin atravesar bloques sólidos.
 * El movimiento es horizontal a velocidad constante; la altura se ajusta al apoyo más alto de
 * las columnas que pisa la caja (subir 1 o bajar hasta MAX_DROP), sin salto ni gravedad continua.
 */
public final class ZombieMovement {
    /** Distancia al centro del waypoint que cuenta como alcanzado. */
    public static final double ARRIVE_DISTANCE = 0.2;
    private static final double EPSILON = 1e-6;

    /**
     * @return false si el siguiente paso quedó bloqueado por un bloque que no estaba al calcular la
     *         ruta; el llamador debe pedir un repath. true cuando avanzó o el Path ya terminó.
     */
    public boolean follow(Zombie zombie, Path path, NavigationGrid grid, double speed, double deltaSeconds) {
        if (path.isFinished()) {
            return true;
        }
        NavigationNode target = path.current();
        double dx = target.centerX() - zombie.getX();
        double dz = target.centerZ() - zombie.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance <= ARRIVE_DISTANCE) {
            path.advance();
            return true;
        }
        double step = Math.min(speed * deltaSeconds, distance);
        double nextX = zombie.getX() + dx / distance * step;
        double nextZ = zombie.getZ() + dz / distance * step;

        int currentGroundY = (int) Math.floor(zombie.getY()) - 1;
        double half = Zombie.WIDTH / 2.0;
        int minX = (int) Math.floor(nextX - half + EPSILON);
        int maxX = (int) Math.floor(nextX + half - EPSILON);
        int minZ = (int) Math.floor(nextZ - half + EPSILON);
        int maxZ = (int) Math.floor(nextZ + half - EPSILON);

        // La caja puede tocar dos columnas por eje: todas necesitan apoyo alcanzable y el zombi
        // se sube al más alto (auto-step de un bloque, igual que al pisar un escalón).
        int feetY = Integer.MIN_VALUE;
        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                Optional<NavigationNode> support = grid.nodeAt(blockX, blockZ, currentGroundY);
                if (support.isEmpty()) {
                    return false;
                }
                feetY = Math.max(feetY, support.get().feetY());
            }
        }
        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                if (!grid.isBodyClear(blockX, feetY, blockZ)) {
                    return false;
                }
            }
        }
        zombie.setPosition(nextX, feetY, nextZ);
        return true;
    }
}
