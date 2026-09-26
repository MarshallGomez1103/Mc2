package domain.enemy;

/**
 * Niveles de dificultad. Cada nivel es solo un paquete de parámetros: la IA y las oleadas usan
 * los mismos algoritmos y nunca preguntan por el nivel, así no hay {@code if (VERY_HARD)} dispersos.
 */
public enum Difficulty {
    /** Los mismos valores con los que se probó la IA: {@link ZombieParameters#defaults()}. */
    NORMAL(ZombieParameters.defaults(),
            new WaveRules(8.0, 2, 1, 6, 1.0, 20.0, 14.0, 20.0)),
    /** Zombis que detectan antes, corren más, aguantan más golpes y atacan más rápido. */
    VERY_HARD(new ZombieParameters(18.0, 1.8, 26.0, 3.6, 5, 0.6, 1.0, 1.0, 0.6),
            new WaveRules(4.0, 4, 2, 12, 0.5, 10.0, 10.0, 16.0));

    private final ZombieParameters zombieParameters;
    private final WaveRules waveRules;

    Difficulty(ZombieParameters zombieParameters, WaveRules waveRules) {
        this.zombieParameters = zombieParameters;
        this.waveRules = waveRules;
    }

    public ZombieParameters zombieParameters() {
        return zombieParameters;
    }

    public WaveRules waveRules() {
        return waveRules;
    }

    /** La siguiente dificultad en orden de declaración; la última vuelve a la primera. */
    public Difficulty next() {
        Difficulty[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
