package domain.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WaveRulesTest {
    private static WaveRules rules(int base, int extra, int max) {
        return new WaveRules(5.0, base, extra, max, 1.0, 10.0, 8.0, 12.0);
    }

    @Test
    void eachWaveAddsExtraZombiesUntilTheCap() {
        WaveRules rules = rules(2, 3, 9);

        assertEquals(2, rules.countFor(1));
        assertEquals(5, rules.countFor(2));
        assertEquals(8, rules.countFor(3));
        assertEquals(9, rules.countFor(4), "la cuarta daría 11, pero el tope es 9");
        assertEquals(9, rules.countFor(1_000_000), "sin desbordar en oleadas muy altas");
    }

    @Test
    void explicitMultiplierDoublesWavesAndSaturatesWithoutOverflow() {
        WaveRules rules = new WaveRules(0, 4, 0, Integer.MAX_VALUE, 1, 1, 5, 8, 2);
        int[] counts = {4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048};
        for (int wave = 1; wave <= counts.length; wave++) assertEquals(counts[wave - 1], rules.countFor(wave));
        assertEquals(1_073_741_824, rules.countFor(29));
        assertEquals(Integer.MAX_VALUE, rules.countFor(30));
        assertEquals(Integer.MAX_VALUE, rules.countFor(Integer.MAX_VALUE));
    }

    @Test
    void explicitMultiplierAlsoHonorsCustomCapAndRejectsInvalidFactor() {
        WaveRules rules = new WaveRules(0, 4, 0, 10, 1, 1, 5, 8, 2);
        assertEquals(4, rules.countFor(1));
        assertEquals(8, rules.countFor(2));
        assertEquals(10, rules.countFor(3));
        assertThrows(IllegalArgumentException.class, () -> new WaveRules(0, 4, 0, 10, 1, 1, 5, 8, 0));
        WaveRules huge = new WaveRules(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 1, 1, 5, 8, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, huge.countFor(Integer.MAX_VALUE));
    }

    @Test
    void zeroExtraKeepsWavesConstant() {
        WaveRules rules = rules(3, 0, 3);
        assertEquals(3, rules.countFor(1));
        assertEquals(3, rules.countFor(50));
    }

    @Test
    void waveNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> rules(2, 1, 5).countFor(0));
    }

    @Test
    void rejectsInconsistentRules() {
        assertThrows(IllegalArgumentException.class, () -> rules(0, 1, 5), "oleada vacía");
        assertThrows(IllegalArgumentException.class, () -> rules(4, 1, 3), "tope menor que la base");
        assertThrows(IllegalArgumentException.class, () -> rules(2, -1, 5), "oleadas que decrecen");
        assertThrows(IllegalArgumentException.class,
                () -> new WaveRules(-1.0, 2, 1, 5, 1.0, 10.0, 8.0, 12.0), "espera negativa");
        assertThrows(IllegalArgumentException.class,
                () -> new WaveRules(5.0, 2, 1, 5, -1.0, 10.0, 8.0, 12.0), "intervalo negativo");
        assertThrows(IllegalArgumentException.class,
                () -> new WaveRules(5.0, 2, 1, 5, 1.0, -1.0, 8.0, 12.0), "pausa negativa");
        assertThrows(IllegalArgumentException.class,
                () -> new WaveRules(5.0, 2, 1, 5, 1.0, 10.0, 0.0, 12.0), "distancia mínima nula");
        assertThrows(IllegalArgumentException.class,
                () -> new WaveRules(5.0, 2, 1, 5, 1.0, 10.0, 12.0, 8.0), "anillo invertido");
    }

    @Test
    void acceptsBoundaryValues() {
        WaveRules rules = new WaveRules(0.0, 1, 0, 1, 0.0, 0.0, 5.0, 5.0);
        assertEquals(1, rules.countFor(1));
    }
}
