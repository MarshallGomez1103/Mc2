package bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/** Preferencia de arranque: el driver decide y GL_RENDERER confirma el dispositivo efectivo. */
public final class GpuPreference {
    public enum Mode { AUTO, INTEGRATED, DEDICATED }
    private GpuPreference() { }

    public static Path file(Path worlds) {
        return worlds.toAbsolutePath().normalize().resolveSibling("graphics.properties");
    }

    public static Mode read(Path worlds) {
        String override = System.getProperty("mc2.gpu");
        if (override != null) return parse(override);
        return readSaved(worlds);
    }

    public static Mode readSaved(Path worlds) {
        Properties values = new Properties();
        Path path = file(worlds);
        if (!Files.exists(path)) return Mode.AUTO;
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
            return parse(values.getProperty("gpu", "AUTO"));
        } catch (IOException failure) {
            System.err.println("Preferencia gráfica no legible; AUTO: " + failure.getMessage());
            return Mode.AUTO;
        }
    }

    public static Mode parse(String value) {
        return "DEDICATED".equalsIgnoreCase(value) ? Mode.DEDICATED
                : "INTEGRATED".equalsIgnoreCase(value) ? Mode.INTEGRATED : Mode.AUTO;
    }

    public static void save(Path worlds, Mode mode) throws IOException {
        Path path = file(worlds);
        Files.createDirectories(path.getParent());
        Properties values = new Properties();
        values.setProperty("gpu", mode.name());
        try (OutputStream output = Files.newOutputStream(path)) {
            values.store(output, "Minecraft2: aplica al próximo inicio; no cambia GPU en caliente");
        }
    }

    public static boolean dedicatedAvailable() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux")
                && vendors().size() > 1;
    }

    public static String label(Mode mode) {
        return switch (mode) {
            case AUTO -> "Automática";
            case INTEGRATED -> "Integrada";
            case DEDICATED -> "Dedicada";
        };
    }

    public static boolean integratedAvailable() { return integratedDevice() != null; }

    /** The boot display adapter on Linux; select its PCI address, never a card index. */
    private static String integratedDevice() {
        Path drm = Path.of("/sys/class/drm");
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")
                || !Files.isDirectory(drm)) return null;
        try (Stream<Path> paths = Files.list(drm)) {
            for (Path card : paths.filter(p -> p.getFileName().toString().matches("card[0-9]+"))
                    .sorted().toList()) {
                Path device = card.resolve("device");
                if (Files.isRegularFile(device.resolve("boot_vga"))
                        && Files.readString(device.resolve("boot_vga")).trim().equals("1")
                        && !Files.readString(device.resolve("vendor")).trim().equals("0x10de")) {
                    return "pci-" + device.toRealPath().getFileName().toString()
                            .replace(':', '_').replace('.', '_');
                }
            }
        } catch (IOException ignored) { /* Let the system choose if detection fails. */ }
        return null;
    }

    private static List<String> vendors() {
        Path drm = Path.of("/sys/class/drm");
        if (!Files.isDirectory(drm)) return List.of();
        try (Stream<Path> paths = Files.list(drm)) {
            List<String> values = new ArrayList<>();
            for (Path card : paths.filter(p -> p.getFileName().toString().matches("card[0-9]+"))
                    .toList()) {
                Path vendor = card.resolve("device/vendor");
                if (Files.isRegularFile(vendor)) values.add(Files.readString(vendor).trim());
            }
            return values;
        } catch (IOException failure) { return List.of(); }
    }

    /** Sin drivers ni cambio en caliente: relanza una sola vez antes de crear contexto. */
    public static void prepare(Path worlds, String[] arguments) {
        Mode mode = read(worlds);
        if (Boolean.getBoolean("mc2.gpu.prepared") || mode == Mode.AUTO) return;
        String integrated = integratedDevice();
        if (mode == Mode.DEDICATED ? !dedicatedAvailable() : integrated == null) {
            System.err.println(label(mode) + " no disponible en esta plataforma; se usará automática.");
            return;
        }
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-Dmc2.gpu.prepared=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Minecraft2Application.class.getName());
        command.addAll(List.of(arguments));
        ProcessBuilder child = new ProcessBuilder(command).inheritIO();
        if (mode == Mode.INTEGRATED) configureIntegratedEnvironment(child.environment(), integrated);
        else configureEnvironment(child.environment(), vendors().contains("0x10de"));
        try {
            System.out.println("Gráfica elegida: " + label(mode) + ".");
            int result = child.start().waitFor();
            if (result == 0) System.exit(0);
            System.err.println("Inicio con " + label(mode) + " falló (" + result + "); intentando AUTO.");
        } catch (IOException failure) {
            System.err.println("No se pudo solicitar GPU dedicada; AUTO: " + failure.getMessage());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Inicio interrumpido", failure);
        }
    }

    public static void configureIntegratedEnvironment(Map<String, String> environment, String pciDevice) {
        environment.remove("__NV_PRIME_RENDER_OFFLOAD");
        environment.remove("__NV_PRIME_RENDER_OFFLOAD_PROVIDER");
        environment.put("__GLX_VENDOR_LIBRARY_NAME", "mesa");
        environment.put("DRI_PRIME", pciDevice);
    }

    public static void configureEnvironment(Map<String, String> environment, boolean nvidia) {
        environment.remove("DRI_PRIME");
        environment.remove("__NV_PRIME_RENDER_OFFLOAD");
        environment.remove("__NV_PRIME_RENDER_OFFLOAD_PROVIDER");
        environment.remove("__GLX_VENDOR_LIBRARY_NAME");
        if (nvidia) {
            environment.put("__NV_PRIME_RENDER_OFFLOAD", "1");
            environment.put("__GLX_VENDOR_LIBRARY_NAME", "nvidia");
        } else {
            environment.put("DRI_PRIME", "1");
        }
    }
}
