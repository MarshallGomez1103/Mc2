package architecture;

import application.GameSession;
import application.PlayerFrameInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas de capas de ADR-001 y ADR-004; analizan dependencias de fuentes, no reflexión. */
class ArchitectureBoundaryTest {
    /** Text blocks, strings, chars and comments, which may name a package without depending on it. */
    private static final Pattern CODE_NOISE = Pattern.compile(
            "\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])+'|/\\*[\\s\\S]*?\\*/|//[^\\n]*");
    /** A dotted name that does not continue a longer one, so imports and fully-qualified uses are both seen. */
    private static final Pattern QUALIFIED = Pattern.compile("(?<![\\w.])([A-Za-z_]\\w*(?:\\.(?:\\w+|\\*))+)");

    private static final List<String> DOMAIN_FORBIDDEN = List.of("application.", "presentation.", "persistence.",
            "bootstrap.", "network.", "com.badlogic.", "org.lwjgl.", "java.awt.", "javax.swing.", "javafx.",
            "java.net.", "javax.net.", "java.nio.channels.", "com.sun.net.httpserver.");
    private static final List<String> APPLICATION_FORBIDDEN = List.of("presentation.", "bootstrap.", "network.",
            "com.badlogic.", "org.lwjgl.", "java.awt.", "javax.swing.", "javafx.",
            "java.net.", "javax.net.", "java.nio.channels.", "com.sun.net.httpserver.");
    private static final List<String> NETWORK_FORBIDDEN = List.of("presentation.", "bootstrap.",
            "com.badlogic.", "org.lwjgl.", "java.awt.", "javax.swing.", "javafx.");
    private static final List<String> PERSISTENCE_FORBIDDEN = List.of("application.", "presentation.",
            "bootstrap.", "network.", "com.badlogic.", "org.lwjgl.", "java.awt.", "javax.swing.", "javafx.");

    @TempDir Path fixture;

