package application;

import domain.Position;
import domain.block.Block;
import domain.block.BlockType;
import domain.player.Player;
import domain.world.Chunk;
import domain.world.World;
import patterns.factory.BlockFactory;

import java.util.Optional;

/**
 * Caso de uso de interacción del jugador con el mundo: apuntar con la mirada
 * (raycast) y colocar/eliminar el bloque apuntado.
 *
 * Va en {@code application} (no en {@code domain}) porque coordina
 * {@link Player} + {@link World} + {@link BlockFactory} para un caso de uso
 * completo, igual que {@link WorldApplicationService} coordina mundo +
 * persistencia. No agrega ningún patrón nuevo: reutiliza Factory (para crear
 * el bloque a colocar) y deja que {@link World} siga siendo el único emisor
 * de notificaciones Observer al llamar a {@code placeBlock}/{@code removeBlock}.
 *
 * Simplificaciones documentadas (dentro del alcance del MVP y de la sección
 * 0.6/0.9 del plan compartido):
 * <ul>
 *   <li>La dirección 3D de la mirada se calcula aquí mismo a partir de
 *       {@code yaw}/{@code pitch} con trigonometría estándar, en vez de
 *       depender de un método nuevo en {@link Player} (por ejemplo,
 *       {@code forwardVector3D()}). {@link Player#forwardVector()} solo
 *       expone el plano horizontal y no alcanza para el raycast. <b>Propuesta
 *       de cambio para revisión de equipo:</b> si en el futuro varias clases
 *       necesitan este vector 3D, tendría sentido moverlo a {@code Player}
 *       como método de solo lectura (no rompe el contrato de la sección 0.6,
 *       ya que no agrega estado); por ahora se mantiene local a esta clase
 *       para no tocar {@code Player.java}, que es responsabilidad exclusiva
 *       del Participante 1.</li>
 *   <li>Antes de aplicar {@code World.placeBlock}, esta clase verifica que el
 *       chunk destino ya exista y que la altura sea válida (0..63). No es un
 *       cambio a la lógica interna de {@link World}/{@link Chunk} (esas
 *       clases no se tocan): es una comprobación defensiva propia de este
 *       servicio, porque {@code World.placeBlock}/{@code removeBlock}
 *       lanzan {@link IllegalArgumentException} si el chunk no existe o la
 *       posición es inválida, y un jugador apuntando hacia el borde de un
 *       chunk todavía no generado (o hacia y = 64) no debería hacer fallar
 *       la interacción — simplemente no pasa nada, igual que cuando el rayo
 *       no encuentra ningún bloque sólido.</li>
 * </ul>
 */
public final class PlayerInteractionService {

    /** Alcance máximo del rayo, en bloques. */
    private static final double MAX_REACH = 6.0;

    /** Paso de avance del rayo por cada muestra. */
    private static final double STEP = 0.05;

    private final BlockFactory blockFactory = new BlockFactory();

    /**
     * Avanza un rayo desde el ojo del jugador ({@code x, y + EYE_HEIGHT, z}) en la
     * dirección de {@code yaw}/{@code pitch}, en pasos de {@link #STEP}, hasta
     * {@link #MAX_REACH}. Devuelve la primera {@link Position} sólida encontrada
     * (para eliminar) junto con la posición inmediatamente anterior del rayo,
     * que siempre es aire (para colocar).
     */
    public Optional<TargetedBlock> raycast(Player player, World world) {
        double eyeX = player.getX();
        double eyeY = player.getY() + Player.EYE_HEIGHT;
        double eyeZ = player.getZ();

        double yawRadians = Math.toRadians(player.getYaw());
        double pitchRadians = Math.toRadians(player.getPitch());

        // yaw controla el plano horizontal (mismo criterio que Player.forwardVector());
        // pitch inclina hacia arriba/abajo. El vector queda normalizado porque
        // sin^2 + cos^2 = 1 en cada factor. snapNearZero evita que, con un yaw
        // exactamente en un múltiplo de 90°, un componente que matemáticamente
        // debería ser cero quede como un épsilon de punto flotante (p. ej.
        // -1.8e-16): sin eso, un jugador parado justo sobre una coordenada
        // entera podría hacer que el rayo redondee al bloque vecino equivocado.
        double directionX = snapNearZero(-Math.sin(yawRadians) * Math.cos(pitchRadians));
        double directionY = snapNearZero(Math.sin(pitchRadians));
        double directionZ = snapNearZero(Math.cos(yawRadians) * Math.cos(pitchRadians));

        Position previousAirPosition = toBlockPosition(eyeX, eyeY, eyeZ);

        int totalSteps = (int) Math.floor(MAX_REACH / STEP);
        for (int step = 1; step <= totalSteps; step++) {
            double distance = step * STEP;
            double sampleX = eyeX + directionX * distance;
            double sampleY = eyeY + directionY * distance;
            double sampleZ = eyeZ + directionZ * distance;

            Position current = toBlockPosition(sampleX, sampleY, sampleZ);

            if (isSolidAt(world, current)) {
                return Optional.of(new TargetedBlock(current, previousAirPosition));
            }
            previousAirPosition = current;
        }
        return Optional.empty();
    }

