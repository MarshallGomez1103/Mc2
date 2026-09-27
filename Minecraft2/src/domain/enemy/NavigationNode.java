package domain.enemy;

/**
 * Celda caminable 2.5D: una columna X/Z del mundo y el Y del bloque sólido que sirve de apoyo.
 * Los pies del zombi quedan en {@code groundY + 1}; el cuerpo ocupa {@code groundY + 1} y
 * {@code groundY + 2}. Un nodo no afirma por sí mismo que sea caminable: eso lo decide
 * {@link NavigationGrid} leyendo el estado actual del World.
 */
public record NavigationNode(int x, int groundY, int z) {
    public int feetY() {
        return groundY + 1;
    }

    public double centerX() {
        return x + 0.5;
    }

    public double centerZ() {
        return z + 0.5;
    }

    /** Shortest horizontal distance with cardinal cost 1 and diagonal cost sqrt(2). */
    public double octileTo(NavigationNode other) {
        int dx = Math.abs(x - other.x);
        int dz = Math.abs(z - other.z);
        return Math.max(dx, dz) + (Math.sqrt(2.0) - 1.0) * Math.min(dx, dz);
    }

    public int manhattanTo(NavigationNode other) {
        return Math.abs(x - other.x) + Math.abs(z - other.z);
    }
}
