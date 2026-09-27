package domain.enemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cosmetic fragments: no health, navigation, hitbox or wave membership; lifetime is the session. */
public final class ZombieDebris {
    public static final int FRAGMENT_COUNT = 18;
    public static final double RADIUS = .20;
    private final List<Fragment> fragments = new ArrayList<>(FRAGMENT_COUNT);
    private boolean settled;

    public ZombieDebris(Zombie source) {
        if (!source.isShattered()) throw new IllegalArgumentException("Solo un zombi desarmado deja piezas");
        double[] heights = {1.575, 1.025, 1.27, 1.27, .35, .35};
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            double angle = i * Math.PI * 2 / FRAGMENT_COUNT;
            double speed = .8 + (i % 3) * .35;
            double height = source.isCrawler() ? .45 : heights[i / 3];
            fragments.add(new Fragment(i / 3, source.getX(), source.getY() + height, source.getZ(),
                    Math.cos(angle) * speed, 1.3 + (i % 4) * .2, Math.sin(angle) * speed));
        }
    }

    public List<Fragment> fragments() { return Collections.unmodifiableList(fragments); }
    public boolean isSettled() { return settled; }

    /** Small swept steps stop pieces at walls/floors, including cave floors below a mountain. */
    public void advance(NavigationGrid grid, double seconds) {
        if (settled || seconds <= 0) return;
        double remaining = seconds;
        while (remaining > 1e-9) {
            double step = Math.min(.02, remaining);
            for (Fragment f : fragments) {
                if (f.settled) continue;
                if (clear(grid, f.x + f.vx * step, f.y, f.z)) f.x += f.vx * step;
                else f.vx = 0;
                if (clear(grid, f.x, f.y, f.z + f.vz * step)) f.z += f.vz * step;
                else f.vz = 0;
                f.vy -= 12 * step;
                double nextY = f.y + f.vy * step;
                if (clear(grid, f.x, nextY, f.z)) {
                    f.y = nextY;
                    f.rotation += step * 150;
                    // Pieces that fall out of the finite world cannot leave a floor decoration.
                    if (f.y < -4) { f.visible = false; f.settled = true; }
                } else if (f.vy < 0) {
                    f.vx = f.vy = f.vz = 0;
                    f.settled = true;
                    f.rotation = 0; // flat pieces stay entirely above the support surface
                } else {
                    f.vy = 0;
                }
            }
            remaining -= step;
        }
        settled = fragments.stream().allMatch(f -> f.settled);
    }

    private static boolean clear(NavigationGrid grid, double x, double y, double z) {
        for (double dx : new double[] {-RADIUS, RADIUS})
            for (double dy : new double[] {-RADIUS, RADIUS})
                for (double dz : new double[] {-RADIUS, RADIUS})
                    if (grid.isSolid((int)Math.floor(x+dx), (int)Math.floor(y+dy), (int)Math.floor(z+dz))) return false;
        return true;
    }

    public static final class Fragment {
        private final int part;
        private double x, y, z, vx, vy, vz, rotation;
        private boolean settled, visible = true;
        private Fragment(int part, double x, double y, double z, double vx, double vy, double vz) {
            this.part=part; this.x=x; this.y=y; this.z=z; this.vx=vx; this.vy=vy; this.vz=vz;
        }
        public int part() { return part; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public double rotation() { return rotation; }
        public boolean visible() { return visible; }
    }
}
