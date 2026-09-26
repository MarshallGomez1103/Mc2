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
