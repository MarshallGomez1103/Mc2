package domain.enemy;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Vista de caminabilidad sobre el World ACTUAL. No copia ni cachea bloques: cada consulta lee
 * {@code findChunk/getBlock}, así un bloque colocado o quitado por el jugador cambia la respuesta
 * de inmediato. Un chunk ausente nunca se trata como terreno.
 *
 * <p>Reglas acordadas: apoyo sólido bajo los pies, dos bloques de aire para el cuerpo, subida
 * máxima de 1 y caída máxima de {@link #MAX_DROP}; vecinos en 4 direcciones para no cortar esquinas
 * de paredes.
 */
public final class NavigationGrid {
    public static final int MAX_CLIMB = 1;
    public static final int MAX_DROP = 3;
    public static final int BODY_HEIGHT = 2;

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final World world;

    public NavigationGrid(World world) {
        this.world = Objects.requireNonNull(world, "world no puede ser null");
    }

    public boolean isSolid(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        Position position = new Position(x, y, z);
        return world.findChunk(position)
                .flatMap(chunk -> chunk.getBlock(position))
                .map(block -> block.getType() != BlockType.AIR)
                .orElse(false);
    }

    public boolean chunkExists(int x, int z) {
        return world.findChunk(new Position(x, 0, z)).isPresent();
    }

    /** Aire suficiente para el cuerpo con los pies en {@code feetY}. */
    public boolean isBodyClear(int x, int feetY, int z) {
        for (int dy = 0; dy < BODY_HEIGHT; dy++) {
            if (isSolid(x, feetY + dy, z)) {
                return false;
            }
        }
        return true;
    }

    public boolean isWalkable(NavigationNode node) {
        return chunkExists(node.x(), node.z())
                && isSolid(node.x(), node.groundY(), node.z())
                && isBodyClear(node.x(), node.feetY(), node.z());
    }

    /**
     * Nodo caminable de la columna (x, z) alcanzable desde una altura de apoyo vecina: busca el
     * apoyo más cercano a {@code fromGroundY} dentro de la subida y caída permitidas.
     */
    public Optional<NavigationNode> nodeAt(int x, int z, int fromGroundY) {
        if (!chunkExists(x, z)) {
            return Optional.empty();
        }
        for (int offset = 0; offset <= MAX_DROP; offset++) {
            if (offset <= MAX_CLIMB) {
                NavigationNode up = new NavigationNode(x, fromGroundY + offset, z);
                if (isWalkable(up)) {
                    return Optional.of(up);
                }
            }
            if (offset > 0) {
                NavigationNode down = new NavigationNode(x, fromGroundY - offset, z);
                if (isWalkable(down)) {
                    return Optional.of(down);
                }
            }
        }
        return Optional.empty();
    }

    /** Nodo bajo una posición continua (pies del jugador o del zombi): primer apoyo sólido hacia abajo. */
    public Optional<NavigationNode> nodeUnder(double x, double y, double z) {
        int columnX = (int) Math.floor(x);
        int columnZ = (int) Math.floor(z);
        if (!chunkExists(columnX, columnZ)) {
            return Optional.empty();
        }
        int feet = Math.min((int) Math.floor(y), Chunk.HEIGHT - 1);
        for (int groundY = feet; groundY >= 0; groundY--) {
            if (isSolid(columnX, groundY, columnZ)) {
                NavigationNode node = new NavigationNode(columnX, groundY, columnZ);
                return isWalkable(node) ? Optional.of(node) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    public List<NavigationNode> neighbors(NavigationNode node) {
        List<NavigationNode> result = new ArrayList<>(DIRECTIONS.length);
        for (int[] direction : DIRECTIONS) {
            nodeAt(node.x() + direction[0], node.z() + direction[1], node.groundY()).ifPresent(result::add);
        }
        return result;
    }
}
