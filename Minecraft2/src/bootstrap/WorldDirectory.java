package bootstrap;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ubicación estable de los mundos: no cambia al ejecutar el JAR desde otra carpeta. */
public final class WorldDirectory {
    private WorldDirectory() {
    }

    public static Path resolve() {
        try {
            Path codeLocation = Path.of(Minecraft2Application.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return resolve(codeLocation, System.getProperty("mc2.worlds.dir"));
        } catch (URISyntaxException failure) {
            throw new IllegalStateException("No se pudo localizar Minecraft 2", failure);
        }
    }

    /** Se expone para probar IDE/JAR y para permitir un directorio explícito opcional. */
    public static Path resolve(Path codeLocation, String configuredDirectory) {
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory).toAbsolutePath().normalize();
        }
        Path location = codeLocation.toAbsolutePath().normalize();
        Path start = Files.isDirectory(location) ? location : location.getParent();
        for (Path current = start; current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current.resolve("worlds");
            }
        }
        return start.resolve("worlds");
    }
}
