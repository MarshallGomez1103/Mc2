package domain.enemy;

import java.util.List;

/** Secuencia de waypoints devuelta por A*, con un cursor al siguiente nodo por alcanzar. */
public final class Path {
    private final List<NavigationNode> nodes;
    private int index;

    public Path(List<NavigationNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Un Path necesita al menos un nodo");
        }
        this.nodes = List.copyOf(nodes);
    }

    public List<NavigationNode> nodes() {
        return nodes;
    }

    public int size() {
        return nodes.size();
    }

    public NavigationNode goal() {
        return nodes.get(nodes.size() - 1);
    }

    public boolean isFinished() {
        return index >= nodes.size();
    }

    public NavigationNode current() {
        if (isFinished()) {
            throw new IllegalStateException("El Path ya terminó");
        }
        return nodes.get(index);
    }

    public void advance() {
        if (!isFinished()) {
            index++;
        }
    }
}