    /** Elimina el bloque apuntado, si existe. */
    public void removeTargetedBlock(Player player, World world) {
        raycast(player, world).ifPresent(target -> {
            Position blockPosition = target.blockPosition();
            // El raycast solo reporta blockPosition cuando isSolidAt lo encontró
            // dentro de un chunk existente (ver isSolidAt), así que el chunk
            // siempre existe aquí: no hace falta comprobación adicional.
            world.removeBlock(
                    Chunk.chunkXFor(blockPosition),
                    Chunk.chunkZFor(blockPosition),
                    blockPosition);
        });
    }

    /** Coloca un bloque del tipo dado en la posición adyacente al bloque apuntado. */
    public void placeBlockOfType(Player player, World world, BlockType type) {
        raycast(player, world).ifPresent(target -> {
            Position placePosition = target.placePosition();
            if (!canPlaceAt(player, world, placePosition)) {
                // Chunk todavía no generado en esa posición, o altura fuera de
                // rango (p. ej. justo encima de y = 63): no hacemos nada, igual
                // que cuando el rayo no encuentra ningún bloque.
                return;
            }
            Block block = blockFactory.create(type, placePosition);
            world.placeBlock(
                    Chunk.chunkXFor(placePosition),
                    Chunk.chunkZFor(placePosition),
                    block);
        });
    }

    /** Un bloque nuevo no puede ocupar la caja del jugador ni reemplazar otro bloque. */
    private boolean canPlaceAt(Player player, World world, Position position) {
        if (position.y() < 0 || position.y() >= Chunk.HEIGHT) {
            return false;
        }
        var targetChunk = world.findChunk(position);
        if (targetChunk.isEmpty() || targetChunk.get().getBlock(position).isPresent()) {
            return false;
        }
        double halfWidth = Player.WIDTH / 2d;
        boolean overlapsX = player.getX() - halfWidth < position.x() + 1d
                && player.getX() + halfWidth > position.x();
        boolean overlapsY = player.getY() < position.y() + 1d
                && player.getY() + Player.HEIGHT > position.y();
        boolean overlapsZ = player.getZ() - halfWidth < position.z() + 1d
                && player.getZ() + halfWidth > position.z();
        return !(overlapsX && overlapsY && overlapsZ);
    }

    /** Redondea a 0 componentes de dirección que deberían ser exactamente cero (ver comentario en {@link #raycast}). */
    private double snapNearZero(double value) {
        return Math.abs(value) < 1e-9 ? 0.0 : value;
    }

    /** Convierte una posición continua a la Position entera (floor de cada eje, igual que Player.toPosition()). */
    private Position toBlockPosition(double x, double y, double z) {
        return new Position((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /** true si el bloque en esa Position existe y no es AIR (ver sección 0.4: todo no-AIR es sólido). */
    private boolean isSolidAt(World world, Position position) {
        if (position.y() < 0 || position.y() >= Chunk.HEIGHT) {
            // Fuera del rango vertical válido del mundo: se trata como aire,
            // no como un error (mismo criterio que CollisionResolver del
            // Participante 3, para que el rayo no se rompa si el jugador
            // mira hacia arriba o hacia abajo del límite del mundo).
            return false;
        }
        return world.findChunk(position)
                .flatMap(chunk -> chunk.getBlock(position))
                .map(block -> block.getType() != BlockType.AIR)
                .orElse(false); // fuera de cualquier chunk generado: tratar como no sólido (aire implícito)
    }
}
