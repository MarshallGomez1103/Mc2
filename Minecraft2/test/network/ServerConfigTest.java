package network;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {
    @Test void defaultsMatchEthiansContainerAndEnvironmentOverridesWorldProperty() {
        var defaults = ServerConfig.fromEnvironment(Map.of(), "saved-worlds");
        assertEquals(7777, defaults.gamePort()); assertEquals(8080, defaults.healthPort());
        assertEquals(20, defaults.tickHz()); assertEquals(0, defaults.loadZombies());
        assertEquals(Path.of("saved-worlds").toAbsolutePath(), defaults.worldDirectory());
        var configured = ServerConfig.fromEnvironment(Map.of("MC2_WORLD_DIR", "env-worlds",
                "MC2_TICK_HZ", "40", "MC2_LOAD_ZOMBIES", "80"), "property-worlds");
        assertEquals(Path.of("env-worlds").toAbsolutePath(), configured.worldDirectory());
        assertEquals(40, configured.tickHz()); assertEquals(80, configured.loadZombies());
    }
    @Test void rejectsInvalidValuesRatherThanSilentlyChangingTheBenchmark() {
        for (var env : java.util.List.of(Map.of("MC2_TICK_HZ", "0"), Map.of("MC2_TICK_HZ", "10"),
                Map.of("MC2_SERVER_PORT", "65536"), Map.of("MC2_LOAD_ZOMBIES", "-1"),
                Map.of("MC2_LOAD_ZOMBIES", "321"), Map.of("MC2_HEALTH_PORT", "7777"),
                Map.of("MC2_TICK_HZ", "twenty"))) {
            assertThrows(IllegalArgumentException.class, () -> ServerConfig.fromEnvironment(env, "worlds"));
        }
    }
}
