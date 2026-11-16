package network;

import java.nio.file.Path;
import java.util.Map;

/** Server environment only; MC2_SERVER_HOST is a client destination, never a bind address. */
public record ServerConfig(int gamePort, int healthPort, int tickHz, int loadZombies, Path worldDirectory) {
    public ServerConfig {
        if (gamePort < 0 || gamePort > 65535 || healthPort < 0 || healthPort > 65535
                || (gamePort != 0 && gamePort == healthPort) || tickHz < 20 || tickHz > 120
                || loadZombies < 0 || loadZombies > 320 || worldDirectory == null) {
            throw new IllegalArgumentException("Invalid server configuration");
        }
    }
    public static ServerConfig fromEnvironment(Map<String, String> env, String configuredWorldDirectory) {
        String fallback = configuredWorldDirectory == null || configuredWorldDirectory.isBlank()
                ? "worlds" : configuredWorldDirectory;
        return new ServerConfig(number(env, "MC2_SERVER_PORT", 7777), number(env, "MC2_HEALTH_PORT", 8080),
                number(env, "MC2_TICK_HZ", 20), number(env, "MC2_LOAD_ZOMBIES", 0),
                Path.of(env.getOrDefault("MC2_WORLD_DIR", fallback)).toAbsolutePath().normalize());
    }
    private static int number(Map<String, String> env, String key, int fallback) {
        try { return env.containsKey(key) ? Integer.parseInt(env.get(key).strip()) : fallback; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + key, e); }
    }
}
