package domain.enemy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Find an exposed pit lip reachable by A*, then commit one collision-checked horizontal step. */
public final class KamikazeDescent {
    private static final int SEARCH_RADIUS = 6;
    private static final int MAX_ROUTE_ATTEMPTS = 6;
    private final AStarPathfinder approachPathfinder = new AStarPathfinder(250);
    private static final int[][] SIDES = {{1,0},{-1,0},{0,1},{0,-1}};

    public Optional<Descent> find(NavigationGrid grid, NavigationNode start, NavigationNode target) {
        if (start.feetY() - target.feetY() <= NavigationGrid.MAX_DROP) return Optional.empty();
        var candidates = new ArrayList<Candidate>();
        Set<String> seen = new HashSet<>();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                var rim = grid.nodeAt(target.x() + dx, target.z() + dz, start.groundY());
                if (rim.isEmpty()) continue;
                for (int[] direction : SIDES) {
                    int x = rim.get().x() + direction[0], z = rim.get().z() + direction[1];
                    var bottom = grid.nodeUnder(x + 0.5, rim.get().feetY() - 0.001, z + 0.5);
                    if (bottom.isEmpty() || rim.get().feetY() - bottom.get().feetY() <= NavigationGrid.MAX_DROP
                            || Math.abs(bottom.get().feetY() - target.feetY()) > NavigationGrid.MAX_DROP) continue;
                    boolean clear = true;
                    for (int y = bottom.get().feetY(); y <= rim.get().feetY() + 1; y++) {
                        if (grid.isSolid(x, y, z)) { clear = false; break; }
                    }
                    String key = rim.get().x() + ":" + rim.get().z() + ":" + x + ":" + z;
                    if (clear && seen.add(key)) candidates.add(new Candidate(rim.get(), bottom.get()));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(c -> start.octileTo(c.rim())
                + 0.25 * c.bottom().octileTo(target)));
        int attempts = 0;
        for (Candidate candidate : candidates) {
            if (attempts++ >= MAX_ROUTE_ATTEMPTS) break;
            Optional<Path> route = approachPathfinder.findPath(grid, start, candidate.rim());
            if (route.isPresent()) return Optional.of(new Descent(route.get(), candidate.bottom(), candidate.rim().feetY()));
        }
        return Optional.empty();
    }

    private record Candidate(NavigationNode rim, NavigationNode bottom) {}
    public record Descent(Path approach, NavigationNode bottom, double entryY) {}
}
