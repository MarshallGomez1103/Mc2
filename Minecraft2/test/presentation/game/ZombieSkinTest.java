package presentation.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ZombieSkinTest {
    private final int[][] pixels = ZombieSkin.pixels();

    @Test
    void keepsTheBodyProportionOfOneToThree() {
        assertEquals(ZombieSkin.HEIGHT, pixels.length);
        for (int[] row : pixels) {
            assertEquals(ZombieSkin.WIDTH, row.length);
        }
        assertEquals(3 * ZombieSkin.WIDTH, ZombieSkin.HEIGHT);
    }

    @Test
    void everyPixelIsOpaque() {
        for (int[] row : pixels) {
            for (int color : row) {
                assertEquals(0xFF, color & 0xFF, "alfa en RGBA8888");
            }
        }
    }

    @Test
    void headShirtAndPantsAreDistinguishable() {
        int head = pixels[1][3];
        int shirt = pixels[12][3];
        int pants = pixels[20][3];

        assertNotEquals(head, shirt);
        assertNotEquals(shirt, pants);
        assertNotEquals(head, pants);
    }

    @Test
    void faceHasEyesAndMouthOnTheHead() {
        assertEquals(ZombieSkin.EYE, pixels[3][1]);
        assertEquals(ZombieSkin.EYE, pixels[3][6]);
        assertEquals(ZombieSkin.MOUTH, pixels[6][3]);
        assertNotEquals(pixels[3][1], pixels[3][3], "entre los ojos hay piel");
    }

    @Test
    void callersCannotCorruptTheSharedSkin() {
        int[][] copy = ZombieSkin.pixels();
        copy[0][0] = 0;
        assertNotSame(copy, pixels);
        assertNotEquals(0, ZombieSkin.pixels()[0][0]);
    }
}
