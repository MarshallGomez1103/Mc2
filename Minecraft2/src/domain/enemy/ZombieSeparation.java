package domain.enemy;

import java.util.List;

/** Local AABB crowd resolution. Dead bodies are visual only and never obstruct the living. */
public final class ZombieSeparation {
    private static final double EPSILON = 1e-5;
    private final ZombieMovement movement = new ZombieMovement();

    public boolean isOccupied(Zombie body, List<Zombie> zombies) {
        return zombies.stream().anyMatch(other -> other != body && overlaps(body, other));
    }

    public boolean overlaps(Zombie a, Zombie b) {
        return a.isAlive() && b.isAlive()
                && a.getY() < b.getY() + b.height() + 0.35 - EPSILON
                && b.getY() < a.getY() + a.height() + 0.35 - EPSILON
                && Math.abs(a.getX() - b.getX()) < Zombie.WIDTH - EPSILON
                && Math.abs(a.getZ() - b.getZ()) < Zombie.WIDTH - EPSILON;
    }

    /** Stable list order breaks exact-coordinate ties; bounded passes converge without random jitter. */
    public void resolve(List<Zombie> zombies, NavigationGrid grid) {
        for (int pass = 0; pass < 32; pass++) {
            boolean changed = false;
            for (int i = 0; i < zombies.size(); i++) {
                for (int j = i + 1; j < zombies.size(); j++) {
                    Zombie a = zombies.get(i), b = zombies.get(j);
                    if (!overlaps(a, b)) continue;
                    double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
                    boolean xAxis = Zombie.WIDTH - Math.abs(dx) < Zombie.WIDTH - Math.abs(dz);
                    if (Math.abs(dx) + Math.abs(dz) < EPSILON) xAxis = (i + j) % 2 == 0;
                    changed |= separate(a, b, grid, xAxis);
                    if (overlaps(a, b)) changed |= separate(a, b, grid, !xAxis);
                }
            }
            if (!changed) return;
        }
    }

    private boolean separate(Zombie a, Zombie b, NavigationGrid grid, boolean xAxis) {
        double delta = xAxis ? a.getX() - b.getX() : a.getZ() - b.getZ();
        double distance = Math.min(0.15, (Zombie.WIDTH - Math.abs(delta) + EPSILON) / 2);
        if (distance <= 0) return false;
        double sign = delta < 0 ? -1 : 1;
        boolean first = move(a, grid, xAxis, sign * distance);
        boolean second = move(b, grid, xAxis, -sign * distance);
        // If one body is against a wall, the other takes its half of the correction too.
        if (first && !second) move(a, grid, xAxis, sign * distance);
        if (second && !first) move(b, grid, xAxis, -sign * distance);
        return first || second;
    }

    private boolean move(Zombie zombie, NavigationGrid grid, boolean xAxis, double amount) {
        if (!zombie.isOnGround()) return false;
        double x = zombie.getX(), y = zombie.getY(), z = zombie.getZ();
        boolean moved = movement.tryMove(zombie, grid, x + (xAxis ? amount : 0),
                z + (xAxis ? 0 : amount));
        // Crowd corrections are horizontal. Only chasing may climb terrain.
        if (moved && Math.abs(zombie.getY() - y) > EPSILON) {
            zombie.setPosition(x, y, z);
            return false;
        }
        return moved;
    }
}
