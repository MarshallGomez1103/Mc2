package presentation.game;

import domain.player.PlayerLife.DeathCause;

/** One stable phrase per death; special causes do not consume the ordinary phrase cycle. */
public final class DeathMessages {
    private static final String[] PHRASES = {"Pereciste", "Paila", "Te agarraron"};
    private int next;
    private boolean wasDead;
    private String current = PHRASES[0];

    public String observe(DeathCause cause) {
        boolean dead = cause != null;
        if (dead && !wasDead) {
            current = switch (cause) {
                case VOID -> "Aquí no hay piso";
                case KAMIKAZE -> "Te mató el zombi kamikaze";
                case ENEMY -> {
                    String phrase = PHRASES[next];
                    next = (next + 1) % PHRASES.length;
                    yield phrase;
                }
            };
        }
        wasDead = dead;
        return current;
    }

    public String observe(boolean dead) {
        return observe(dead ? DeathCause.ENEMY : null);
    }
}
