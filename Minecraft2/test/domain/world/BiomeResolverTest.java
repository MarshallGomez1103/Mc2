package domain.world;

import domain.world.biome.BiomeResolver;
import domain.world.biome.BiomeType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeResolverTest {
    @Test
    void sameSeedAndCoordinatesAlwaysResolveTheSameBiomeIncludingNegatives() {
        BiomeResolver first = new BiomeResolver(17);
        BiomeResolver second = new BiomeResolver(17);
        for (int x = -40; x <= 40; x += 3) {
            for (int z = -40; z <= 40; z += 3) {
                assertEquals(first.biomeAt(x, z), second.biomeAt(x, z));
            }
        }
    }

    @Test
    void generatesAllMandatoryBiomesAsRegionsRatherThanIndependentBlocks() {
        BiomeResolver resolver = new BiomeResolver(17);
        EnumSet<BiomeType> seen = EnumSet.noneOf(BiomeType.class);
        int boundaries = 0;
        int comparisons = 0;
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                BiomeType current = resolver.biomeAt(x, z);
                seen.add(current);
                if (current != resolver.biomeAt(x + 1, z)) {
                    boundaries++;
                }
                comparisons++;
            }
        }
        assertEquals(EnumSet.allOf(BiomeType.class), seen);
        assertTrue(boundaries < comparisons / 10, "los límites deben ser poco frecuentes frente al interior");
    }

    @Test
    void changingSeedChangesSomeBiomeAssignments() {
        BiomeResolver first = new BiomeResolver(17);
        BiomeResolver second = new BiomeResolver(18);
        int changed = 0;
        for (int x = -32; x <= 32; x += 4) {
            for (int z = -32; z <= 32; z += 4) {
                if (first.biomeAt(x, z) != second.biomeAt(x, z)) {
                    changed++;
                }
            }
        }
        assertTrue(changed > 0);
    }
}
