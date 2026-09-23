package domain.enemy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* sobre {@link NavigationGrid}: open set en cola de prioridad por f = g + h, closed set,
 * mapa de padres y reconstrucción del camino. Coste 1 por paso más 0.5 por bloque de desnivel;
 * la heurística Manhattan en X/Z nunca sobreestima porque cada paso cuesta al menos 1.
 *
 * <p>Ausencia de ruta = {@link Optional#empty()}: no se inventa un camino parcial. El presupuesto de
 * expansiones evita recorrer un mundo de 256 chunks cuando el destino es inalcanzable.
 */
public final class AStarPathfinder {
    public static final int DEFAULT_MAX_EXPANSIONS = 2_000;
    private static final double CLIMB_COST = 0.5;

    private final int maxExpansions;
    private int searchCount;
    private int lastExpansions;
    private long totalExpansions;

    public AStarPathfinder() {
        this(DEFAULT_MAX_EXPANSIONS);
    }

    public AStarPathfinder(int maxExpansions) {
        if (maxExpansions <= 0) {
            throw new IllegalArgumentException("maxExpansions debe ser positivo");
        }
        this.maxExpansions = maxExpansions;
    }

    public Optional<Path> findPath(NavigationGrid grid, NavigationNode start, NavigationNode goal) {
        Objects.requireNonNull(grid, "grid no puede ser null");
        Objects.requireNonNull(start, "start no puede ser null");
        Objects.requireNonNull(goal, "goal no puede ser null");
        searchCount++;
        lastExpansions = 0;

        if (!grid.isWalkable(start) || !grid.isWalkable(goal)) {
            return Optional.empty();
        }
        if (start.equals(goal)) {
            return Optional.of(new Path(List.of(start)));
        }

        Map<NavigationNode, Double> gScore = new HashMap<>();
        Map<NavigationNode, NavigationNode> parent = new HashMap<>();
        Set<NavigationNode> closed = new HashSet<>();
        PriorityQueue<OpenEntry> open = new PriorityQueue<>(
                Comparator.comparingDouble(OpenEntry::f).thenComparingDouble(OpenEntry::h));

        gScore.put(start, 0.0);
        open.add(new OpenEntry(start, 0.0, start.manhattanTo(goal)));

        while (!open.isEmpty()) {
            OpenEntry entry = open.poll();
            NavigationNode current = entry.node();
            if (closed.contains(current)) {
                continue;
            }
            if (current.equals(goal)) {
                return Optional.of(new Path(reconstruct(parent, goal)));
            }
            closed.add(current);
            lastExpansions++;
            totalExpansions++;
            if (lastExpansions > maxExpansions) {
                return Optional.empty();
            }

            double currentG = gScore.get(current);
            for (NavigationNode neighbor : grid.neighbors(current)) {
                if (closed.contains(neighbor)) {
                    continue;
                }
                double tentativeG = currentG + 1.0
                        + CLIMB_COST * Math.abs(neighbor.groundY() - current.groundY());
                Double knownG = gScore.get(neighbor);
                if (knownG == null || tentativeG < knownG) {
                    gScore.put(neighbor, tentativeG);
                    parent.put(neighbor, current);
                    open.add(new OpenEntry(neighbor, tentativeG, neighbor.manhattanTo(goal)));
                }
            }
        }
        return Optional.empty();
    }

    private static List<NavigationNode> reconstruct(Map<NavigationNode, NavigationNode> parent,
                                                    NavigationNode goal) {
        List<NavigationNode> reversed = new ArrayList<>();
        for (NavigationNode node = goal; node != null; node = parent.get(node)) {
            reversed.add(node);
        }
        List<NavigationNode> ordered = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            ordered.add(reversed.get(i));
        }
        return ordered;
    }

    /** Búsquedas ejecutadas desde la creación; sirve para comprobar que no hay A* por zombi y frame. */
    public int getSearchCount() {
        return searchCount;
    }

    public int getLastExpansions() {
        return lastExpansions;
    }

    public long getTotalExpansions() {
        return totalExpansions;
    }

    private record OpenEntry(NavigationNode node, double g, double h) {
        double f() {
            return g + h;
        }
    }
}
