package presentation.game;

/**
 * Textura pixel-art del zombi generada por código, sin archivos de imagen. Es Java puro (colores
 * RGBA8888 en una matriz) para poder probarla sin OpenGL; {@code ZombieRenderer} la vuelca en un
 * {@code Pixmap}.
 *
 * <p>Proporción 1:3 como el cuerpo del zombi (0.6 × 1.8): cabeza verde con ojos y boca, camisa
 * turquesa y pantalón azul. La misma imagen se aplica a las seis caras de la caja.
 */
public final class ZombieSkin {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 24;
    /** Filas [0, HEAD_ROWS) son cabeza; después camisa hasta PANTS_START y pantalón hasta el final. */
    public static final int HEAD_ROWS = 8;
    public static final int PANTS_START = 17;

    static final int SKIN = rgba(0x4F, 0x8F, 0x3A);
    static final int SKIN_DARK = rgba(0x3B, 0x6E, 0x2B);
    static final int EYE = rgba(0x1A, 0x1A, 0x1A);
    static final int MOUTH = rgba(0x2E, 0x4A, 0x22);
    static final int SHIRT = rgba(0x2F, 0x9C, 0xA0);
    static final int SHIRT_DARK = rgba(0x24, 0x7A, 0x7D);
    static final int PANTS = rgba(0x3A, 0x3F, 0x9E);
    static final int SHOES = rgba(0x4A, 0x4A, 0x4A);

    private ZombieSkin() {
    }

    /** Píxeles por fila y columna, empezando por arriba. Cada llamada devuelve una copia nueva. */
    public static int[][] pixels() {
        int[][] pixels = new int[HEIGHT][WIDTH];
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                pixels[row][column] = baseColor(row, column);
            }
        }
        // Ojos y boca.
        pixels[3][1] = EYE;
        pixels[3][2] = EYE;
        pixels[3][5] = EYE;
        pixels[3][6] = EYE;
        for (int column = 2; column <= 5; column++) {
            pixels[6][column] = MOUTH;
        }
        return pixels;
    }

    private static int baseColor(int row, int column) {
        if (row < HEAD_ROWS) {
            return row == 0 || column == 0 || column == WIDTH - 1 ? SKIN_DARK : SKIN;
        }
        if (row < PANTS_START) {
            return column == 0 || column == WIDTH - 1 ? SHIRT_DARK : SHIRT;
        }
        return row == HEIGHT - 1 ? SHOES : PANTS;
    }

    private static int rgba(int red, int green, int blue) {
        return red << 24 | green << 16 | blue << 8 | 0xFF;
    }
}
