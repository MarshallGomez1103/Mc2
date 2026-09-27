package domain.enemy;

import domain.player.Player;

/** Continuous, swept body collision using the same jump speed and gravity as the player. */
public final class ZombiePhysics {
    private static final double EPS = 1e-6;
    private final KamikazePolicy landingPolicy;

    public ZombiePhysics() { this(new KamikazePolicy(() -> 1.0)); }
    public ZombiePhysics(KamikazePolicy landingPolicy) { this.landingPolicy = landingPolicy; }

    public void jump(Zombie zombie, NavigationGrid grid) {
        if (!zombie.isCrawler() && zombie.isOnGround() && zombie.jumpCooldownSeconds() <= EPS
                && isBodyClear(zombie, grid, zombie.getX(), zombie.getY() + 0.12, zombie.getZ())) {
            zombie.beginJump(Player.JUMP_SPEED);
        }
    }

    public void advance(Zombie zombie, NavigationGrid grid, double deltaSeconds) {
        if (!zombie.isAlive() || deltaSeconds <= 0) return;
        double left = deltaSeconds;
        while (left > EPS && zombie.isAlive()) {
            double dt = Math.min(left, 1.0 / 120.0);
            left -= dt;
            zombie.advanceMotionTimers(dt);
            if (supported(zombie, grid) && zombie.velocityY() <= 0) {
                land(zombie);
                continue;
            }
            zombie.setOnGround(false);
            zombie.trackFallHeight();
            zombie.setVelocityY(zombie.velocityY() + Player.GRAVITY * dt);
            double dy = zombie.velocityY() * dt;
            double remaining = Math.abs(dy);
            double sign = Math.signum(dy);
            while (remaining > EPS) {
                double step = Math.min(remaining, 0.04);
                double nextY = zombie.getY() + sign * step;
                if (!isBodyClear(zombie, grid, zombie.getX(), nextY, zombie.getZ())) {
                    // Narrow the final collision fraction to retain exact continuous feet height.
                    double lo = 0, hi = step;
                    for (int i = 0; i < 20; i++) {
                        double mid = (lo + hi) / 2;
                        if (isBodyClear(zombie, grid, zombie.getX(), zombie.getY() + sign * mid,
                                zombie.getZ())) lo = mid; else hi = mid;
                    }
                    double contactY = zombie.getY() + sign * lo;
                    if (Math.abs(contactY - Math.rint(contactY)) < 2e-6) contactY = Math.rint(contactY);
                    zombie.setPosition(zombie.getX(), contactY, zombie.getZ());
                    if (sign < 0) land(zombie); else zombie.setVelocityY(0);
                    break;
                }
                zombie.setPosition(zombie.getX(), nextY, zombie.getZ());
                zombie.trackFallHeight();
                remaining -= step;
            }
            if (zombie.getY() < -8) zombie.shatter();
        }
    }

    private void land(Zombie zombie) {
        if (!zombie.isOnGround()) {
            landingPolicy.onLanding(zombie, zombie.fallDistance());
        }
        zombie.finishLanding();
    }

    public boolean supported(Zombie zombie, NavigationGrid grid) {
        return !isBodyClear(zombie, grid, zombie.getX(), zombie.getY() - 0.002, zombie.getZ());
    }

    public boolean isBodyClear(Zombie zombie, NavigationGrid grid, double x, double y, double z) {
        double half = Zombie.WIDTH / 2;
        int minX = (int) Math.floor(x - half + EPS), maxX = (int) Math.floor(x + half - EPS);
        int minZ = (int) Math.floor(z - half + EPS), maxZ = (int) Math.floor(z + half - EPS);
        int minY = (int) Math.floor(y + EPS), maxY = (int) Math.floor(y + zombie.height() - EPS);
        for (int bx = minX; bx <= maxX; bx++) for (int bz = minZ; bz <= maxZ; bz++) {
            if (!grid.chunkExists(bx, bz)) return false;
            for (int by = minY; by <= maxY; by++) if (grid.isSolid(bx, by, bz)) return false;
        }
        return true;
    }
}
