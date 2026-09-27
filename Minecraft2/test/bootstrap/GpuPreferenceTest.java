package bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GpuPreferenceTest {
    @TempDir Path directory;
    @Test void storesPreferenceOutsideWorldsAndReadsNextLaunch() throws Exception {
        Path worlds = directory.resolve("worlds");
        assertEquals(GpuPreference.Mode.AUTO, GpuPreference.read(worlds));
        GpuPreference.save(worlds, GpuPreference.Mode.DEDICATED);
        assertEquals(GpuPreference.Mode.DEDICATED, GpuPreference.read(worlds));
        assertTrue(Files.exists(directory.resolve("graphics.properties")));
        assertFalse(Files.exists(worlds));
        GpuPreference.save(worlds, GpuPreference.Mode.INTEGRATED);
        assertEquals(GpuPreference.Mode.INTEGRATED, GpuPreference.read(worlds));
        GpuPreference.save(worlds, GpuPreference.Mode.AUTO);
        assertEquals(GpuPreference.Mode.AUTO, GpuPreference.read(worlds));
    }
    @Test void unknownPreferenceFallsBackToAuto() {
        assertEquals(GpuPreference.Mode.AUTO, GpuPreference.parse("GPU-OFF"));
        assertEquals(GpuPreference.Mode.DEDICATED, GpuPreference.parse("dedicated"));
    }
    @Test void nvidiaPrimeEnvironmentDoesNotRequestMesaDevice() {
        Map<String,String> environment = new HashMap<>();
        environment.put("USER_OPTION", "kept");
        GpuPreference.configureEnvironment(environment, true);
        assertEquals("1", environment.get("__NV_PRIME_RENDER_OFFLOAD"));
        assertEquals("nvidia", environment.get("__GLX_VENDOR_LIBRARY_NAME"));
        assertFalse(environment.containsKey("DRI_PRIME"));
        assertEquals("kept", environment.get("USER_OPTION"));
    }
    @Test void mesaPreferenceUsesDocumentedDriPrimeInsteadOfNvidiaVariables() {
        Map<String,String> environment = new HashMap<>();
        GpuPreference.configureEnvironment(environment, false);
        assertEquals("1", environment.get("DRI_PRIME"));
        assertFalse(environment.containsKey("__GLX_VENDOR_LIBRARY_NAME"));
    }
    @Test void integratedSelectionClearsInheritedNvidiaOffload() {
        Map<String,String> environment = new HashMap<>();
        environment.put("__NV_PRIME_RENDER_OFFLOAD", "1");
        environment.put("__NV_PRIME_RENDER_OFFLOAD_PROVIDER", "NVIDIA-G0");
        environment.put("__GLX_VENDOR_LIBRARY_NAME", "nvidia");
        GpuPreference.configureIntegratedEnvironment(environment, "pci-0000_00_02_0");
        assertEquals("pci-0000_00_02_0", environment.get("DRI_PRIME"));
        assertEquals("mesa", environment.get("__GLX_VENDOR_LIBRARY_NAME"));
        assertFalse(environment.containsKey("__NV_PRIME_RENDER_OFFLOAD"));
        assertFalse(environment.containsKey("__NV_PRIME_RENDER_OFFLOAD_PROVIDER"));
    }
}
