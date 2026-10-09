package presentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Prueba de caja negra sobre el programa empaquetado (target/*.jar), ejecutado como un
 * proceso real del sistema operativo y dirigido unicamente por su consola publica
 * (stdin/stdout), sin invocar ninguna clase Java del proyecto directamente.
 *
 * Alcance declarado: solo el flujo de menu de consola (crear, listar, guardar,
 * cargar, eliminar, salir). No ejercita la opcion "6. Jugar": esa abre la ventana
 * grafica de LibGDX y requiere display, fuera del alcance de este smoke. La capa
 * grafica ya cuenta con su propio smoke de 224 frames documentado aparte.
 *
 * Requiere que exista un jar empaquetado en target/ (`mvn package`). Guarda los
 * mundos de prueba en un directorio temporal real, nunca en la carpeta de mundos
 * del usuario.
 */
class GameFlowSmokeIT {

    @Test
    void consoleFlowCreatesListsSavesAndDeletesWorldThroughPackagedJar(@TempDir Path worldsDir)
            throws IOException, InterruptedException {
        Path jar = locatePackagedJar();

        String script = String.join("\n",
                "1", "smokeWorld", "1",   // crear mundo pequeno "smokeWorld"
                "2",                      // listar mundos
                "4",                      // guardar mundo actual
                "5", "smokeWorld", "s",   // eliminar mundo (confirmar)
                "0", "");                 // salir

        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-Dmc2.console=true",
                "-Dmc2.worlds.dir=" + worldsDir.toAbsolutePath(),
                "-jar", jar.toAbsolutePath().toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        process.getOutputStream().write(script.getBytes());
        process.getOutputStream().close();

        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            fail("El proceso del JAR empaquetado no termino a tiempo:\n" + output);
        }

        assertEquals(0, process.exitValue(), "Salida de consola:\n" + output);
        assertTrue(output.contains("Mundo creado y cargado: smokeWorld"), output);
        assertTrue(output.contains("smokeWorld"), "El listado debe mostrar el mundo creado:\n" + output);
        assertTrue(output.contains("Mundo actual guardado: smokeWorld"), output);
        assertTrue(output.contains("Mundo eliminado: smokeWorld"), output);
        assertTrue(output.contains("Hasta luego."), output);
    }

    private static Path locatePackagedJar() throws IOException {
        Path target = Path.of("target");
        try (var files = Files.list(target)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("original"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontro el JAR empaquetado en target/. Ejecute 'mvn package' antes de esta prueba."));
        }
    }
}