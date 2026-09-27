package domain.enemy;

/** Horizontal motion and timed jumps; vertical movement is resolved by continuous gravity. */
public final class ZombieMovement {
    public static final double ARRIVE_DISTANCE = 0.2;
    public static final double CRAWLER_SPEED_FACTOR = 0.28;
    private static final double EPSILON = 1e-6;
    private final ZombiePhysics physics;

    public ZombieMovement() { this(new ZombiePhysics()); }
    public ZombieMovement(ZombiePhysics physics) { this.physics = physics; }

    public void advancePhysics(Zombie zombie, NavigationGrid grid, double deltaSeconds) {
        physics.advance(zombie, grid, deltaSeconds);
    }

    public boolean follow(Zombie zombie, Path path, NavigationGrid grid, double speed, double deltaSeconds) {
        if (!path.isFinished() && path.current().feetY() > zombie.getY() + 0.1) {
            physics.jump(zombie, grid);
        }
        physics.advance(zombie, grid, deltaSeconds);
        if (!zombie.isAlive()) return true;
        double budget = Math.max(0, speed * (zombie.isCrawler() ? CRAWLER_SPEED_FACTOR : 1) * deltaSeconds);
        while (!path.isFinished()) {
            NavigationNode target = path.current();
            double dx = target.centerX() - zombie.getX(), dz = target.centerZ() - zombie.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance <= EPSILON) { path.advance(); continue; }
            if (budget <= EPSILON) return true;
            double step = Math.min(0.1, Math.min(budget, distance));
            if (!tryMove(zombie, grid, zombie.getX() + dx / distance * step,
                    zombie.getZ() + dz / distance * step)) {
                // A planned one-block climb waits for jump height; a changed flat route is blocked.
                return target.feetY() > zombie.getY() + EPSILON && !zombie.isCrawler();
            }
            budget -= step;
        }
        return true;
    }

    /** Move toward a drop/cave waypoint without requiring support, while never crossing a wall. */
    public boolean toward(Zombie zombie, NavigationGrid grid, double x, double z,
                          double speed, double deltaSeconds) {
        physics.advance(zombie, grid, deltaSeconds);
        if (!zombie.isAlive()) return false;
        double dx = x - zombie.getX(), dz = z - zombie.getZ(), distance = Math.hypot(dx, dz);
        if (distance <= EPSILON) return true;
        double budget = Math.min(distance, speed * (zombie.isCrawler() ? CRAWLER_SPEED_FACTOR : 1) * deltaSeconds);
        while (budget > EPSILON) {
            double step = Math.min(0.1, budget);
            if (!tryMove(zombie, grid, zombie.getX() + dx / distance * step,
                    zombie.getZ() + dz / distance * step)) return false;
            budget -= step;
        }
        return true;
    }

    /** Horizontal body validation only: separation cannot climb and air movement needs no floor. */
    public boolean tryMove(Zombie zombie, NavigationGrid grid, double nextX, double nextZ) {
        if (!physics.isBodyClear(zombie, grid, nextX, zombie.getY(), nextZ)) return false;
        zombie.setPosition(nextX, zombie.getY(), nextZ);
        return true;
    }
}
