package domain.enemy;

/**
 * Reglas de oleadas de una dificultad. HordeManager solo las consume; no sabe qué dificultad
 * las produjo.
 *
 * @param firstWaveDelaySeconds espera desde el inicio de la sesión hasta la primera oleada.
 * @param baseCount             zombis de la primera oleada.
 * @param extraPerWave          zombis añadidos en cada oleada siguiente.
 * @param maxCount              tope de zombis de una oleada, por grande que sea su número.
 * @param spawnIntervalSeconds  separación entre dos apariciones dentro de una misma oleada.
 * @param pauseSeconds          descanso entre el final de una oleada y el comienzo de la siguiente.
 * @param minSpawnDistance      distancia horizontal mínima al jugador de cada aparición.
 * @param maxSpawnDistance      distancia horizontal máxima al jugador de cada aparición.
 */
public record WaveRules(
        double firstWaveDelaySeconds,
        int baseCount,
        int extraPerWave,
        int maxCount,
        double spawnIntervalSeconds,
        double pauseSeconds,
        double minSpawnDistance,
        double maxSpawnDistance) {

    public WaveRules {
        if (firstWaveDelaySeconds < 0 || pauseSeconds < 0 || spawnIntervalSeconds < 0) {
            throw new IllegalArgumentException("Los tiempos de oleada no pueden ser negativos");
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
        long count = baseCount + (long) extraPerWave * (wave - 1);
        return (int) Math.min(count, maxCount);
    }
}