    @Test void domainDoesNotImportUpperLayersGraphicsOrStorage() throws Exception {
        assertNoViolations(Path.of("src", "domain"), DOMAIN_FORBIDDEN, false);
    }
    @Test void applicationOnlyUsesTheWorldStorageAbstractionOutsideItsCore() throws Exception {
        assertNoViolations(Path.of("src", "application"), APPLICATION_FORBIDDEN, true);
    }
    @Test void networkStaysIndependentOfPresentationCompositionAndGraphics() throws Exception {
        assertNoViolations(Path.of("src", "network"), NETWORK_FORBIDDEN, false);
    }
    @Test void persistenceDoesNotDependOnUpperLayersOrTransport() throws Exception {
        assertNoViolations(Path.of("src", "persistence"), PERSISTENCE_FORBIDDEN, false);
    }
    @Test void coreRejectsTheNetworkAdapterAndDirectSockets() throws Exception {
        write("A.java", "package domain; class A { network.ProtocolCodec codec; java.net.Socket socket; }");
        assertEquals(2, violations(fixture, DOMAIN_FORBIDDEN, false).size());
        assertEquals(2, violations(fixture, APPLICATION_FORBIDDEN, true).size());
    }
    @Test void networkRejectsUiAndCompositionEvenWithoutImports() throws Exception {
        write("A.java", "package network; class A { presentation.game.VoxelGame view; bootstrap.ClientBootstrap root; }");
        assertEquals(2, violations(fixture, NETWORK_FORBIDDEN, false).size());
    }
    @Test void networkRejectsGraphicsBackendsAndStandardDesktopUi() throws Exception {
        write("A.java", "package network; import static com.badlogic.gdx.Gdx.graphics; "
                + "class A { org.lwjgl.glfw.GLFW backend; java.awt.Frame window; javax.swing.JFrame swing; }");
        assertEquals(4, violations(fixture, NETWORK_FORBIDDEN, false).size());
    }
    @Test void storageCannotReachBackIntoTheNetworkAdapter() throws Exception {
        write("A.java", "package persistence; import network.GameServer; class A { }");
        assertEquals(1, violations(fixture, PERSISTENCE_FORBIDDEN, false).size());
    }
    @Test void networkAllowsItsRealApplicationDomainAndJsonDependencies() throws Exception {
        write("A.java", "package network; import application.GameSession; import domain.world.World; "
                + "import persistence.JsonParser; import persistence.JsonWorldStorage; "
                + "class A { java.net.ServerSocket socket; com.sun.net.httpserver.HttpServer health; }");
        assertEquals(List.of(), violations(fixture, NETWORK_FORBIDDEN, false));
    }
    @Test void scannerIgnoresUiNamesInCommentsStringsAndTextBlocks() throws Exception {
        write("A.java", "package network; /* presentation.game.VoxelGame */ class A { "
                + "String x = \"bootstrap.ClientBootstrap\"; String y = \"\"\"\ncom.badlogic.gdx.Gdx\n\"\"\"; }");
        assertEquals(List.of(), violations(fixture, NETWORK_FORBIDDEN, false));
    }
    @Test void scannerDetectsFullyQualifiedUseWithoutImport() throws Exception {
        write("A.java", "package domain;\nclass A { Object v = new com.badlogic.gdx.math.Vector2(); }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsFullyQualifiedUseOfAnUpperLayer() throws Exception {
        write("A.java", "package domain;\nclass A { int v = application.GameSettings.SIZE; }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsStaticImport() throws Exception {
        write("A.java", "package domain;\nimport static com.badlogic.gdx.Gdx.graphics;\nclass A { }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsWildcardImport() throws Exception {
        write("A.java", "package application;\nimport persistence.*;\nclass A { }\n");
        assertEquals(1, violations(fixture, APPLICATION_FORBIDDEN, true).size());
    }
    @Test void scannerDetectsIndentedImport() throws Exception {
        write("A.java", "package domain;\n    import presentation.game.GameWindow;\n\timport bootstrap.WorldDirectory;\nclass A { }\n");
        assertEquals(2, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsFullyQualifiedUseSplitOverLines() throws Exception {
        write("A.java", "package domain;\nclass A { Object v = new com.badlogic\n    .gdx.math.Vector2(); }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsFullyQualifiedUseWithSpacesAroundDots() throws Exception {
        write("A.java", "package domain;\nclass A { int v = application . GameSettings . SIZE; }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void scannerDetectsPersistenceOtherThanWorldStorageFromApplication() throws Exception {
        write("A.java", "package application;\nimport persistence.JsonWorldStorage;\nclass A { persistence.WorldFile f; }\n");
        assertEquals(2, violations(fixture, APPLICATION_FORBIDDEN, true).size());
    }
    @Test void scannerAcceptsCleanSourceCommentsStringsAndTheStorageInterface() throws Exception {
        write("A.java", "package application;\nimport domain.world.World;\nimport persistence.WorldStorage;\n"
                + "/** Mentions presentation.game.GameWindow and com.badlogic.gdx.Gdx. */\n"
                + "class A { // import bootstrap.Foo;\n  String s = \"persistence.Json and com.badlogic.gdx\"; persistence.WorldStorage w; }\n");
        assertEquals(List.of(), violations(fixture, APPLICATION_FORBIDDEN, true));
    }
    @Test void scannerReadsEveryJavaFileBelowTheLayer() throws Exception {
        Files.createDirectories(fixture.resolve("sub"));
        write("sub/B.java", "package domain.sub;\nimport application.X;\nclass B { }\n");
        assertEquals(1, violations(fixture, DOMAIN_FORBIDDEN, false).size());
    }
    @Test void realLayersContainSourcesSoTheRulesAreNotVacuous() throws Exception {
        for (String layer : List.of("domain", "application", "network", "persistence")) {
            try (var paths = Files.walk(Path.of("src", layer))) {
                assertTrue(paths.filter(p -> p.toString().endsWith(".java")).count() > 0, layer);
            }
        }
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
    private void write(String name, String source) throws Exception {
        Files.writeString(fixture.resolve(name), source);
    }
    private static void assertNoViolations(Path root, List<String> forbidden, boolean storageOnly) throws Exception {
        assertEquals(List.of(), violations(root, forbidden, storageOnly));
    }
    private static List<String> violations(Path root, List<String> forbidden, boolean storageOnly) throws Exception {
        List<String> found = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                // ponytail: regex scan, not a parser; it sees imports at any indentation and fully-qualified uses,
                // but not reflection by string. Grow into a real parser or ArchUnit if the rules need more.
                String code = CODE_NOISE.matcher(Files.readString(file)).replaceAll(" ").replaceAll("\\s*\\.\\s*", ".");
                var references = QUALIFIED.matcher(code);
                while (references.find()) {
                    String dependency = references.group(1);
                    boolean banned = forbidden.stream().anyMatch(dependency::startsWith)
                            || (storageOnly && dependency.startsWith("persistence.") && !dependency.equals("persistence.WorldStorage"));
                    if (banned) found.add(file + " depende de " + dependency);
                }
            }
        }
        return found;
    }
}
