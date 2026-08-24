package presentation.game;

import com.badlogic.gdx.graphics.Color;
import domain.block.BlockType;

/**
 * Representación visual de los ocho tipos de bloque: un color base por material y un
 * sombreado según hacia dónde mira la cara.
 *
 * <p>El sombreado no es decorativo. Con un color plano idéntico en las seis caras, un mundo de
 * cubos se ve como una mancha uniforme y no se distingue el relieve. Oscurecer los laterales y
 * la base respecto a la cara superior basta para que el terreno se lea, sin necesidad de
 * texturas ni de iluminación real.
 */
public final class BlockAppearance {
    /** Factores de brillo por orientación de la cara. */
    private static final float TOP_SHADE = 1.00f;
    private static final float SIDE_X_SHADE = 0.72f;
    private static final float SIDE_Z_SHADE = 0.86f;
    private static final float BOTTOM_SHADE = 0.55f;

    private BlockAppearance() {
    }

    /** Color base del material, antes de aplicar el sombreado de la cara. */
    public static Color baseColorOf(BlockType type) {
        return switch (type) {
            case GRASS -> new Color(0.35f, 0.68f, 0.28f, 1f);
            case DIRT -> new Color(0.55f, 0.39f, 0.24f, 1f);
            case STONE -> new Color(0.53f, 0.53f, 0.55f, 1f);
            case SAND -> new Color(0.87f, 0.82f, 0.58f, 1f);
            case GRAVEL -> new Color(0.45f, 0.43f, 0.42f, 1f);
            case WOOD -> new Color(0.44f, 0.31f, 0.17f, 1f);
            case LEAVES -> new Color(0.24f, 0.52f, 0.22f, 1f);
            // El aire no se dibuja nunca; se devuelve transparente por completitud.
            case AIR -> new Color(0f, 0f, 0f, 0f);
        };
    }

    /** Color final de una cara concreta, ya sombreado según su orientación. */
    public static Color colorOf(BlockType type, Face face) {
        Color color = baseColorOf(type);
        float shade = shadeOf(face);
        return color.set(color.r * shade, color.g * shade, color.b * shade, color.a);
    }

    private static float shadeOf(Face face) {
        return switch (face) {
            case TOP -> TOP_SHADE;
            case BOTTOM -> BOTTOM_SHADE;
            case EAST, WEST -> SIDE_X_SHADE;
            case NORTH, SOUTH -> SIDE_Z_SHADE;
        };
    }

    /** Las seis caras de un cubo, con el desplazamiento hacia el bloque vecino. */
    public enum Face {
        TOP(0, 1, 0),
        BOTTOM(0, -1, 0),
        EAST(1, 0, 0),
        WEST(-1, 0, 0),
        NORTH(0, 0, 1),
        SOUTH(0, 0, -1);

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        Face(int offsetX, int offsetY, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        public int offsetX() {
            return offsetX;
        }

        public int offsetY() {
            return offsetY;
        }

        public int offsetZ() {
            return offsetZ;
        }
    }
}
