package presentation.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DeathMessagesTest {
    @Test void usesOnlyRequestedPhrasesAndChangesOnlyAfterRespawn() {
        var messages = new DeathMessages();
        for (String expected : new String[] {"Pereciste", "Paila", "Te agarraron", "Pereciste"}) {
            messages.observe(false);
            assertEquals(expected, messages.observe(true));
            for (int frame = 0; frame < 100; frame++) assertEquals(expected, messages.observe(true));
        }
    }
    @Test void specialCausesHaveExactPhrasesAndDoNotConsumeGenericCycle() {
        var messages = new DeathMessages();
        assertEquals("Aquí no hay piso", messages.observe(domain.player.PlayerLife.DeathCause.VOID));
        assertEquals("Aquí no hay piso", messages.observe(domain.player.PlayerLife.DeathCause.VOID));
        messages.observe(false);
        assertEquals("Te mató el zombi kamikaze", messages.observe(domain.player.PlayerLife.DeathCause.KAMIKAZE));
        messages.observe(false);
        assertEquals("Pereciste", messages.observe(domain.player.PlayerLife.DeathCause.ENEMY));
        messages.observe(false);
        assertEquals("Paila", messages.observe(domain.player.PlayerLife.DeathCause.ENEMY));
    }
}
