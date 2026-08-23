package domain.player;

import domain.Position;
import domain.block.BlockType;
import domain.world.Chunk;
import domain.world.World;

/**
 * Resuelve el movimiento del jugador contra el terreno del mundo, eje por eje,
 * evitando que atraviese bloques sólidos y actualizando su estado de aterrizaje.
 *
 * Integra los deltas producidos por {@link PlayerMovementService} (horizontal,
 * Participante 1) y {@link PlayerPhysics} (vertical, Participante 2): ninguno
 * de esos dos aplica movimiento directamente sobre {@link Player}; esta clase
 * es la única que sí muta {@code player.x/y/z} como resultado final del frame.
 *
 * Simplificaciones documentadas (suficientes para el MVP, según el plan
 * compartido — no se implementa física más compleja que esto):
 * <ul>
 *   <li>Resolución eje por eje (X, luego Z, luego Y), no un swept-AABB
 *       continuo: si un eje colisiona, ese eje simplemente no se aplica en
 *       este frame (no se recorta al punto exacto de contacto ni se hace
 *       slide a lo largo de la pared dentro del mismo tick). Con deltas
 *       pequeños por frame (los que produce un bucle de juego típico), esto
 *       es prácticamente indistinguible de un resolver más preciso.</li>
 *   <li>No hay manejo especial de esquinas: si el jugador choca contra dos
 *       bloques en una esquina, ambos ejes horizontales pueden bloquearse en
 *       el mismo frame; no se intenta "esquivar" automáticamente.</li>
 *   <li>Se asume que el jugador nunca es más ancho que un par de bloques
 *       ({@link Player#WIDTH} = 0.6), por lo que la caja de colisión nunca
 *       cruza más de dos bloques por eje horizontal en un caso normal.</li>
 *   <li>Cuando un eje colisiona, la posición de ese eje se deja exactamente
 *       donde estaba antes del intento (no se "empuja" hasta tocar la cara
 *       del bloque). Esto puede dejar un margen submilimétrico entre el
 *       jugador y el obstáculo, invisible en la práctica dado el tamaño de
 *       paso típico por frame.</li>
 * </ul>
 */
public final class CollisionResolver {

    /**
     * Margen pequeño para evitar que comparaciones de punto flotante en los
     * límites exactos de un bloque (p. ej. una caja que termina justo en
     * x = 5.0) se cuenten como colisión contra el bloque vecino.
     */
    private static final double EPSILON = 1e-6;

    /**
     * Intenta mover al jugador por (dx, dy, dz) desde su posición actual, resolviendo
     * colisiones eje por eje contra bloques sólidos de World. Actualiza player.onGround
     * y cancela player.velocityY a 0 si el movimiento vertical fue bloqueado.
     * Debe mutar player.x/y/z directamente (a diferencia de los servicios anteriores,
     * esta clase SÍ aplica el resultado final).
     */
    public void resolveAndApply(Player player, World world, double dx, double dy, double dz) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // 1. Eje X: mover y chequear la caja completa (WIDTH x HEIGHT) en la nueva posición.
        double candidateX = x + dx;
        if (!collidesBox(world, candidateX, y, z)) {
            x = candidateX;
        }

        // 2. Eje Z: igual que X, ya con el X (posiblemente) actualizado.
        double candidateZ = z + dz;
        if (!collidesBox(world, x, y, candidateZ)) {
            z = candidateZ;
        }

        // 3. Eje Y: determina onGround y corta velocityY si hubo choque vertical.
        double candidateY = y + dy;
        if (collidesBox(world, x, candidateY, z)) {
            if (dy <= 0) {
                // Chocó hacia abajo: aterrizó sobre el suelo.
                player.setOnGround(true);
            }
            // Chocó hacia arriba (techo) o hacia abajo: en ambos casos se anula
            // la velocidad vertical acumulada; solo el caso hacia abajo marca onGround.
            player.setVelocityY(0);
            // No se aplica el movimiento en Y: ese eje queda cancelado este frame.
        } else {
            y = candidateY;
            player.setOnGround(false);
        }

        player.setX(x);
        player.setY(y);
        player.setZ(z);
    }

    /**
     * true si la caja de colisión del jugador (WIDTH x HEIGHT, centrada en x/z,
     * con la base en y) se superpone con algún bloque sólido si se ubicara en (x, y, z).
     */
    private boolean collidesBox(World world, double x, double y, double z) {
        double half = Player.WIDTH / 2.0;

        int minBlockX = (int) Math.floor(x - half + EPSILON);
        int maxBlockX = (int) Math.floor(x + half - EPSILON);
        int minBlockZ = (int) Math.floor(z - half + EPSILON);
        int maxBlockZ = (int) Math.floor(z + half - EPSILON);
        int minBlockY = (int) Math.floor(y + EPSILON);
        int maxBlockY = (int) Math.floor(y + Player.HEIGHT - EPSILON);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (isSolidAt(world, new Position(blockX, blockY, blockZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** true si el bloque en esa Position existe y no es AIR (ver sección 0.4: todo no-AIR es sólido). */
    private boolean isSolidAt(World world, Position position) {
        if (position.y() < 0 || position.y() >= Chunk.HEIGHT) {
            // Fuera del rango vertical válido del mundo: se trata como aire,
            // no como un error (evita romper la caja de colisión si el
            // jugador está por debajo de y = 0 o por encima de y = 63).
            return false;
        }
        return world.findChunk(position)
                .flatMap(chunk -> chunk.getBlock(position))
                .map(block -> block.getType() != BlockType.AIR)
                .orElse(false); // fuera de cualquier chunk generado: tratar como no sólido (aire implícito)
    }
}
