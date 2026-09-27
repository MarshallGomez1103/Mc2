package presentation.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ZombieAtlasTest {
    @Test void eyesOnlyAppearOnHeadFrontAndAllOtherFacesHaveDifferentUvTiles() {
        int[][] pixels=ZombieAtlas.pixels();
        assertEquals(ZombieSkin.EYE,pixels[3][1]);
        assertNotEquals(ZombieSkin.EYE,pixels[3][9]);
        for(int part=0;part<4;part++) for(int face=0;face<6;face++) {
            float[] uv=ZombieAtlas.uv(part,face);
            assertTrue(uv[0]>=0&&uv[1]>=0&&uv[2]<=1&&uv[3]<=1);
            assertTrue(uv[2]>uv[0]&&uv[3]>uv[1]);
        }
        assertThrows(IllegalArgumentException.class,()->ZombieAtlas.uv(4,0));
    }
    @Test void everyPixelIsOpaqueAndAtlasIsFresh() {
        int[][] pixels=ZombieAtlas.pixels();
        for(int[] row:pixels) for(int color:row) assertEquals(255,color&255);
        pixels[0][0]=0;assertNotEquals(0,ZombieAtlas.pixels()[0][0]);
    }
}
