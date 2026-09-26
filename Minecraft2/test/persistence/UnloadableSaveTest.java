package persistence;

import domain.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El códec nunca escribe ni acepta un mundo que después no se podría cargar o que se cargaría mal. */
class UnloadableSaveTest {

    @Test
    void refusesToWriteAPlayerBelowTheWorld() {
        World world = WorldSamples.twoChunkWorld("cayendo");
        world.getPlayer().setY(-3.0);

        assertThrows(IllegalArgumentException.class, () -> WorldJsonCodec.write(world));
    }

    @Test
    void refusesToWriteAPlayerAboveTheWorld() {
        World world = WorldSamples.twoChunkWorld("saltando");
        world.getPlayer().setY(64.5);

        assertThrows(IllegalArgumentException.class, () -> WorldJsonCodec.write(world));
    }

    @Test
    void chunkCoordinatesThatWouldOverflowAreAnInvalidFile() {
        String json = WorldJsonCodec.write(WorldSamples.twoChunkWorld("lejano"));
        String tampered = json.replace("      \"x\": -1,", "      \"x\": 200000000,");
        assertTrue(!tampered.equals(json), "la muestra debe contener el chunk -1");

        InvalidWorldFileException failure = assertThrows(InvalidWorldFileException.class,
                () -> WorldJsonCodec.read(tampered, WorldSamples.FACTORY));
        assertTrue(failure.getMessage().contains("fuera del mundo"), failure.getMessage());
    }
}
