package application;

import domain.enemy.Difficulty;
import domain.enemy.WaveRules;
import domain.enemy.ZombieParameters;

import java.util.Objects;

/**
 * Configuración del Corte 2 durante una sesión del programa: se cambia desde el menú y se lee al
 * abrir cada ventana de juego. No se guarda en disco ni en el JSON del mundo; al reiniciar el
 * programa vuelve a los valores por defecto.
 *
 * <p>La textura de bloques sigue en {@code GameWindow}: la textura de enemigos es una opción
 * independiente.
 */
public final class GameSettings {
    private boolean enemiesEnabled = true;
    private boolean enemyTexturesEnabled = true;
    private Difficulty difficulty = Difficulty.NORMAL;

    public boolean enemiesEnabled() {
        return enemiesEnabled;
    }

    /** Con enemigos OFF no aparecen oleadas ni se actualiza ningún zombi. */
    public boolean toggleEnemies() {
        enemiesEnabled = !enemiesEnabled;
        return enemiesEnabled;
    }

    public boolean enemyTexturesEnabled() {
        return enemyTexturesEnabled;
    }

    /** Con texturas de enemigos OFF el zombi se dibuja como una caja de color plano. */
    public boolean toggleEnemyTextures() {
        enemyTexturesEnabled = !enemyTexturesEnabled;
        return enemyTexturesEnabled;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty no puede ser null");
    }

    /** Pasa a la siguiente dificultad y la devuelve. */
    public Difficulty cycleDifficulty() {
        difficulty = difficulty.next();
        return difficulty;
    }

    public ZombieParameters zombieParameters() {
        return difficulty.zombieParameters();
    }

    public WaveRules waveRules() {
        return difficulty.waveRules();
    }
}
