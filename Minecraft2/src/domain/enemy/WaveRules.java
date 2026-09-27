package domain.enemy;

/**
 * Reglas de oleadas de una dificultad. HordeManager solo las consume; no sabe qué dificultad
 * las produjo.
 *
 * @param firstWaveDelaySeconds espera desde el inicio de la sesión hasta la primera oleada.
 * @param baseCount             zombis de la primera oleada.
 * @param extraPerWave          zombis añadidos después de aplicar el multiplicador.
 * @param maxCount              tope de zombis de una oleada, por grande que sea su número.
 * @param spawnIntervalSeconds  separación entre dos apariciones dentro de una misma oleada.
 * @param pauseSeconds          descanso entre el final de una oleada y el comienzo de la siguiente.
 * @param minSpawnDistance      distancia horizontal mínima al jugador de cada aparición.
 * @param maxSpawnDistance      distancia horizontal máxima al jugador de cada aparición.
 * @param waveMultiplier        factor de crecimiento (1 conserva la progresión lineal).
 */
public record WaveRules(
        double firstWaveDelaySeconds,
        int baseCount,
        int extraPerWave,
        int maxCount,
        double spawnIntervalSeconds,
        double pauseSeconds,
        double minSpawnDistance,
        double maxSpawnDistance,
        int waveMultiplier) {

    /** Compatibilidad con reglas lineales: los ocho argumentos anteriores conservan su significado. */
    public WaveRules(double firstWaveDelaySeconds, int baseCount, int extraPerWave, int maxCount,
                     double spawnIntervalSeconds, double pauseSeconds,
                     double minSpawnDistance, double maxSpawnDistance) {
        this(firstWaveDelaySeconds, baseCount, extraPerWave, maxCount, spawnIntervalSeconds,
                pauseSeconds, minSpawnDistance, maxSpawnDistance, 1);
    }

    public WaveRules {
        if (firstWaveDelaySeconds < 0 || pauseSeconds < 0 || spawnIntervalSeconds < 0) {
            throw new IllegalArgumentException("Los tiempos de oleada no pueden ser negativos");
        }
        if (waveMultiplier < 1) {
            throw new IllegalArgumentException("El multiplicador de oleadas debe ser al menos 1");
        }
        if (baseCount <= 0 || extraPerWave < 0 || maxCount < baseCount) {
            throw new IllegalArgumentException("Se requiere 0 < baseCount <= maxCount y extraPerWave >= 0");
        }
        if (minSpawnDistance <= 0 || maxSpawnDistance < minSpawnDistance) {
            throw new IllegalArgumentException("Se requiere 0 < minSpawnDistance <= maxSpawnDistance");
        }
    }

    /** Zombis de la oleada {@code wave} (la primera es 1), limitados por {@link #maxCount()}. */
    public int countFor(int wave) {
        if (wave < 1) {
            throw new IllegalArgumentException("La primera oleada es la 1: " + wave);
        }
        if (waveMultiplier == 1) {
            long count = baseCount + (long) extraPerWave * (wave - 1);
            return (int) Math.min(count, maxCount);
        }
        long count = baseCount;
        // Con multiplicador >=2 se alcanza el límite int en <=31 pasos: incluso wave MAX es acotado.
        for (int number = 1; number < wave && count < maxCount; number++) {
            count = Math.min((long) maxCount, count * waveMultiplier + extraPerWave);
        }
        return (int) count;
    }
}
