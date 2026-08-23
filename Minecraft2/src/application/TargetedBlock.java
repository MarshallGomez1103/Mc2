package application;

import domain.Position;

/**
 * Resultado de un raycast desde la mirada del jugador ({@link PlayerInteractionService#raycast}).
 *
 * @param blockPosition posición entera del primer bloque sólido encontrado (candidato a eliminar).
 * @param placePosition última posición de aire recorrida por el rayo antes de {@code blockPosition}
 *                      (candidato a colocar un bloque nuevo, adyacente a la cara apuntada).
 */
public record TargetedBlock(Position blockPosition, Position placePosition) {
}
