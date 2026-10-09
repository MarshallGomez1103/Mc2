package presentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Caja negra de consola: proceso JAR, stdin/stdout y archivos temporales reales. */
class GameFlowSmokeIT {
    @Test
    void createsListsSavesThenReloadsAndDeletesInANewProcess(@TempDir Path dir) throws Exception {
        // Arrange / Act: crear y cerrar usando solo la interfaz pública.
        String first = console(dir, "1", "smokeWorld", "1", "2", "4", "0");
        // Assert: el listado y el archivo confirman el guardado.
        assertTrue(first.contains("Mundo creado y cargado: smokeWorld"), first);
        assertTrue(first.contains("  - smokeWorld"), first);
        assertTrue(first.contains("Mundo actual guardado: smokeWorld"), first);
        assertTrue(Files.isRegularFile(dir.resolve("smokeWorld.json")));
        // Act: otro proceso debe leer el archivo, sin compartir el Singleton.
        String second = console(dir, "3", "smokeWorld", "2", "5", "smokeWorld", "s", "2", "0");
        // Assert
        assertTrue(second.contains("Mundo cargado: smokeWorld"), second);
        assertTrue(second.contains("  - smokeWorld"), second);
        assertTrue(second.contains("Mundo eliminado: smokeWorld"), second);
        assertTrue(second.contains("No hay mundos guardados."), second);
        assertFalse(Files.exists(dir.resolve("smokeWorld.json")));
    }

    @Test
    void cancellingDeletionKeepsTheWorldAvailableAfterRestart(@TempDir Path dir) throws Exception {
        // Arrange
        console(dir, "1", "conservado", "1", "0");
        byte[] saved = Files.readAllBytes(dir.resolve("conservado.json"));
        // Act
        String output = console(dir, "5", "conservado", "n", "3", "conservado", "2", "0");
        // Assert
        assertTrue(output.contains("Operación cancelada. No se eliminó ningún mundo."), output);
        assertTrue(output.contains("Mundo cargado: conservado"), output);
        assertTrue(output.contains("  - conservado"), output);
        assertArrayEquals(saved, Files.readAllBytes(dir.resolve("conservado.json")));
    }

    @Test
    void invalidJsonIsReportedWithoutReplacingTheCurrentWorld(@TempDir Path dir) throws Exception {
        // Arrange: el archivo inválido es una entrada externa, no un servicio simulado.
        console(dir, "1", "valido", "1", "0");
        Path invalid = dir.resolve("roto.json");
        Files.writeString(invalid, "{\"name\":\"incompleto\"}", StandardCharsets.UTF_8);
        // Act
        String output = console(dir, "3", "valido", "2", "3", "roto", "4", "0");
        // Assert: el mundo válido sigue activo y el archivo inválido no se destruye.
        assertTrue(output.contains("  - roto"), output);
        assertFalse(output.contains("Mundo cargado: roto"), output);
        assertTrue(output.contains("Problema con el archivo del mundo:"), output);
        assertTrue(output.contains("Mundo actual guardado: valido"), output);
        assertEquals("{\"name\":\"incompleto\"}", Files.readString(invalid));
    }

    private static String console(Path dir, String... options) throws IOException, InterruptedException {
        Path jar = Path.of("target", "minecraft2-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "mvn verify debe empaquetar el JAR antes de Failsafe");
        Path outputFile = Files.createTempFile(dir, "console-", ".txt");
        String executable = java.io.File.separatorChar == '\\' ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Process process = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8",
                "-Dmc2.console=true", "-Dmc2.gpu=AUTO", "-Dmc2.worlds.dir=" + dir.toAbsolutePath(),
                "-jar", jar.toString()).directory(dir.toFile()).redirectErrorStream(true)
                .redirectOutput(outputFile.toFile()).start();
        try {
            try (var stdin = process.getOutputStream()) {
                stdin.write((String.join("\n", options) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            // La salida va a archivo: leer stdout no puede bloquear el timeout.
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                fail("El proceso de consola excedió 30 s:\n" + Files.readString(outputFile));
            }
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("Hasta luego."), output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
