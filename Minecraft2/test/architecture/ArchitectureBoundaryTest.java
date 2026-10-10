package architecture;

import application.GameSession;
import application.PlayerFrameInput;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas de dependencia del ADR-001, comprobadas sin una biblioteca adicional. */
class ArchitectureBoundaryTest {
    private static final Pattern IMPORT = Pattern.compile("(?m)^import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    @Test void domainDoesNotImportUpperLayersGraphicsOrStorage() throws Exception {
        checkImports("domain", List.of("application.", "presentation.", "persistence.",
                "bootstrap.", "com.badlogic."), false);
    }
    @Test void applicationOnlyUsesTheWorldStorageAbstractionOutsideItsCore() throws Exception {
        checkImports("application", List.of("presentation.", "bootstrap.", "com.badlogic."), true);
    }
    @Test void inputTranslatesDevicesAndSessionAcceptsDataInsteadOfACallback() throws Exception {
        String source = Files.readString(Path.of("src/presentation/game/GameInput.java"));
        for (String forbidden : List.of("PlayerPhysics", "CollisionResolver", "PlayerMovementService",
                "Stamina", "PlayerControlService", "EnemyUpdateService", "PistolService", "ZombieMeleeService")) {
            assertFalse(source.contains(forbidden), "GameInput no debe coordinar " + forbidden);
        }
        assertNotNull(GameSession.class.getMethod("advance", double.class, PlayerFrameInput.class));
        assertThrows(NoSuchMethodException.class,
                () -> GameSession.class.getMethod("advance", double.class, Runnable.class));
    }
    private static void checkImports(String layer, List<String> forbidden, boolean storageOnly) throws Exception {
        try (var paths = Files.walk(Path.of("src", layer))) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                var imports = IMPORT.matcher(Files.readString(file));
                while (imports.find()) {
                    String dependency = imports.group(1);
                    for (String prefix : forbidden) {
                        assertFalse(dependency.startsWith(prefix), file + " depende de " + dependency);
                    }
                    if (storageOnly && dependency.startsWith("persistence.")) {
                        assertEquals("persistence.WorldStorage", dependency, file.toString());
                    }
                }
            }
        }
    }
}
