package application;

import domain.enemy.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsTest {
    private final GameSettings settings = new GameSettings();

    @Test
    void newSessionStartsWithEnemiesOnTexturedAndNormal() {
        assertTrue(settings.enemiesEnabled());
        assertTrue(settings.enemyTexturesEnabled());
        assertEquals(Difficulty.NORMAL, settings.difficulty());
    }

    @Test
    void enemyTogglesAreIndependent() {
        assertFalse(settings.toggleEnemies());
        assertTrue(settings.enemyTexturesEnabled(), "apagar enemigos no toca sus texturas");

        assertFalse(settings.toggleEnemyTextures());
        assertFalse(settings.enemyTexturesEnabled());
        assertFalse(settings.enemiesEnabled(), "cambiar texturas no reactiva enemigos");

        assertTrue(settings.toggleEnemies());
        assertTrue(settings.toggleEnemyTextures());
    }

    @Test
    void parametersFollowTheSelectedDifficulty() {
        assertEquals(Difficulty.NORMAL.zombieParameters(), settings.zombieParameters());
        assertEquals(Difficulty.NORMAL.waveRules(), settings.waveRules());

        settings.setDifficulty(Difficulty.VERY_HARD);

        assertEquals(Difficulty.VERY_HARD.zombieParameters(), settings.zombieParameters());
        assertEquals(Difficulty.VERY_HARD.waveRules(), settings.waveRules());
    }

    @Test
    void cyclingDifficultyAlternatesLevels() {
        assertEquals(Difficulty.VERY_HARD, settings.cycleDifficulty());
        assertEquals(Difficulty.VERY_HARD, settings.difficulty());
        assertEquals(Difficulty.NORMAL, settings.cycleDifficulty());
    }

    @Test
    void difficultyCannotBeCleared() {
        assertThrows(NullPointerException.class, () -> settings.setDifficulty(null));
        assertEquals(Difficulty.NORMAL, settings.difficulty());
    }
}
